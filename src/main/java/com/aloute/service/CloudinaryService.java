
package com.aloute.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private static final Logger log =
            LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final Path uploadDir = Paths.get("uploads");

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {

        if (isConfigured(cloudName, apiKey, apiSecret)) {
            this.cloudinary = new Cloudinary(
                    ObjectUtils.asMap(
                            "cloud_name", cloudName,
                            "api_key", apiKey,
                            "api_secret", apiSecret,
                            "secure", true
                    )
            );

            log.info("Cloudinary initialized.");
        } else {
            this.cloudinary = null;
            log.warn(
                    "Cloudinary credentials missing; "
                    + "using local storage."
            );
        }

        createUploadDirectory();
    }

    private boolean isConfigured(
            String cloudName,
            String apiKey,
            String apiSecret) {

        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    public String uploadFile(
            byte[] data,
            String filename,
            String contentType) {

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException(
                    "Uploaded file must not be empty."
            );
        }

        if (cloudinary != null) {
            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                        data,
                        ObjectUtils.asMap(
                                "resource_type", "auto",
                                "public_id", UUID.randomUUID().toString()
                        )
                );

                Object url = result.get("secure_url");

                if (url != null) {
                    return url.toString();
                }

                log.warn("Cloudinary returned no secure URL.");

            } catch (Exception e) {
                log.error(
                        "Cloudinary upload failed; "
                        + "falling back to local storage.",
                        e
                );
            }
        }

        return uploadLocally(data, filename);
    }

    private String uploadLocally(byte[] data, String filename) {
        try {
            Files.createDirectories(uploadDir);

            String extension = "";

            if (filename != null && filename.contains(".")) {
                String candidate =
                        filename.substring(filename.lastIndexOf('.'));

                if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                    extension = candidate;
                }
            }

            String storedFilename =
                    UUID.randomUUID() + extension;

            Path base = uploadDir.toAbsolutePath().normalize();
            Path destination = base.resolve(storedFilename).normalize();

            if (!destination.startsWith(base)) {
                throw new IOException("Invalid upload path.");
            }

            Files.write(destination, data);

            log.info("File saved locally: {}", storedFilename);

            return "/uploads/" + storedFilename;

        } catch (IOException e) {
            log.error("Local file upload failed.", e);
            throw new RuntimeException("Upload failed.", e);
        }
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create upload directory.", e
            );
        }
    }

    public void deleteFile(String url) {
        // TODO: Implement deletion from Cloudinary or local storage.
    }
}
