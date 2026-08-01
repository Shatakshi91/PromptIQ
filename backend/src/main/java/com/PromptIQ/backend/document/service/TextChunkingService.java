package com.PromptIQ.backend.document.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private static final int CHUNK_SIZE = 1000;   // characters, not tokens — simple and predictable
    private static final int CHUNK_OVERLAP = 200;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String normalized = text.replaceAll("\\s+", " ").trim();

        if (normalized.isEmpty()) return chunks;

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(normalized.substring(start, end).trim());

            if (end == normalized.length()) break;
            start = end - CHUNK_OVERLAP; // step back for overlap
        }

        return chunks;
    }
}