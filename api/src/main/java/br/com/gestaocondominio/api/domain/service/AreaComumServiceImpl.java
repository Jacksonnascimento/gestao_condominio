package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.controller.dto.AreaComumTurnoDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.AreaComumTurno;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaComumServiceImpl implements AreaComumService {

    private final AreaComumRepository areaComumRepository;
    private final CondominioService condominioService;

    @Override
    @Transactional
    public AreaComum salvar(AreaComumRequestDTO dto) {
        AreaComum areaComum;

        if (dto.getAreCod() != null) {
            areaComum = buscarPorId(dto.getAreCod());
            areaComum.getTurnos().clear();
        } else {
            areaComum = new AreaComum();
            areaComum.setCondominio(condominioService.buscarCondominioPorId(dto.getConCod())
                    .orElseThrow(() -> new RuntimeException("Condomínio não encontrado.")));
            areaComum.setTurnos(new ArrayList<>());
        }

        areaComum.setNome(dto.getNome());
        areaComum.setDescricao(dto.getDescricao());
        areaComum.setTermosUso(dto.getTermosUso());
        areaComum.setCapacidadeMaxima(dto.getCapacidadeMaxima());
        areaComum.setPermiteConvidados(dto.getPermiteConvidados() != null ? dto.getPermiteConvidados() : false);
        areaComum.setLimiteConvidados(dto.getLimiteConvidados());
        areaComum.setDiasAntecedenciaMin(dto.getDiasAntecedenciaMin() != null ? dto.getDiasAntecedenciaMin() : 1);
        areaComum.setDiasAntecedenciaMax(dto.getDiasAntecedenciaMax() != null ? dto.getDiasAntecedenciaMax() : 30);
        areaComum.setAtiva(dto.getAtiva() != null ? dto.getAtiva() : true);
        areaComum.setTaxaValor(dto.getTaxaValor());

        if (dto.getTurnos() != null) {
            for (AreaComumTurnoDTO turnoDTO : dto.getTurnos()) {
                AreaComumTurno turno = AreaComumTurno.builder()
                        .areaComum(areaComum)
                        .nome(turnoDTO.getNome())
                        .horaInicio(turnoDTO.getHoraInicio())
                        .horaFim(turnoDTO.getHoraFim())
                        .ativo(turnoDTO.getAtivo() != null ? turnoDTO.getAtivo() : true)
                        .build();
                areaComum.getTurnos().add(turno);
            }
        }

        return areaComumRepository.save(areaComum);
    }

    @Override
    public AreaComum buscarPorId(Integer areCod) {
        return areaComumRepository.findById(areCod)
                .orElseThrow(() -> new RuntimeException("Área comum não encontrada."));
    }

    @Override
    public List<AreaComum> listarPorCondominio(Integer conCod) {
        return areaComumRepository.findByCondominioConCodOrderByNomeAsc(conCod);
    }

    @Override
    public List<AreaComum> listarAtivasPorCondominio(Integer conCod) {
        return areaComumRepository.findByCondominioConCodAndAtivaTrueOrderByNomeAsc(conCod);
    }

    @Override
    @Transactional
    public void excluir(Integer areCod) {
        AreaComum area = buscarPorId(areCod);
        areaComumRepository.delete(area);
    }
}