package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.controller.dto.AreaComumTurnoDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.AreaComumTurno;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaComumServiceImpl implements AreaComumService {

    /** Papéis que gerenciam áreas comuns e aprovam reservas, como no menu e na tela de reservas. */
    private static final Set<UserRole> PAPEIS_DE_GESTAO = Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);

    private final AreaComumRepository areaComumRepository;
    private final CondominioService condominioService;
    private final UsuarioCondominioService usuarioCondominioService;
    private final ReservaRepository reservaRepository;

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

        preencherCampos(areaComum, dto);

        if (dto.getTurnos() != null) {
            for (AreaComumTurnoDTO turnoDTO : dto.getTurnos()) {
                areaComum.getTurnos().add(novoTurno(areaComum, turnoDTO));
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

    // ---------------------------------------------------------------------------------------------------------
    // API v1
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public Set<Integer> condominiosGerenciados(Pessoa usuario) {
        // Só vínculos ativos contam, como nas permissões do login (ROLE_<PAPEL>_<conCod>)
        return usuarioCondominioService.findByPessoa(usuario).stream()
                .filter(vinculo -> Boolean.TRUE.equals(vinculo.getUscAtivoAssociacao()))
                .filter(vinculo -> PAPEIS_DE_GESTAO.contains(vinculo.getUscPapel()))
                .map(UsuarioCondominio::getConCod)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean podeGerenciar(Pessoa usuario, Integer conCod) {
        return Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin()) || condominiosGerenciados(usuario).contains(conCod);
    }

    @Override
    public boolean podeGerenciarAlgum(Pessoa usuario) {
        return Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin()) || !condominiosGerenciados(usuario).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaComum> listarParaGestao(Pessoa usuario, Integer conCod) {
        if (conCod != null) {
            exigirGestao(usuario, conCod);
            return areaComumRepository.findByCondominioConCodOrderByNomeAsc(conCod);
        }
        if (Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())) {
            return areaComumRepository.findAllByOrderByNomeAsc();
        }
        Set<Integer> geridos = condominiosGerenciados(usuario);
        if (geridos.isEmpty()) {
            throw new AccessDeniedException("Você não gerencia áreas comuns de nenhum condomínio.");
        }
        return areaComumRepository.findByCondominioConCodInOrderByNomeAsc(geridos);
    }

    @Override
    @Transactional(readOnly = true)
    public AreaComum buscarParaGestao(Integer areCod, Pessoa usuario) {
        AreaComum area = areaComumRepository.findById(areCod)
                .orElseThrow(() -> new EntityNotFoundException("Área comum não encontrada."));
        exigirGestao(usuario, area.getCondominio().getConCod());
        return area;
    }

    @Override
    @Transactional
    public AreaComum criar(AreaComumRequestDTO dto, Pessoa usuario) {
        if (dto.getConCod() == null) {
            throw new IllegalArgumentException("Informe o condomínio.");
        }
        exigirGestao(usuario, dto.getConCod());

        AreaComum area = new AreaComum();
        area.setCondominio(condominioService.buscarCondominioPorId(dto.getConCod())
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado.")));
        area.setTurnos(new ArrayList<>());
        preencherCampos(area, dto);
        if (dto.getTurnos() != null) {
            for (AreaComumTurnoDTO turnoDTO : dto.getTurnos()) {
                if (turnoDTO.getTurCod() != null) {
                    throw new IllegalArgumentException("Uma área nova não pode trazer turnos já cadastrados.");
                }
                area.getTurnos().add(novoTurno(area, turnoDTO));
            }
        }
        return areaComumRepository.save(area);
    }

    @Override
    @Transactional
    public AreaComum atualizar(Integer areCod, AreaComumRequestDTO dto, Pessoa usuario) {
        // A área continua no condomínio em que foi cadastrada: o condomínio do pedido é ignorado
        AreaComum area = buscarParaGestao(areCod, usuario);
        preencherCampos(area, dto);
        atualizarTurnos(area, dto.getTurnos() == null ? List.of() : dto.getTurnos());
        return areaComumRepository.save(area);
    }

    @Override
    @Transactional
    public void excluir(Integer areCod, Pessoa usuario) {
        AreaComum area = buscarParaGestao(areCod, usuario);
        // Sem esta conferência, o banco recusaria a exclusão (as reservas apontam para a área) com um erro genérico
        if (reservaRepository.existsByAreaComumAreCod(areCod)) {
            throw new IllegalArgumentException(
                    "Esta área comum já tem reservas e não pode ser excluída. Para tirá-la de uso, inative-a.");
        }
        areaComumRepository.delete(area);
    }

    private void exigirGestao(Pessoa usuario, Integer conCod) {
        if (!podeGerenciar(usuario, conCod)) {
            throw new AccessDeniedException("Você não gerencia as áreas comuns deste condomínio.");
        }
    }

    /**
     * Na edição pela API, os turnos são comparados pelo código: o que vem com código é alterado, o que vem sem é
     * criado e o que não vem é removido. Assim as reservas continuam apontando para o mesmo turno. Turno com reservas
     * não pode ser removido (o banco recusaria); para tirá-lo de uso, ele deve vir com {@code ativo = false}.
     */
    private void atualizarTurnos(AreaComum area, List<AreaComumTurnoDTO> turnosPedidos) {
        Map<Integer, AreaComumTurno> existentes = new HashMap<>();
        area.getTurnos().forEach(turno -> existentes.put(turno.getTurCod(), turno));

        Set<Integer> mantidos = new HashSet<>();
        List<AreaComumTurno> novos = new ArrayList<>();
        for (AreaComumTurnoDTO pedido : turnosPedidos) {
            if (pedido.getTurCod() == null) {
                novos.add(novoTurno(area, pedido));
                continue;
            }
            AreaComumTurno turno = existentes.get(pedido.getTurCod());
            if (turno == null) {
                throw new IllegalArgumentException("O turno " + pedido.getTurCod() + " não pertence a esta área.");
            }
            if (!mantidos.add(pedido.getTurCod())) {
                throw new IllegalArgumentException("O turno " + pedido.getTurCod() + " foi informado mais de uma vez.");
            }
            turno.setNome(pedido.getNome());
            turno.setHoraInicio(pedido.getHoraInicio());
            turno.setHoraFim(pedido.getHoraFim());
            turno.setAtivo(pedido.getAtivo() != null ? pedido.getAtivo() : true);
        }

        for (AreaComumTurno turno : existentes.values()) {
            if (!mantidos.contains(turno.getTurCod()) && reservaRepository.existsByTurnoTurCod(turno.getTurCod())) {
                throw new IllegalArgumentException("O turno \"" + turno.getNome()
                        + "\" já tem reservas e não pode ser removido. Para tirá-lo de uso, desative-o.");
            }
        }

        area.getTurnos().removeIf(turno -> !mantidos.contains(turno.getTurCod()));
        area.getTurnos().addAll(novos);
    }

    private void preencherCampos(AreaComum areaComum, AreaComumRequestDTO dto) {
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
    }

    private AreaComumTurno novoTurno(AreaComum areaComum, AreaComumTurnoDTO turnoDTO) {
        return AreaComumTurno.builder()
                .areaComum(areaComum)
                .nome(turnoDTO.getNome())
                .horaInicio(turnoDTO.getHoraInicio())
                .horaFim(turnoDTO.getHoraFim())
                .ativo(turnoDTO.getAtivo() != null ? turnoDTO.getAtivo() : true)
                .build();
    }
}
