package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays; 

@Service
public class ReservaAreaComumService {

    private final ReservaAreaComumRepository reservaAreaComumRepository;
    private final AreaComumRepository areaComumRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioRepository condominioRepository; 

    public ReservaAreaComumService(ReservaAreaComumRepository reservaAreaComumRepository,
                                   AreaComumRepository areaComumRepository,
                                   UnidadeRepository unidadeRepository,
                                   PessoaRepository pessoaRepository,
                                   CondominioRepository condominioRepository) { 
        this.reservaAreaComumRepository = reservaAreaComumRepository;
        this.areaComumRepository = areaComumRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaRepository = pessoaRepository;
        this.condominioRepository = condominioRepository; 
    }

    public List<ReservaAreaComum> listarTodasReservasAreaComum() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Pessoa pessoaLogada = userDetails.getPessoa();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return reservaAreaComumRepository.findAll();
        }

        Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (!condoIdsComAcessoAdmin.isEmpty()) {
            
            List<Condominio> condominios = condominioRepository.findAllById(condoIdsComAcessoAdmin);
            return reservaAreaComumRepository.findByAreaComum_CondominioIn(condominios);
           
        }
        
        return reservaAreaComumRepository.findBySolicitante(pessoaLogada);
    }
    
    public Optional<ReservaAreaComum> buscarReservaAreaComumPorId(Integer id) {
        Optional<ReservaAreaComum> reservaOpt = reservaAreaComumRepository.findById(id);
        reservaOpt.ifPresent(this::checkPermissionToViewOrModify);
        return reservaOpt;
    }

    public ReservaAreaComum cadastrarReservaAreaComum(ReservaAreaComum reserva) {
        AreaComum areaComum = areaComumRepository.findById(reserva.getAreaComum().getArcCod())
                .orElseThrow(() -> new IllegalArgumentException("Área Comum não encontrada"));
        unidadeRepository.findById(reserva.getUnidade().getUniCod())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada"));
        Pessoa solicitante = pessoaRepository.findById(reserva.getSolicitante().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Solicitante não encontrado"));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (!userDetails.getPessoa().getPesCod().equals(solicitante.getPesCod())) {
            throw new AccessDeniedException("Um usuário não pode criar uma reserva em nome de outro.");
        }

        if (reserva.getDataHoraInicio().isAfter(reserva.getDataHoraFim())) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim.");
        }
        if (!areaComum.getArcPermiteReserva()) {
            throw new IllegalArgumentException("Esta Área Comum não permite reservas.");
        }
        
        List<ReservaAreaComum> conflitos = reservaAreaComumRepository
                .findByAreaComumAndStatusNotInAndDataHoraFimAfterAndDataHoraInicioBefore(
                        areaComum, List.of(ReservaAreaComumStatus.CANCELADA), reserva.getDataHoraInicio(), reserva.getDataHoraFim());
        
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("Já existe uma reserva para esta área comum neste período.");
        }

        reserva.setStatus(ReservaAreaComumStatus.SOLICITADA);
        reserva.setDtSolicitacao(LocalDateTime.now());
        return reservaAreaComumRepository.save(reserva);
    }

    public ReservaAreaComum atualizarReservaAreaComum(Integer id, ReservaAreaComum reservaAtualizada) {
        ReservaAreaComum reservaExistente = reservaAreaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada com o ID: " + id));
        
        checkPermissionToViewOrModify(reservaExistente);
        
        if (reservaAtualizada.getStatus() != null) {
            reservaExistente.setStatus(reservaAtualizada.getStatus());
        }
        if (reservaAtualizada.getObservacoes() != null) {
            reservaExistente.setObservacoes(reservaAtualizada.getObservacoes());
        }
        
        reservaExistente.setDtAtualizacao(LocalDateTime.now());
        return reservaAreaComumRepository.save(reservaExistente);
    }

    public void deletarReservaAreaComum(Integer id) {
        ReservaAreaComum reserva = reservaAreaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada com o ID: " + id));
        
        checkPermissionToViewOrModify(reserva);
        
        reservaAreaComumRepository.delete(reserva);
    }
    
    private void checkPermissionToViewOrModify(ReservaAreaComum reserva) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Integer pessoaLogadaId = userDetails.getPessoa().getPesCod();
        Integer condominioId = reserva.getAreaComum().getCondominio().getConCod();

        if (reserva.getSolicitante().getPesCod().equals(pessoaLogadaId)) {
            return;
        }
        
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
            hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
            hasAuthority(authentication, "ROLE_ADMIN_" + condominioId)) {
            return;
        }
        
        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar ou modificar esta reserva.");
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