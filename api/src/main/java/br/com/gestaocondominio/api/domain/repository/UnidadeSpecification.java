package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UnidadeSpecification {

    /** Filtros da tela de unidades. A busca procura no número e no bloco. */
    public static Specification<Unidade> comFiltros(Integer condominioId, String busca, UnidadeStatusOcupacao status,
                                                    boolean incluirInativas) {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("condominio", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (condominioId != null) {
                predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("uniStatusOcupacao"), status));
            }
            if (!incluirInativas) {
                predicates.add(cb.isTrue(root.get("uniAtiva")));
            }
            if (StringUtils.hasText(busca)) {
                String padrao = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("uniNumero")), padrao),
                        cb.like(cb.lower(root.get("bloco")), padrao)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Só as unidades dos condomínios informados ou as unidades informadas (as que a pessoa ocupa). */
    public static Specification<Unidade> visiveis(Collection<Integer> condominios, Collection<Integer> unidades) {
        return (root, query, cb) -> {
            List<Predicate> alternativas = new ArrayList<>();
            if (!condominios.isEmpty()) {
                alternativas.add(root.get("condominio").get("conCod").in(condominios));
            }
            if (!unidades.isEmpty()) {
                alternativas.add(root.get("uniCod").in(unidades));
            }
            return alternativas.isEmpty() ? cb.disjunction() : cb.or(alternativas.toArray(new Predicate[0]));
        };
    }
}
