import { useAuthStore } from '../store/authStore'

/**
 * Streams an AI reply via SSE using fetch (not EventSource, since EventSource
 * cannot send Authorization headers). Parses raw "event: X\ndata: Y\n\n" frames
 * manually from the response body's ReadableStream.
 *
 * @param conversationId - target conversation
 * @param content - the user's message text
 * @param onToken - called with each incremental text chunk as it arrives
 * @param onDone - called once the stream completes successfully
 * @param onError - called if the stream fails or the connection drops
 */
export async function streamChatMessage(conversationId, content, { onToken, onDone, onError }) {
  const accessToken = useAuthStore.getState().accessToken

  try {
    const response = await fetch(`/api/v1/conversations/${conversationId}/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ content }),
    })

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}))
      throw new Error(errorBody.message || `Stream failed with status ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

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
          onDone()
        }
      }
    }
  } catch (err) {
    onError(err.message || 'Streaming connection failed')
  }
}