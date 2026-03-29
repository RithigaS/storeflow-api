package com.grootan.storeflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    String storeProductImage(MultipartFile file, Long productId);

    String storeAvatar(MultipartFile file, Long userId);

    Path loadAsPath(String relativePath);

    void validateImageFile(MultipartFile file);

    String detectContentType(Path path);
}