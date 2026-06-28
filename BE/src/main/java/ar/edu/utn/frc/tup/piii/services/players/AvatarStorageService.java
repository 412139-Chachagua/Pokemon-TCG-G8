package ar.edu.utn.frc.tup.piii.services.players;

import ar.edu.utn.frc.tup.piii.exceptions.StorageException;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class AvatarStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);

    private final Path uploadDir;

    public AvatarStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        validate(file);
        String extension = resolveExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        String relativePath = "avatars/" + filename;

        try {
            Files.createDirectories(uploadDir);
            Path destination = uploadDir.resolve(filename);

            log.info("UploadDir: {}", uploadDir);
            log.info("Destination: {}", destination);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("No se pudo guardar el avatar.", e);
        }

        return relativePath;
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("El archivo está vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Tipo de archivo no permitido. Use PNG, JPG o WEBP");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("El archivo excede el tamaño máximo de 2 MB");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return "png";

        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }
}
