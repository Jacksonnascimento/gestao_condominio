package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.*;
import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public interface OcorrenciaService {

    Page<OcorrenciaResumoDTO> consultarOcorrencias(
            Pessoa usuarioLogado,
            Integer condominioId,
            String buscaUnidade,
            String buscaTitulo,
            OcorrenciaTipo tipo,
            OcorrenciaStatus status,
            LocalDate inicioApos,
            LocalDate fimAntes,
            Pageable pageable);

    Map<String, Long> contarOcorrenciasPorStatusEPeriodo(
            Pessoa usuarioLogado,
            Integer condominioId,
            String buscaUnidade,
            String buscaTitulo,
            OcorrenciaTipo tipo,
            OcorrenciaStatus status,
            LocalDate inicioApos,
            LocalDate fimAntes);

    OcorrenciaDetalhesDTO buscarPorIdDetalhes(Integer id, Pessoa usuarioLogado);

    Ocorrencia criarOcorrencia(OcorrenciaRequestDTO dto, Pessoa usuarioLogado);

    OcorrenciaComentarioDTO adicionarComentario(Integer ocorrenciaId, OcorrenciaComentarioRequestDTO dto,
            Pessoa usuarioLogado);

    OcorrenciaAnexoDTO adicionarAnexo(Integer ocorrenciaId, MultipartFile anexo, Pessoa usuarioLogado);

    void excluirAnexo(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado);

    Resource carregarAnexoComoRecurso(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado);

    String getNomeOriginalAnexo(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado);

    Ocorrencia finalizarOcorrencia(Integer ocorrenciaId, OcorrenciaFinalizarRequestDTO dto, Pessoa usuarioLogado);

    Ocorrencia buscarOcorrenciaPorIdEValidarAcesso(Integer id, Pessoa usuarioLogado, boolean edicao);
}