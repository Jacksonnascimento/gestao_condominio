package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OcupanteSpecification {

    public static Specification<Ocupante> comFiltros(Integer condominioId, String busca, OcupanteVinculo vinculo) {
        
        return (root, query, criteriaBuilder) -> {
            
            
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                 root.fetch("pessoa", JoinType.LEFT);
                 root.fetch("unidade", JoinType.LEFT).fetch("condominio", JoinType.LEFT);
                 query.distinct(true);
            }

            
            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                Join<Ocupante, Unidade> unidadeJoin = root.join("unidade");
                Join<Unidade, Condominio> condominioJoin = unidadeJoin.join("condominio");
                predicates.add(criteriaBuilder.equal(condominioJoin.get("conCod"), condominioId));
            }

            if (StringUtils.hasText(busca)) {
                Join<Ocupante, Pessoa> pessoaJoin = root.join("pessoa");
                String buscaPattern = "%" + busca.toLowerCase() + "%";
                
                Predicate buscaPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(pessoaJoin.get("pesNome")), buscaPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(pessoaJoin.get("pesEmail")), buscaPattern)
                );
                predicates.add(buscaPredicate);
            }

            if (vinculo != null) {
                predicates.add(criteriaBuilder.equal(root.get("ocuVinculo"), vinculo));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}