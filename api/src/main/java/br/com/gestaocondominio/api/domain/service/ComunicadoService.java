package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoResposta;
import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ComunicadoService {

    Comunicado criar(ComunicadoRequestDTO dto, MultipartFile anexo);

    Comunicado atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo);

    void excluir(Integer id);

    Comunicado getComunicadoById(Integer id);

    Page<Comunicado> consultar(
            String titulo,
            String mensagem,
            String publicoDestino,
            Boolean isUrgente,
            Pageable pageable);

    // ---- Usados pela API /api/v1 ----
    // Recebem quem está logado e conferem a permissão. Os métodos acima são os das telas antigas: listam só o que a
    // pessoa pode ver, mas não conferem quem cria, altera, exclui ou abre um comunicado pelo id.

    /** Comunicados que a pessoa pode ver, com os filtros da tela, dos mais recentes para os mais antigos. */
    Page<ComunicadoResposta> consultarVisiveis(Pessoa usuarioLogado, String titulo, String mensagem,
                                               PublicoDestino publicoDestino, Boolean urgente, Pageable pageable);

    /** Um comunicado, se a pessoa pode vê-lo pela mesma regra da listagem. */
    ComunicadoResposta buscarVisivel(Integer id, Pessoa usuarioLogado);

    /** Se a pessoa publica comunicados: administrador geral, ou síndico e administração do seu condomínio. */
    boolean podeGerenciar(Pessoa usuarioLogado);

    ComunicadoResposta criar(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado);

    ComunicadoResposta atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado);

    void excluir(Integer id, Pessoa usuarioLogado);

    /** Arquivo anexado ao comunicado, se a pessoa pode ver o comunicado. */
    Resource carregarAnexo(Integer id, Pessoa usuarioLogado);
}