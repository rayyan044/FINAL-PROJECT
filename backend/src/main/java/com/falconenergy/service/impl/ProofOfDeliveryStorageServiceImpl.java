package com.falconenergy.service.impl;

import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.service.ProofOfDeliveryStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProofOfDeliveryStorageServiceImpl implements ProofOfDeliveryStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private final Path fileStorageLocation;

    public ProofOfDeliveryStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("uploads/pod").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, Long deliveryId) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        // Validate File Size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB.");
        }

        // Validate Content Type / Extension
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && 
                                    !contentType.equals("image/jpeg") && 
                                    !contentType.equals("image/jpg"))) {
            throw new BadRequestException("Invalid file type. Only PNG and JPEG/JPG images are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Cannot store file with relative path outside current directory: " + originalFilename);
        }

        // Extract extension safely
        String extension = "jpg";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex > 0) {
            extension = originalFilename.substring(extIndex + 1).toLowerCase();
        }

        // Generate safe unique filename
        String filename = "pod-" + deliveryId + "-" + UUID.randomUUID() + "." + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(filename).normalize();
            
            // Double check that we are still in the safe directory
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("Cannot store file outside current directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + filename + ". Please try again!", e);
        }
    }

    @Override
    public byte[] loadFile(String filename) {
        try {
            if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new BadRequestException("Invalid file request path.");
            }

            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            
            // Validate path traversal
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("Access denied. Attempted path traversal.");
            }

            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("File not found " + filename);
        }
    }
}
