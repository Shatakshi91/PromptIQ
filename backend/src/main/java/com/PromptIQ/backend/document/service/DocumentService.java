package com.PromptIQ.backend.document.service;

import com.PromptIQ.backend.auth.repository.UserRepository;
import com.PromptIQ.backend.common.dto.PageResponse;
import com.PromptIQ.backend.common.exception.ApiException;
import com.PromptIQ.backend.document.dto.DocumentResponse;
import com.PromptIQ.backend.document.entity.Document;
import com.PromptIQ.backend.document.entity.DocumentChunk;
import com.PromptIQ.backend.document.entity.DocumentStatus;
import com.PromptIQ.backend.document.repository.DocumentChunkRepository;
import com.PromptIQ.backend.document.repository.DocumentRepository;
import com.PromptIQ.backend.embedding.client.EmbeddingClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB cap

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final TextExtractionService extractionService;
    private final TextChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            UserRepository userRepository,
            TextExtractionService extractionService,
            TextChunkingService chunkingService,
            EmbeddingClient embeddingClient
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.extractionService = extractionService;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public DocumentResponse upload(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds 10MB limit");
        }

        Document document = Document.builder()
                .user(userRepository.getReferenceById(userId))
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .status(DocumentStatus.PENDING)
                .build();
        document = documentRepository.save(document);

        try {
            String text = extractionService.extractText(file);
            List<String> chunks = chunkingService.chunk(text);

            if (chunks.isEmpty()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Document produced no usable text chunks");
            }

            int index = 0;
            for (String chunkText : chunks) {
                float[] vector = embeddingClient.embed(chunkText);
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(index++)
                        .content(chunkText)
                        .embedding(vector)
                        .build();
                chunkRepository.save(chunk);
            }

            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);

        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown processing error");
            documentRepository.save(document);

            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to process document: " + e.getMessage());
        }

        long chunkCount = chunkRepository.countByDocumentId(document.getId());
        return DocumentResponse.from(document, chunkCount);
    }

    public PageResponse<DocumentResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(
                documentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable),
                d -> DocumentResponse.from(d, chunkRepository.countByDocumentId(d.getId()))
        );
    }

    public DocumentResponse get(UUID userId, UUID documentId) {
        Document document = getOwnedDocumentOrThrow(userId, documentId);
        return DocumentResponse.from(document, chunkRepository.countByDocumentId(documentId));
    }
    @Transactional
    public void delete(UUID userId, UUID documentId) {
        Document document = getOwnedDocumentOrThrow(userId, documentId);
        chunkRepository.deleteAllByDocumentId(documentId);
        documentRepository.delete(document);
    }

    private Document getOwnedDocumentOrThrow(UUID userId, UUID documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
    }
}