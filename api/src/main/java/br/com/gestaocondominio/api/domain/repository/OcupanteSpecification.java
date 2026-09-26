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
import java.util.Collection;
import java.util.List;

public class OcupanteSpecification {

    /**
     * Filtros da listagem paginada, por condomínio, unidade, vínculo e nome ou e-mail: sem {@code distinct} (as junções são
     * todas para um, então não repetem linhas) e em ordem alfabética do nome do ocupante.
     */
    public static Specification<Ocupante> paraListagem(Integer condominioId, String busca, OcupanteVinculo vinculo,
                                                       Integer unidadeId) {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                @SuppressWarnings("unchecked")
                Join<Ocupante, Pessoa> pessoa = (Join<Ocupante, Pessoa>) root.<Ocupante, Pessoa>fetch("pessoa", JoinType.INNER);
                root.fetch("unidade", JoinType.INNER).fetch("condominio", JoinType.INNER);
                query.orderBy(cb.asc(cb.lower(pessoa.get("pesNome"))), cb.asc(root.get("ocuCod")));
            }

            List<Predicate> predicates = new ArrayList<>();
            if (condominioId != null) {
                predicates.add(cb.equal(root.get("unidade").get("condominio").get("conCod"), condominioId));
            }
            if (unidadeId != null) {
                predicates.add(cb.equal(root.get("unidade").get("uniCod"), unidadeId));
            }
            if (vinculo != null) {
                predicates.add(cb.equal(root.get("ocuVinculo"), vinculo));
            }
            if (StringUtils.hasText(busca)) {
                String padrao = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("pessoa").get("pesNome")), padrao),
                        cb.like(cb.lower(root.get("pessoa").get("pesEmail")), padrao)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Só os ocupantes das unidades dos condomínios informados ou das unidades informadas. */
    public static Specification<Ocupante> visiveis(Collection<Integer> condominios, Collection<Integer> unidades) {
        return (root, query, cb) -> {
            List<Predicate> alternativas = new ArrayList<>();
            if (!condominios.isEmpty()) {
                alternativas.add(root.get("unidade").get("condominio").get("conCod").in(condominios));
            }
            if (!unidades.isEmpty()) {
                alternativas.add(root.get("unidade").get("uniCod").in(unidades));
            }
            return alternativas.isEmpty() ? cb.disjunction() : cb.or(alternativas.toArray(new Predicate[0]));
        };
    }
}