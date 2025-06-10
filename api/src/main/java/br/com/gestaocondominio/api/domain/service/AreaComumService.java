package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaAreaComumRepository; // IMPORT ATIVO AGORA
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AreaComumService {

    private final AreaComumRepository areaComumRepository;
    private final CondominioRepository condominioRepository;
    private final ReservaAreaComumRepository reservaAreaComumRepository;

    public AreaComumService(AreaComumRepository areaComumRepository,
                            CondominioRepository condominioRepository,
                            ReservaAreaComumRepository reservaAreaComumRepository) {
        this.areaComumRepository = areaComumRepository;
        this.condominioRepository = condominioRepository;
        this.reservaAreaComumRepository = reservaAreaComumRepository;
    }

    public AreaComum cadastrarAreaComum(AreaComum areaComum) {
        if (areaComum.getCondominio() == null || areaComum.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a área comum.");
        }
        condominioRepository.findById(areaComum.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + areaComum.getCondominio().getConCod()));

        areaComum.setArcDtCadastro(LocalDateTime.now());
        areaComum.setArcDtAtualizacao(LocalDateTime.now());

        if (areaComum.getArcPermiteReserva() == null) {
            areaComum.setArcPermiteReserva(true);
        }

        return areaComumRepository.save(areaComum);
    }

    public Optional<AreaComum> buscarAreaComumPorId(Integer id) {
        return areaComumRepository.findById(id);
    }

    public List<AreaComum> listarTodasAreasComuns() {
        return areaComumRepository.findAll();
    }

    public AreaComum atualizarAreaComum(Integer id, AreaComum areaComumAtualizada) {
        AreaComum areaComumExistente = areaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área comum não encontrada com o ID: " + id));

        if (areaComumAtualizada.getCondominio() != null && !areaComumAtualizada.getCondominio().getConCod().equals(areaComumExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o condomínio de uma área comum existente.");
        }

        areaComumExistente.setArcNome(areaComumAtualizada.getArcNome());
        areaComumExistente.setArcDescricao(areaComumAtualizada.getArcDescricao());
        areaComumExistente.setArcRegrasUso(areaComumAtualizada.getArcRegrasUso());
        areaComumExistente.setArcCapacidadeMaxima(areaComumAtualizada.getArcCapacidadeMaxima());
        areaComumExistente.setArcPermiteReserva(areaComumAtualizada.getArcPermiteReserva());

        areaComumExistente.setArcDtAtualizacao(LocalDateTime.now());
        return areaComumRepository.save(areaComumExistente);
    }

    public void deletarAreaComum(Integer id) {
        AreaComum areaComum = areaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área comum não encontrada com o ID: " + id));

        if (!reservaAreaComumRepository.findByAreaComum(areaComum).isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir a área comum pois existem reservas associadas a ela.");
        }

        areaComumRepository.deleteById(id);
    }
}