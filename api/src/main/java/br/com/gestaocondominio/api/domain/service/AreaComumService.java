package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaAreaComumRepository;
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

   
    public List<AreaComum> listarTodasAreasComuns() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return areaComumRepository.findAll();
        }

        Set<Integer> condoIds = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_");
        
        if (condoIds.isEmpty()) {
            return List.of();
        }

        List<Condominio> condominiosPermitidos = condominioRepository.findAllById(condoIds);
        return areaComumRepository.findByCondominioIn(condominiosPermitidos);
    }

  
    public Optional<AreaComum> buscarAreaComumPorId(Integer id) {
        Optional<AreaComum> areaComumOpt = areaComumRepository.findById(id);
        
        areaComumOpt.ifPresent(areaComum -> {
            Integer condominioId = areaComum.getCondominio().getConCod();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                    hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                    hasAuthority(authentication, "ROLE_ADMIN_" + condominioId) ||
                                    hasAuthority(authentication, "ROLE_MORADOR_" + condominioId);
            
            if (!hasPermission) {
                throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para visualizar recursos deste condomínio.");
            }
        });

        return areaComumOpt;
    }

    public AreaComum atualizarAreaComum(Integer id, AreaComum areaComumAtualizada) {
        AreaComum areaComumExistente = areaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área comum não encontrada com o ID: " + id));

        checkAdminOrSindicoPermission(areaComumExistente.getCondominio().getConCod());

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

        checkAdminOrSindicoPermission(areaComum.getCondominio().getConCod());
        
        if (!reservaAreaComumRepository.findByAreaComum(areaComum).isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir a área comum pois existem reservas associadas a ela.");
        }

        areaComumRepository.deleteById(id);
    }

   
    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);

        if (!hasPermission) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar áreas comuns neste condomínio.");
        }
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