package com.falconenergy.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProofOfDeliveryStorageService {
    /**
     * Securely validates, renames, and stores a POD photo file.
     * @param file The uploaded multipart file
     * @param deliveryId The ID of the associated delivery
     * @return The generated safe unique filename or relative path
     */
    String storeFile(MultipartFile file, Long deliveryId);

    /**
     * Securely retrieves the bytes of a stored file.
     * @param filepath The stored file path or reference
     * @return The byte contents of the file
     */
    byte[] loadFile(String filepath);
}
