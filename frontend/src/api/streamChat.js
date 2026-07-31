import axios from 'axios'
import { useAuthStore } from '../store/authStore'

/**
 * Streams an AI reply via SSE using fetch (not EventSource, since EventSource
 * cannot send Authorization headers). Parses raw "event: X\ndata: Y\n\n" frames
 * manually from the response body's ReadableStream.
 *
 * Handles 401 responses by attempting a token refresh and retrying once.
 *
 * @param conversationId - target conversation
 * @param content - the user's message text
 * @param onToken - called with each incremental text chunk as it arrives
 * @param onDone - called once the stream completes successfully
 * @param onError - called if the stream fails or the connection drops
 */
export async function streamChatMessage(conversationId, content, { onToken, onDone, onError }) {
  const doStream = async (token) => {
    const response = await fetch(`/api/v1/conversations/${conversationId}/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ content }),
    })

    if (response.status === 401) {
      return { unauthorized: true }
    }

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}))
      throw new Error(errorBody.message || `Stream failed with status ${response.status}`)
    }

    return { unauthorized: false, response }
  }

  try {
    let accessToken = useAuthStore.getState().accessToken
    let result = await doStream(accessToken)

    // If 401, attempt token refresh and retry once
    if (result.unauthorized) {
      const { refreshToken, setTokens, logout } = useAuthStore.getState()

      if (!refreshToken) {
        logout()
        window.location.href = '/login'
        return
      }

      try {
        const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken })
        setTokens(data.accessToken, data.refreshToken)
        accessToken = data.accessToken

        // Retry with the new token
        result = await doStream(accessToken)

        if (result.unauthorized) {
          // Still 401 after refresh — force logout
          logout()
          window.location.href = '/login'
          return
        }
      } catch {
        // Refresh failed — force logout
        logout()
        window.location.href = '/login'
        return
      }
    }

    const { response } = result
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let doneEventReceived = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // SSE frames are separated by a blank line ("\n\n")
      const frames = buffer.split('\n\n')
      buffer = frames.pop() // keep the last (possibly incomplete) frame in the buffer

      for (const frame of frames) {
        if (!frame.trim()) continue

        const lines = frame.split('\n')
        let eventType = 'message'
        const dataLines = []

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5))
          }
        }

        if (eventType === 'token') {
          onToken(dataLines.join('\n'))
        } else if (eventType === 'done') {
          doneEventReceived = true
          onDone()
        }
      }
    }

    // Fallback: if the stream ended without an explicit "done" event,
    // call onDone so the UI doesn't freeze in "streaming" state
    if (!doneEventReceived) {
      onDone()
    }
  } catch (err) {
    onError(err.message || 'Streaming connection failed')
  }
}