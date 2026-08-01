package com.PromptIQ.backend.workflow.impl;

import com.PromptIQ.backend.llm.client.LlmClient;
import com.PromptIQ.backend.tool.ToolExecutionService;
import com.PromptIQ.backend.workflow.Workflow;
import com.PromptIQ.backend.workflow.WorkflowProgressListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResearchWorkflow implements Workflow {

    private static final Pattern NUMBERED_LINE = Pattern.compile("^\\s*\\d+[.)]\\s*(.+)$");

    private final LlmClient llmClient;
    private final ToolExecutionService toolExecutionService;

    public ResearchWorkflow(LlmClient llmClient, ToolExecutionService toolExecutionService) {
        this.llmClient = llmClient;
        this.toolExecutionService = toolExecutionService;
    }

    @Override
    public String key() { return "research"; }

    @Override
    public String displayName() { return "Research & Summarize"; }

    @Override
    public String description() {
        return "Breaks a topic into focused sub-questions, researches each independently "
                + "(using tools where helpful), then synthesizes a cohesive report.";
    }

    @Override
    public String run(UUID userId, String topic, WorkflowProgressListener progress) {
        progress.onStep("Generating focused sub-questions for: " + topic);
        List<String> subQuestions = generateSubQuestions(topic);

        StringBuilder findings = new StringBuilder();
        for (int i = 0; i < subQuestions.size(); i++) {
            String question = subQuestions.get(i);
            progress.onStep("Researching (" + (i + 1) + "/" + subQuestions.size() + "): " + question);
            String answer = researchQuestion(question);
            findings.append("Q: ").append(question).append("\nA: ").append(answer).append("\n\n");
        }

        progress.onStep("Synthesizing final report");
        return synthesize(topic, findings.toString());
    }

    private List<String> generateSubQuestions(String topic) {
        String systemPrompt = """
                You break a broad topic into exactly 3 focused, non-overlapping sub-questions
                that together give a well-rounded understanding of it. Respond with ONLY the
                3 questions, one per line, numbered 1-3. No preamble, no extra commentary.
                """;

        LlmClient.LlmResponse response = llmClient.chat(List.of(
                LlmClient.LlmMessage.system(systemPrompt),
                LlmClient.LlmMessage.user("Topic: " + topic)
        ));

        List<String> questions = response.content().lines()
                .map(line -> {
                    Matcher m = NUMBERED_LINE.matcher(line);
                    return m.matches() ? m.group(1).trim() : line.trim();
                })
                .filter(line -> !line.isBlank())
                .limit(3)
                .toList();

        // Defensive fallback: if the model didn't follow the format, at least don't crash the workflow
        return questions.isEmpty() ? List.of(topic) : questions;
    }

    private String researchQuestion(String question) {
        // Reuses Feature 9's tool infrastructure — the model can call web_search here if it
        // judges it necessary, same tool-calling loop pattern as ChatOrchestrationService,
        // kept intentionally simple (single iteration, no chained multi-tool loop) since this
        // is a sub-step within a larger workflow, not the main conversational loop.
        List<LlmClient.ToolSpec> tools = toolExecutionService.availableToolSpecs();

        LlmClient.LlmResponse response = llmClient.chat(List.of(
                LlmClient.LlmMessage.system("Answer the question concisely and factually, in 2-4 sentences."),
                LlmClient.LlmMessage.user(question)
        ), tools);

        if (response.hasToolCalls()) {
            List<LlmClient.LlmMessage> followUp = new java.util.ArrayList<>(List.of(
                    LlmClient.LlmMessage.system("Answer the question concisely and factually, in 2-4 sentences."),
                    LlmClient.LlmMessage.user(question),
                    LlmClient.LlmMessage.assistantWithToolCalls(response.toolCalls())
            ));
            for (LlmClient.ToolCallRequest call : response.toolCalls()) {
                String result = toolExecutionService.execute(call);
                followUp.add(LlmClient.LlmMessage.toolResult(call.id(), result));
            }
            response = llmClient.chat(followUp, tools);
        }

        return response.content();
    }

    private String synthesize(String topic, String findings) {
        String systemPrompt = """
                You write a clear, well-organized report synthesizing research findings into
                a cohesive narrative — not just a list of Q&A pairs. Use short paragraphs.
                """;

        LlmClient.LlmResponse response = llmClient.chat(List.of(
                LlmClient.LlmMessage.system(systemPrompt),
                LlmClient.LlmMessage.user("Topic: " + topic + "\n\nResearch findings:\n" + findings)
        ));

        return response.content();
    }
}