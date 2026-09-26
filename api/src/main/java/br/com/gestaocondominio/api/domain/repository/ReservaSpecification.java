package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Filtros da listagem de reservas da API v1, os mesmos da tela de reservas. */
public final class ReservaSpecification {

    private ReservaSpecification() {
    }

    /**
     * Reservas que a pessoa pode ver: as que ela mesma solicitou e as dos condomínios que ela gerencia. Quem não
     * gerencia nenhum condomínio passa {@code condominiosGeridos} vazio e vê só as próprias.
     */
    public static Specification<Reserva> visiveisPara(Integer pesCod, Collection<Integer> condominiosGeridos) {
        return (root, query, cb) -> {
            Predicate propria = cb.equal(root.get("morador").get("pesCod"), pesCod);
            if (condominiosGeridos.isEmpty()) {
                return propria;
            }
            return cb.or(propria, root.get("areaComum").get("condominio").get("conCod").in(condominiosGeridos));
        };
    }

    public static Specification<Reserva> filtrar(Integer condominioId, ReservaStatus status, String busca,
                                                 Integer areaId, LocalDate dataInicio, LocalDate dataFim) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (condominioId != null) {
                predicados.add(cb.equal(root.get("areaComum").get("condominio").get("conCod"), condominioId));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (areaId != null) {
                predicados.add(cb.equal(root.get("areaComum").get("areCod"), areaId));
            }
            // Como na tela: nome da área, nome de quem solicitou ou número da unidade
            if (StringUtils.hasText(busca)) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("areaComum").get("nome")), termo),
                        cb.like(cb.lower(root.get("morador").get("pesNome")), termo),
                        cb.like(cb.lower(root.get("unidade").get("uniNumero")), termo)));
            }
            if (dataInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("data"), dataInicio));
            }
            if (dataFim != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("data"), dataFim));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
