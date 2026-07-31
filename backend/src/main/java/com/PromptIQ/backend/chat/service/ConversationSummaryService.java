package com.PromptIQ.backend.chat.service;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.repository.ConversationRepository;
import com.PromptIQ.backend.chat.repository.MessageRepository;
import com.PromptIQ.backend.llm.client.LlmClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationSummaryService {

    private static final int RAW_MESSAGE_WINDOW = 10;   // always send these unsummarized
    private static final int SUMMARIZE_TRIGGER_THRESHOLD = 20; // summarize once history exceeds this

    private static final String SUMMARY_SYSTEM_PROMPT = """
            Summarize the following conversation history concisely, preserving key facts,
            decisions, and context needed to continue the conversation naturally. Write it
            as a compact paragraph, not a transcript.
            """;

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final LlmClient llmClient;

    public ConversationSummaryService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            LlmClient llmClient
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.llmClient = llmClient;
    }

    /**
     * Returns the effective context to send to the LLM: an optional rolling summary
     * plus the most recent raw messages. Triggers a re-summarization if the unsummarized
     * tail has grown past the threshold.
     */
    @Transactional
    public ConversationContext buildContext(Conversation conversation) {
        List<Message> allMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(
                        conversation.getId(),
                        PageRequest.of(0, 500, Sort.by("createdAt").ascending())
                ).getContent();

        int unsummarizedCount = countUnsummarized(allMessages, conversation.getSummarizedUpToMessageId());

        if (unsummarizedCount > SUMMARIZE_TRIGGER_THRESHOLD) {
            summarizeOlderMessages(conversation, allMessages);
        }

        List<Message> recentWindow = allMessages.size() > RAW_MESSAGE_WINDOW
                ? allMessages.subList(allMessages.size() - RAW_MESSAGE_WINDOW, allMessages.size())
                : allMessages;

        return new ConversationContext(conversation.getSummary(), recentWindow);
    }

    private void summarizeOlderMessages(Conversation conversation, List<Message> allMessages) {
        List<Message> toSummarize = allMessages.size() > RAW_MESSAGE_WINDOW
                ? allMessages.subList(0, allMessages.size() - RAW_MESSAGE_WINDOW)
                : List.of();

        if (toSummarize.isEmpty()) return;

        String transcript = toSummarize.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String existingSummary = conversation.getSummary();
        String promptContent = existingSummary != null
                ? "Previous summary: " + existingSummary + "\n\nNew messages to fold in:\n" + transcript
                : transcript;

        LlmClient.LlmResponse response = llmClient.chat(List.of(
                LlmClient.LlmMessage.system(SUMMARY_SYSTEM_PROMPT),
                LlmClient.LlmMessage.user(promptContent)
        ));

        conversation.setSummary(response.content());
        conversation.setSummarizedUpToMessageId(toSummarize.get(toSummarize.size() - 1).getId());
        conversationRepository.save(conversation);
    }

    private int countUnsummarized(List<Message> allMessages, java.util.UUID summarizedUpToId) {
        if (summarizedUpToId == null) return allMessages.size();
        int index = -1;
        for (int i = 0; i < allMessages.size(); i++) {
            if (allMessages.get(i).getId().equals(summarizedUpToId)) { index = i; break; }
        }
        return index == -1 ? allMessages.size() : allMessages.size() - index - 1;
    }

    public record ConversationContext(String summary, List<Message> recentMessages) {}
}