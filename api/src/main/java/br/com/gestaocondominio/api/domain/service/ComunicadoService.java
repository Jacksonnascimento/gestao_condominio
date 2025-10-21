package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Comunicado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


public interface ComunicadoService {

    void criar(ComunicadoRequestDTO dto, MultipartFile anexo);

    void atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo);

    void excluir(Integer id);

    Comunicado getComunicadoById(Integer id);

    Page<Comunicado> consultar(
            String titulo,
            String mensagem,
            String publicoDestino,
            Boolean isUrgente,
            Pageable pageable);
}