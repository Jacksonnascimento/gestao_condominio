package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import java.util.List;
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
}