package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UnidadeService {
    Unidade cadastrarUnidade(UnidadeRequestDTO dto);
    List<Unidade> listarTodasUnidades(boolean incluirInativas, String statusOcupacao, String busca);
    Optional<Unidade> buscarUnidadePorId(Integer id);
    Unidade atualizarUnidade(Integer id, UnidadeRequestDTO dto);
    Unidade inativarUnidade(Integer id);
    Unidade ativarUnidade(Integer id);
    void checkAdminOrSindicoPermission(Integer unidadeId);
    List<Unidade> findByCondominioId(Integer condominioId);
    List<Unidade> findAtivasByCondominioId(Integer condominioId);

    /**
     * Unidades que a pessoa pode ver: todas para o administrador geral; as dos condomínios em que é síndico,
     * administradora ou funcionário administrativo; e as que ela ocupa.
     */
    Page<Unidade> consultarUnidades(Pessoa usuario, Integer condominioId, String busca, UnidadeStatusOcupacao status,
                                    boolean incluirInativas, Pageable pageable);

    /** Quantidade por situação de ocupação (e o {@code TOTAL}), com os mesmos filtros de {@link #consultarUnidades}. */
    Map<String, Long> contarUnidadesPorStatus(Pessoa usuario, Integer condominioId, String busca,
                                              UnidadeStatusOcupacao status, boolean incluirInativas);

    /** Uma unidade, conferindo se a pessoa pode vê-la (mesma regra de {@link #consultarUnidades}). */
    Unidade buscarUnidadeVisivel(Integer id, Pessoa usuario);

    /** Se quem está logado cadastra e edita unidades em pelo menos um condomínio. */
    boolean podeGerenciarUnidades();
}