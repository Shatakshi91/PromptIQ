package com.PromptIQ.backend.document.service;

import com.PromptIQ.backend.common.exception.ApiException;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractionService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        try {
            String text = tika.parseToString(file.getInputStream());
            if (text == null || text.isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No extractable text found in file");
            }
            return text;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to extract text from file: " + e.getMessage());
        }
    }
}