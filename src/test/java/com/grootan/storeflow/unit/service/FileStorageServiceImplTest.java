package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(
                "uploads",
                "products",
                "avatars"
        );
    }

    @Test
    void validateImageFileRejectsFileLargerThan5Mb() {
        byte[] oversizedContent = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                oversizedContent
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.validateImageFile(file)
        );

        assertEquals("File size must not exceed 5MB", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void validateImageFileRejectsDisallowedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.validateImageFile(file)
        );

        assertEquals("Only JPEG, PNG, and WEBP image files are allowed", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void validateImageFileRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.validateImageFile(file)
        );

        assertEquals("File is required", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    @Test
    void loadAsPathRejectsNullPath() {
        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.loadAsPath(null)
        );

        assertEquals("File path is empty", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}