package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Encomenda;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class EncomendaSpecification {

    public static Specification<Encomenda> filtrar(
            Integer condominioId,
            String busca,
            Integer unidadeId,
            EncomendaStatus status,
            List<Unidade> unidadesPermitidas) {

        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("unidade", JoinType.INNER).fetch("condominio", JoinType.INNER);
                root.fetch("pessoaRegistro", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioId));
            }

            if (unidadeId != null) {
                predicates.add(cb.equal(root.get("unidade").get("uniCod"), unidadeId));
            }

            if (StringUtils.hasText(busca)) {
                String buscaLower = "%" + busca.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("destinatario")), buscaLower),
                        cb.like(cb.lower(root.get("descricao")), buscaLower)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (unidadesPermitidas != null && !unidadesPermitidas.isEmpty()) {
                predicates.add(root.get("unidade").in(unidadesPermitidas));
            }

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                 query.orderBy(cb.desc(root.get("dataRecebimento")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}