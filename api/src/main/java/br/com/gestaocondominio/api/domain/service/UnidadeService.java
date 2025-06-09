package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;

    public UnidadeService(UnidadeRepository unidadeRepository, CondominioRepository condominioRepository) {
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
    }

    public Unidade cadastrarUnidade(Unidade unidade) {
        if (unidade.getCondominio() == null || unidade.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a unidade.");
        }
        Condominio condominio = condominioRepository.findById(unidade.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + unidade.getCondominio().getConCod()));

        if (unidade.getUniNumero() == null || unidade.getUniNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Número da unidade não pode ser vazio.");
        }

        Optional<Unidade> unidadeExistente = unidadeRepository.findByUniNumeroAndCondominio(unidade.getUniNumero(), condominio);
        if (unidadeExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe uma unidade com este número para o condomínio informado: " + unidade.getUniNumero());
        }

        if (unidade.getUniStatusOcupacao() == null || String.valueOf(unidade.getUniStatusOcupacao()).trim().isEmpty()) {
            unidade.setUniStatusOcupacao('D');
        } else {
            char status = Character.toUpperCase(unidade.getUniStatusOcupacao());
            if (status != 'O' && status != 'D' && status != 'V' && status != 'A') {
                throw new IllegalArgumentException("Status de ocupação inválido. Use 'O' (Ocupado), 'D' (Desocupado), 'V' (Em Venda) ou 'A' (Alugado).");
            }
            unidade.setUniStatusOcupacao(status);
        }

        if (unidade.getUniValorTaxaCondominio() == null || unidade.getUniValorTaxaCondominio().compareTo(BigDecimal.ZERO) < 0) {
             throw new IllegalArgumentException("Valor da taxa de condomínio não pode ser nulo ou negativo.");
        }


        unidade.setUniDtCadastro(LocalDateTime.now());
        unidade.setUniDtAtualizacao(LocalDateTime.now());

        return unidadeRepository.save(unidade);
    }

    public Optional<Unidade> buscarUnidadePorId(Integer id) {
        return unidadeRepository.findById(id);
    }

    public List<Unidade> listarTodasUnidades() {
        return unidadeRepository.findAll();
    }

    public Unidade atualizarUnidade(Integer id, Unidade unidadeAtualizada) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        if (unidadeAtualizada.getCondominio() != null && !unidadeAtualizada.getCondominio().getConCod().equals(unidadeExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de uma unidade existente.");
        }

        if (!unidadeAtualizada.getUniNumero().equalsIgnoreCase(unidadeExistente.getUniNumero())) {
            Optional<Unidade> unidadeConflito = unidadeRepository.findByUniNumeroAndCondominio(unidadeAtualizada.getUniNumero(), unidadeExistente.getCondominio());
            if (unidadeConflito.isPresent() && !unidadeConflito.get().getUniCod().equals(id)) {
                throw new IllegalArgumentException("Novo número de unidade já cadastrado para o condomínio: " + unidadeAtualizada.getUniNumero());
            }
        }

        unidadeExistente.setUniNumero(unidadeAtualizada.getUniNumero());
        
        if (unidadeAtualizada.getUniStatusOcupacao() == null || String.valueOf(unidadeAtualizada.getUniStatusOcupacao()).trim().isEmpty()) {
            throw new IllegalArgumentException("Status de ocupação da unidade não pode ser vazio na atualização.");
        } else {
            char status = Character.toUpperCase(unidadeAtualizada.getUniStatusOcupacao());
            if (status != 'O' && status != 'D' && status != 'V' && status != 'A') {
                throw new IllegalArgumentException("Status de ocupação inválido. Use 'O' (Ocupado), 'D' (Desocupado), 'V' (Em Venda) ou 'A' (Alugado).");
            }
            unidadeExistente.setUniStatusOcupacao(status);
        }

        
        if (unidadeAtualizada.getUniValorTaxaCondominio() == null || unidadeAtualizada.getUniValorTaxaCondominio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da taxa de condomínio não pode ser nulo ou negativo na atualização.");
        }
        unidadeExistente.setUniValorTaxaCondominio(unidadeAtualizada.getUniValorTaxaCondominio());
        
        unidadeExistente.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidadeExistente);
    }
}