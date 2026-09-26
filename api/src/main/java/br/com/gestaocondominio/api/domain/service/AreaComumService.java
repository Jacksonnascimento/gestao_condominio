package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Pessoa;

import java.util.List;
import java.util.Set;

public interface AreaComumService {
    AreaComum salvar(AreaComumRequestDTO dto);
    AreaComum buscarPorId(Integer areCod);
    List<AreaComum> listarPorCondominio(Integer conCod);
    List<AreaComum> listarAtivasPorCondominio(Integer conCod);
    void excluir(Integer areCod);

    // ---- Usados pela API v1: recebem quem está logado e conferem se a pessoa pode agir no condomínio da área. ----

    /** Condomínios em que a pessoa gerencia áreas e reservas (síndico, administração ou funcionário adm. ativo). */
    Set<Integer> condominiosGerenciados(Pessoa usuario);

    /** Administrador geral, ou gestor ativo do condomínio. */
    boolean podeGerenciar(Pessoa usuario, Integer conCod);

    /** Se a pessoa gerencia algum condomínio (ou é administrador geral). */
    boolean podeGerenciarAlgum(Pessoa usuario);

    /**
     * Áreas que a pessoa gerencia, em ordem de nome: as do condomínio informado, ou as de todos os condomínios que
     * ela gerencia. Recusa quem não gerencia o condomínio (ou nenhum condomínio).
     */
    List<AreaComum> listarParaGestao(Pessoa usuario, Integer conCod);

    /** Uma área, se a pessoa gerencia o condomínio dela. */
    AreaComum buscarParaGestao(Integer areCod, Pessoa usuario);

    AreaComum criar(AreaComumRequestDTO dto, Pessoa usuario);

    /** Edita a área e os turnos, mantendo os turnos que vêm com código (e as reservas deles). */
    AreaComum atualizar(Integer areCod, AreaComumRequestDTO dto, Pessoa usuario);

    /** Exclui a área, se ela nunca foi reservada; se já foi, ela só pode ser inativada. */
    void excluir(Integer areCod, Pessoa usuario);
}
