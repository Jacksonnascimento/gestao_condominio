package br.com.gestaocondominio.api.domain.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    void init();

    String store(MultipartFile file, String subdiretorio);

    Resource loadAsResource(String filename, String subdiretorio);

    void delete(String filename, String subdiretorio);
}