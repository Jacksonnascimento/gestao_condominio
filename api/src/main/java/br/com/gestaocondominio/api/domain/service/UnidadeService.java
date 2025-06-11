package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.MoradorRepository;
import br.com.gestaocondominio.api.domain.repository.FinanceiroCobrancaRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaAreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.SolicitacaoManutencaoRepository;
// REMOVER A LINHA ABAIXO se estiver presente:
// import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;
    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final ReservaAreaComumRepository reservaAreaComumRepository;
    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;

    public UnidadeService(UnidadeRepository unidadeRepository,
                          CondominioRepository condominioRepository,
                          MoradorRepository moradorRepository,
                          FinanceiroCobrancaRepository financeiroCobrancaRepository,
                          ReservaAreaComumRepository reservaAreaComumRepository,
                          SolicitacaoManutencaoRepository solicitacaoManutencaoRepository
                        
                          ) {
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.moradorRepository = moradorRepository;
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.reservaAreaComumRepository = reservaAreaComumRepository;
        this.solicitacaoManutencaoRepository = solicitacaoManutencaoRepository;
       
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

        if (unidade.getUniAtiva() == null) {
            unidade.setUniAtiva(true); 
        }

        return unidadeRepository.save(unidade);
    }

    public Optional<Unidade> buscarUnidadePorId(Integer id) {
        return unidadeRepository.findById(id);
    }

    public List<Unidade> listarTodasUnidadesAtivas() {
        return unidadeRepository.findByUniAtiva(true);
    }

    public List<Unidade> listarTodasUnidades(boolean incluirInativas) {
        if (incluirInativas) {
            return unidadeRepository.findAll();
        }
        return unidadeRepository.findByUniAtiva(true);
    }

    public Unidade atualizarUnidade(Integer id, Unidade unidadeAtualizada) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        if (unidadeAtualizada.getCondominio() != null && !unidadeAtualizada.getCondominio().getConCod().equals(unidadeExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de uma unidade existente.");
        }

        if (unidadeAtualizada.getUniNumero() != null && !unidadeAtualizada.getUniNumero().equalsIgnoreCase(unidadeExistente.getUniNumero())) {
            Optional<Unidade> unidadeConflito = unidadeRepository.findByUniNumeroAndCondominio(unidadeAtualizada.getUniNumero(), unidadeExistente.getCondominio());
            if (unidadeConflito.isPresent() && !unidadeConflito.get().getUniCod().equals(id)) {
                throw new IllegalArgumentException("Novo número de unidade já cadastrado para o condomínio: " + unidadeAtualizada.getUniNumero());
            }
            unidadeExistente.setUniNumero(unidadeAtualizada.getUniNumero());
        }
        
        if (unidadeAtualizada.getUniStatusOcupacao() != null && String.valueOf(unidadeAtualizada.getUniStatusOcupacao()).trim().isEmpty()) {
            throw new IllegalArgumentException("Status de ocupação da unidade não pode ser vazio na atualização.");
        } else if (unidadeAtualizada.getUniStatusOcupacao() != null) {
            char status = Character.toUpperCase(unidadeAtualizada.getUniStatusOcupacao());
            if (status != 'O' && status != 'D' && status != 'V' && status != 'A') {
                throw new IllegalArgumentException("Status de ocupação inválido. Use 'O' (Ocupado), 'D' (Desocupado), 'V' (Em Venda) ou 'A' (Alugado).");
            }
            unidadeExistente.setUniStatusOcupacao(status);
        }

        if (unidadeAtualizada.getUniValorTaxaCondominio() != null && unidadeAtualizada.getUniValorTaxaCondominio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da taxa de condomínio não pode ser nulo ou negativo na atualização.");
        } else if (unidadeAtualizada.getUniValorTaxaCondominio() != null) {
            unidadeExistente.setUniValorTaxaCondominio(unidadeAtualizada.getUniValorTaxaCondominio());
        }
        
        if (unidadeAtualizada.getUniAtiva() != null) {
            unidadeExistente.setUniAtiva(unidadeAtualizada.getUniAtiva());
        }

        unidadeExistente.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidadeExistente);
    }

    public Unidade inativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));
        
        if (!moradorRepository.findByUnidade(unidade).isEmpty()) { 
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem moradores vinculados a ela.");
        }
        
        List<br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca> cobrancasAtivas = 
            financeiroCobrancaRepository.findByUnidadeAndFicStatusPagamentoNotIn(
                unidade, 
                java.util.Arrays.asList("PAGA", "CANCELADA") 
            );
        if (!cobrancasAtivas.isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem cobranças financeiras ATIVAS ou PENDENTES vinculadas a ela.");
        }
        
        List<br.com.gestaocondominio.api.domain.entity.ReservaAreaComum> reservasAtivas = 
            reservaAreaComumRepository.findByUnidadeAndStatusNotIn(
                unidade,
                java.util.Arrays.asList("REALIZADA", "CANCELADA") 
            );
        if (!reservasAtivas.isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem reservas de áreas comuns ATIVAS ou FUTURAS vinculadas a ela.");
        }
        
        List<br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao> solicitacoesAtivas = 
            solicitacaoManutencaoRepository.findByUnidadeAndStatusNotIn(
                unidade,
                java.util.Arrays.asList("CONCLUIDA", "CANCELADA") 
            );
        if (!solicitacoesAtivas.isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem solicitações de manutenção ATIVAS ou PENDENTES vinculadas a ela.");
        }
   
        unidade.setUniAtiva(false); 
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }

    public Unidade ativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));
        unidade.setUniAtiva(true); 
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }
}