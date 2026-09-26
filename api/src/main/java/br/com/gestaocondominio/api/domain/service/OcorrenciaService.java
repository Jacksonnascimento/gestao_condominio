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

    /**
     * Se a pessoa gerencia ocorrências em algum condomínio (administrador geral, síndico, administração ou funcionário
     * administrativo). É o que a tela usa para mostrar os filtros de data e os botões de gestão.
     */
    boolean podeGerenciarOcorrencias(Pessoa usuarioLogado);

    /**
     * Se a pessoa gerencia esta ocorrência: administrador geral, ou síndico, administração ou funcionário
     * administrativo com vínculo ativo no condomínio dela. Confere antes se a pessoa pode ver a ocorrência.
     */
    boolean gerenciaOcorrencia(Integer ocorrenciaId, Pessoa usuarioLogado);

    /**
     * Recusa (403) quem não gerencia a ocorrência. Na tela, comentários e anexos (ver, incluir, excluir e baixar) só
     * aparecem para quem gerencia; os métodos acima aceitam também o autor e os ocupantes da unidade.
     */
    void conferirGestao(Integer ocorrenciaId, Pessoa usuarioLogado);
}