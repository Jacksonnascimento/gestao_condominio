package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Visitante;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class VisitanteSpecification {

    public static Specification<Visitante> filtrar(
            Integer condominioId,
            String nome,
            Integer unidadeId) {

        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("unidade", JoinType.LEFT);
                root.fetch("condominio", JoinType.INNER);
                root.fetch("pessoaRegistro", JoinType.LEFT);
                root.fetch("moradorAutorizou", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioId));
            }

            if (StringUtils.hasText(nome)) {
                predicates.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }

            if (unidadeId != null) {
                predicates.add(cb.equal(root.get("unidade").get("uniCod"), unidadeId));
            }

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.orderBy(cb.desc(root.get("dataEntrada")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}