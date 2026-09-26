package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoResposta;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ComunicadoService {

    // Recebem quem está logado e conferem a permissão. O condomínio é o escolhido na tela: sem ele, vale um dos
    // condomínios da pessoa; com ele, a pessoa precisa ter acesso ativo a ele (o administrador geral tem a todos).

    /**
     * Comunicados que a pessoa pode ver no condomínio, com os filtros da tela, dos mais recentes para os mais antigos.
     * Para o administrador geral, sem condomínio, os de todos.
     */
    Page<ComunicadoResposta> consultarVisiveis(Pessoa usuarioLogado, Integer condominioId, String titulo,
                                               String mensagem, PublicoDestino publicoDestino, Boolean urgente,
                                               Pageable pageable);

    /** Um comunicado, se a pessoa o vê em algum dos condomínios de destino. */
    ComunicadoResposta buscarVisivel(Integer id, Pessoa usuarioLogado);

    /** Se a pessoa publica comunicados no condomínio: administrador geral, ou síndico e administração dele. */
    boolean podeGerenciar(Pessoa usuarioLogado, Integer condominioId);

    /** Publica no condomínio (ou, para o administrador geral, nos condomínios escolhidos no pedido). */
    ComunicadoResposta criar(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado,
                             Integer condominioId);

    ComunicadoResposta atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado);

    void excluir(Integer id, Pessoa usuarioLogado);

    /** Arquivo anexado ao comunicado, com o nome com que foi enviado, se a pessoa pode ver o comunicado. */
    AnexoDoComunicado carregarAnexo(Integer id, Pessoa usuarioLogado);

    record AnexoDoComunicado(Resource arquivo, String nome) {
    }
}
