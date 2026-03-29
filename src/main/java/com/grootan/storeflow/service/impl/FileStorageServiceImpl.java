package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path uploadRootPath;
    private final String productImageDir;
    private final String avatarDir;

    public FileStorageServiceImpl(
            @Value("${app.file.upload-dir}") String uploadDir,
            @Value("${app.file.product-image-dir}") String productImageDir,
            @Value("${app.file.avatar-dir}") String avatarDir
    ) {
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.productImageDir = productImageDir;
        this.avatarDir = avatarDir;
    }

    @Override
    public String storeProductImage(MultipartFile file, Long productId) {
        validateImageFile(file);
        return storeFile(file, productImageDir, "product-" + productId);
    }

    @Override
    public String storeAvatar(MultipartFile file, Long userId) {
        validateImageFile(file);
        return storeFile(file, avatarDir, "user-" + userId);
    }

    @Override
    public Path loadAsPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new AppException("File path is empty", HttpStatus.NOT_FOUND);
        }

        Path filePath = uploadRootPath.resolve(relativePath).normalize();

        if (!filePath.startsWith(uploadRootPath)) {
            throw new AppException("Invalid file path", HttpStatus.BAD_REQUEST);
        }

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new AppException("File not found", HttpStatus.NOT_FOUND);
        }

        return filePath;
    }

    @Override
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("File is required", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException("File size must not exceed 5MB", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new AppException(
                    "Only JPEG, PNG, and WEBP image files are allowed",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Override
    public String detectContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String storeFile(MultipartFile file, String subDirectory, String filePrefix) {
        try {
            Path targetDir = uploadRootPath.resolve(subDirectory).normalize();
            Files.createDirectories(targetDir);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = filePrefix + "-" + UUID.randomUUID() + extension;

            Path targetFile = targetDir.resolve(filename).normalize();

            if (!targetFile.startsWith(targetDir)) {
                throw new AppException("Invalid file path", HttpStatus.BAD_REQUEST);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return subDirectory + "/" + filename;
        } catch (IOException e) {
            throw new AppException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(lastDotIndex).toLowerCase();
    }
}