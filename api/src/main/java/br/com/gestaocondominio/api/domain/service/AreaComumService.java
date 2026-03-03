package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;

import java.util.List;

public interface AreaComumService {
    AreaComum salvar(AreaComumRequestDTO dto);
    AreaComum buscarPorId(Integer areCod);
    List<AreaComum> listarPorCondominio(Integer conCod);
    List<AreaComum> listarAtivasPorCondominio(Integer conCod);
    void excluir(Integer areCod);
}