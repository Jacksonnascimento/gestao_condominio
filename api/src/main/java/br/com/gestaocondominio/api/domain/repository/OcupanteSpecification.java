package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OcupanteSpecification {

    public static Specification<Ocupante> comFiltros(Integer condominioId, String busca, OcupanteVinculo vinculo, Integer unidadeId) {
        
        return (root, query, criteriaBuilder) -> {
            
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("pessoa", JoinType.LEFT);
                root.fetch("unidade", JoinType.LEFT).fetch("condominio", JoinType.LEFT);
                query.distinct(true);
            }
            
            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                predicates.add(criteriaBuilder.equal(root.get("unidade").get("condominio").get("conCod"), condominioId));
            }
            
            if (unidadeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("unidade").get("uniCod"), unidadeId));
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