package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SolicitacaoManutencaoService {

    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;
    private final CondominioRepository condominioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;
    private final TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository;

    public SolicitacaoManutencaoService(SolicitacaoManutencaoRepository solicitacaoManutencaoRepository,
                                        CondominioRepository condominioRepository,
                                        UnidadeRepository unidadeRepository,
                                        PessoaRepository pessoaRepository,
                                        TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository) {
        this.solicitacaoManutencaoRepository = solicitacaoManutencaoRepository;
        this.condominioRepository = condominioRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaRepository = pessoaRepository;
        this.tipoSolicitacaoManutencaoRepository = tipoSolicitacaoManutencaoRepository;
    }

    public List<SolicitacaoManutencao> listarTodasSolicitacoesManutencao() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return solicitacaoManutencaoRepository.findAll();
        }

        Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (!condoIdsComAcessoAdmin.isEmpty()) {
            List<Condominio> condominios = condominioRepository.findAllById(condoIdsComAcessoAdmin);
            return solicitacaoManutencaoRepository.findByCondominioIn(condominios);
        }

        return solicitacaoManutencaoRepository.findBySolicitante(userDetails.getPessoa());
    }

    public Optional<SolicitacaoManutencao> buscarSolicitacaoManutencaoPorId(Integer id) {
        Optional<SolicitacaoManutencao> solicitacaoOpt = solicitacaoManutencaoRepository.findById(id);
        solicitacaoOpt.ifPresent(this::checkPermissionToView);
        return solicitacaoOpt;
    }

    public SolicitacaoManutencao cadastrarSolicitacaoManutencao(SolicitacaoManutencao solicitacao) {
        if (solicitacao.getCondominio() == null || solicitacao.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a solicitação.");
        }
        condominioRepository.findById(solicitacao.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + solicitacao.getCondominio().getConCod()));

        if (solicitacao.getUnidade() != null && solicitacao.getUnidade().getUniCod() != null) {
            unidadeRepository.findById(solicitacao.getUnidade().getUniCod())
                    .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + solicitacao.getUnidade().getUniCod()));
        }

        if (solicitacao.getSolicitante() == null || solicitacao.getSolicitante().getPesCod() == null) {
            throw new IllegalArgumentException("Solicitante deve ser informado.");
        }
        Pessoa solicitante = pessoaRepository.findById(solicitacao.getSolicitante().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Solicitante não encontrado com o ID: " + solicitacao.getSolicitante().getPesCod()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (!userDetails.getPessoa().getPesCod().equals(solicitante.getPesCod())) {
            throw new AuthorizationDeniedException("Um usuário não pode criar uma solicitação em nome de outro.");
        }

        if (solicitacao.getTipoSolicitacao() == null || solicitacao.getTipoSolicitacao().getTsmCod() == null) {
            throw new IllegalArgumentException("Tipo de solicitação deve ser informado.");
        }
        tipoSolicitacaoManutencaoRepository.findById(solicitacao.getTipoSolicitacao().getTsmCod())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de solicitação não encontrado com o ID: " + solicitacao.getTipoSolicitacao().getTsmCod()));

        solicitacao.setStatus(SolicitacaoManutencaoStatus.ABERTA);
        solicitacao.setDtAbertura(LocalDateTime.now());
        solicitacao.setDtAtualizacao(LocalDateTime.now());
        
        return solicitacaoManutencaoRepository.save(solicitacao);
    }

    public SolicitacaoManutencao atualizarSolicitacaoManutencao(Integer id, SolicitacaoManutencao solicitacaoAtualizada) {
        SolicitacaoManutencao solicitacaoExistente = solicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de manutenção não encontrada com o ID: " + id));

        checkAdminOrSindicoPermission(solicitacaoExistente.getCondominio().getConCod());

        if (solicitacaoAtualizada.getStatus() != null) {
            solicitacaoExistente.setStatus(solicitacaoAtualizada.getStatus());
        }
        if (solicitacaoAtualizada.getResponsavel() != null) {
            pessoaRepository.findById(solicitacaoAtualizada.getResponsavel().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa responsável não encontrada com o ID: " + solicitacaoAtualizada.getResponsavel().getPesCod()));
            solicitacaoExistente.setResponsavel(solicitacaoAtualizada.getResponsavel());
        }
        if (solicitacaoAtualizada.getDtConclusao() != null) {
            solicitacaoExistente.setDtConclusao(solicitacaoAtualizada.getDtConclusao());
        }

        solicitacaoExistente.setDtAtualizacao(LocalDateTime.now());
        return solicitacaoManutencaoRepository.save(solicitacaoExistente);
    }

    public void deletarSolicitacaoManutencao(Integer id) {
        SolicitacaoManutencao solicitacao = solicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de manutenção não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(solicitacao.getCondominio().getConCod());
        
        if (solicitacao.getStatus() == SolicitacaoManutencaoStatus.EM_EXECUCAO || solicitacao.getStatus() == SolicitacaoManutencaoStatus.CONCLUIDA) {
            throw new IllegalArgumentException("Não é possível excluir uma solicitação que está em execução ou já foi concluída.");
        }

        solicitacaoManutencaoRepository.delete(solicitacao);
    }

    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);
        if (!hasPermission) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar solicitações de manutenção neste condomínio.");
        }
    }

    private void checkPermissionToView(SolicitacaoManutencao solicitacao) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
            hasAuthority(authentication, "ROLE_SINDICO_" + solicitacao.getCondominio().getConCod()) ||
            hasAuthority(authentication, "ROLE_ADMIN_" + solicitacao.getCondominio().getConCod())) {
            return;
        }
        
        if (solicitacao.getSolicitante().getPesCod().equals(userDetails.getPessoa().getPesCod())) {
            return;
        }

        throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para visualizar esta solicitação.");
    }
    
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}