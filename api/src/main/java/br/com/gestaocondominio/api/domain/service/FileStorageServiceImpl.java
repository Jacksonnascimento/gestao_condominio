package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads");
    private static final String COMUNICADOS_DIR = "comunicados";

    private static final String OCORRENCIAS_DIR = "ocorrencias";

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation.resolve(COMUNICADOS_DIR));

            Files.createDirectories(rootLocation.resolve(OCORRENCIAS_DIR));
        } catch (IOException e) {
            throw new StorageException("Não foi possível inicializar os diretórios de uploads", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subdiretorio) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {

            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex >= 0) {
                fileExtension = originalFilename.substring(lastDotIndex);
            }
        } catch (Exception e) {

            fileExtension = "";
        }

        String newFilename = UUID.randomUUID().toString() + fileExtension;
        Path destinationDirectory = this.rootLocation.resolve(subdiretorio);

        try {

            Files.createDirectories(destinationDirectory);

            if (originalFilename.contains("..")) {

                throw new StorageException("Nome de arquivo inválido: " + originalFilename);
            }

            Path destinationFile = destinationDirectory.resolve(newFilename).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(destinationDirectory.toAbsolutePath())) {
                throw new StorageException("Não é possível salvar o arquivo fora do diretório atual.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return Paths.get(subdiretorio).resolve(newFilename).toString().replace("\\", "/"); // Garante barras /

        } catch (IOException e) {
            throw new StorageException("Falha ao salvar o arquivo.", e);
        }
    }

    @Override
    public Resource loadAsResource(String filename, String subdiretorio) {
        try {
            Path file = rootLocation.resolve(subdiretorio).resolve(filename).normalize().toAbsolutePath();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {

                throw new StorageException("Arquivo não encontrado: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Não foi possível ler o arquivo (URL mal formada): " + filename, e);
        } catch (StorageException e) {

            throw e;
        } catch (Exception e) {

            throw new StorageException("Não foi possível ler o arquivo: " + filename, e);
        }
    }

    @Override
    public void delete(String filename, String subdiretorio) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        try {
            Path file = rootLocation.resolve(subdiretorio).resolve(filename).normalize().toAbsolutePath();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Falha ao excluir o arquivo: " + filename, e);
        }
    }
}