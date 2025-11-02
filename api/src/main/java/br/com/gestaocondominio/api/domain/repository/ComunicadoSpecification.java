package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ComunicadoSpecification {

    private static final Set<PublicoDestino> TODOS_OS_PUBLICOS = Set.of(PublicoDestino.values());

    public static Specification<Comunicado> filtrar(
            Pessoa pessoa,
            Integer conCodAtivo,
            Set<PublicoDestino> publicosPermitidosParaVisualizar,
            boolean isUsuarioAdminCondo,
            String tituloFiltroTela,
            String mensagemFiltroTela,
            String publicoDestinoFiltroTela,
            Boolean isUrgenteFiltroTela) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                 root.fetch("criador", JoinType.LEFT);
                 
                 if (Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin())) {
                    root.fetch("condominios", JoinType.LEFT);
                    query.distinct(true);
                 }
            }

           
            if (Boolean.FALSE.equals(pessoa.getPesIsGlobalAdmin())) {
                if (conCodAtivo != null) {
                    
                    predicates.add(cb.equal(root.join("condominios").get("conCod"), conCodAtivo));
                } else {
                    return cb.disjunction();
                }
            }

           
            if (!Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin()) && !isUsuarioAdminCondo) {
                 if (publicosPermitidosParaVisualizar != null && !publicosPermitidosParaVisualizar.isEmpty() && !publicosPermitidosParaVisualizar.equals(TODOS_OS_PUBLICOS)) {
                    predicates.add(root.get("publicoDestino").in(publicosPermitidosParaVisualizar));
                } else if (publicosPermitidosParaVisualizar == null || publicosPermitidosParaVisualizar.isEmpty()) {
                     return cb.disjunction();
                 }
            }

         
            if (tituloFiltroTela != null && !tituloFiltroTela.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + tituloFiltroTela.toLowerCase() + "%"));
            }

            if (mensagemFiltroTela != null && !mensagemFiltroTela.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("mensagem")), "%" + mensagemFiltroTela.toLowerCase() + "%"));
            }

            if (publicoDestinoFiltroTela != null && !publicoDestinoFiltroTela.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("publicoDestino"), PublicoDestino.valueOf(publicoDestinoFiltroTela)));
                } catch (IllegalArgumentException e) {
                    
                }
            }

            if (isUrgenteFiltroTela != null) {
                predicates.add(cb.equal(root.get("isUrgente"), isUrgenteFiltroTela));
            }

            
             if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.orderBy(cb.desc(root.get("dataCadastro")));
                 
                if (Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin())) {
                    query.distinct(true);
                }
             }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}