package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.ComunicadoEntrega;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.ComunicadoDestino;
import br.com.gestaocondominio.api.domain.repository.ComunicadoEntregaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.GestaoComunicacaoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GestaoComunicacaoService {

    private final GestaoComunicacaoRepository gestaoComunicacaoRepository;
    private final CondominioRepository condominioRepository;
    private final PessoaRepository pessoaRepository;
    private final ComunicadoEntregaRepository comunicadoEntregaRepository;

    public GestaoComunicacaoService(GestaoComunicacaoRepository gestaoComunicacaoRepository, CondominioRepository condominioRepository, PessoaRepository pessoaRepository, ComunicadoEntregaRepository comunicadoEntregaRepository) {
        this.gestaoComunicacaoRepository = gestaoComunicacaoRepository;
        this.condominioRepository = condominioRepository;
        this.pessoaRepository = pessoaRepository;
        this.comunicadoEntregaRepository = comunicadoEntregaRepository;
    }

    @Transactional
    public GestaoComunicacao cadastrarComunicacao(GestaoComunicacao comunicacao) {
        Pessoa remetente = pessoaRepository.findById(comunicacao.getRemetente().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Remetente não encontrado"));
        Condominio condominio = condominioRepository.findById(comunicacao.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (!userDetails.getPessoa().getPesCod().equals(remetente.getPesCod())) {
            throw new AuthorizationDeniedException("Um usuário não pode enviar um comunicado em nome de outro.");
        }

        boolean isGestor = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") || hasAuthority(authentication, "ROLE_SINDICO_" + condominio.getConCod()) || hasAuthority(authentication, "ROLE_ADMIN_" + condominio.getConCod());
        if (!isGestor && comunicacao.getComDesTodos() == ComunicadoDestino.TODOS) {
            throw new AuthorizationDeniedException("Moradores não podem enviar comunicados para todos.");
        }
        
        comunicacao.setComDtCadastro(LocalDateTime.now());
        comunicacao.setComDtAtualizacao(LocalDateTime.now());
        return gestaoComunicacaoRepository.save(comunicacao);
    }
    
    public List<GestaoComunicacao> listarTodasComunicacoes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return gestaoComunicacaoRepository.findAll();
        }

        Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (!condoIdsComAcessoAdmin.isEmpty()) {
            List<Condominio> condominios = condominioRepository.findAllById(condoIdsComAcessoAdmin);
            return condominios.stream()
                .flatMap(c -> gestaoComunicacaoRepository.findByCondominio(c).stream())
                .collect(Collectors.toList());
        }

        Set<Integer> condoIdsMorador = getCondoIdsFromRoles(authentication, "ROLE_MORADOR_");
        if (condoIdsMorador.isEmpty()) return List.of();
        
        List<Condominio> condominiosDoMorador = condominioRepository.findAllById(condoIdsMorador);

        List<GestaoComunicacao> comunicadosGerais = condominiosDoMorador.stream()
            .flatMap(c -> gestaoComunicacaoRepository.findByCondominioAndComDesTodos(c, ComunicadoDestino.TODOS).stream())
            .collect(Collectors.toList());

        List<ComunicadoEntrega> entregasDirecionadas = comunicadoEntregaRepository.findByDestinatario(userDetails.getPessoa());
        List<GestaoComunicacao> comunicadosDirecionados = entregasDirecionadas.stream()
            .map(ComunicadoEntrega::getComunicado)
            .collect(Collectors.toList());

        return Stream.concat(comunicadosGerais.stream(), comunicadosDirecionados.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public Optional<GestaoComunicacao> buscarComunicacaoPorId(Integer id) {
        Optional<GestaoComunicacao> comunicadoOpt = gestaoComunicacaoRepository.findById(id);
        comunicadoOpt.ifPresent(this::checkPermissionToView);
        return comunicadoOpt;
    }

    public GestaoComunicacao atualizarComunicacao(Integer id, GestaoComunicacao comunicacaoAtualizada) {
        GestaoComunicacao comunicacaoExistente = gestaoComunicacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comunicado não encontrado com o ID: " + id));

        checkPermissionToModify(comunicacaoExistente);

        if (comunicacaoExistente.getComDtEnvio() != null) {
            throw new IllegalStateException("Não é possível atualizar um comunicado que já foi enviado.");
        }

        if (comunicacaoAtualizada.getComAssunto() != null) {
            comunicacaoExistente.setComAssunto(comunicacaoAtualizada.getComAssunto());
        }
        if (comunicacaoAtualizada.getComMensagem() != null) {
            comunicacaoExistente.setComMensagem(comunicacaoAtualizada.getComMensagem());
        }
        if (comunicacaoAtualizada.getComDesTodos() != null) {
            comunicacaoExistente.setComDesTodos(comunicacaoAtualizada.getComDesTodos());
        }

        comunicacaoExistente.setComDtAtualizacao(LocalDateTime.now());
        return gestaoComunicacaoRepository.save(comunicacaoExistente);
    }

    @Transactional
    public void deletarComunicacao(Integer id) {
        GestaoComunicacao comunicacao = gestaoComunicacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comunicado não encontrado com o ID: " + id));
        
        checkPermissionToModify(comunicacao);

        if (comunicacao.getComDtEnvio() != null) {
            throw new IllegalStateException("Não é possível excluir um comunicado que já foi enviado.");
        }

        comunicadoEntregaRepository.deleteAll(comunicadoEntregaRepository.findByComunicado(comunicacao));
        gestaoComunicacaoRepository.delete(comunicacao);
    }
    
    private void checkPermissionToView(GestaoComunicacao comunicacao) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (comunicacao.getComDesTodos() == ComunicadoDestino.TODOS && hasRoleInCondo(authentication, comunicacao.getCondominio().getConCod())) {
            return;
        }

        boolean isRecipient = comunicadoEntregaRepository.findByComunicadoAndDestinatario(comunicacao, userDetails.getPessoa()).isPresent();
        if (isRecipient) {
            return;
        }
        
        boolean isSender = comunicacao.getRemetente().getPesCod().equals(userDetails.getPessoa().getPesCod());
        if (isSender) {
            return;
        }

        checkAdminOrSindicoPermission(comunicacao.getCondominio().getConCod());
    }

    private void checkPermissionToModify(GestaoComunicacao comunicacao) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        if (comunicacao.getRemetente().getPesCod().equals(userDetails.getPessoa().getPesCod())) {
            return; 
        }
        
        
        checkAdminOrSindicoPermission(comunicacao.getCondominio().getConCod());
    }

    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);
        if (!hasPermission) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar comunicados neste condomínio.");
        }
    }
    
    private boolean hasRoleInCondo(Authentication auth, Integer condominioId) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().endsWith("_" + condominioId));
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