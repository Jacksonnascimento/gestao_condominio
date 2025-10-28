package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OcorrenciaSpecification {

    public static Specification<Ocorrencia> filtrar(
            Pessoa usuarioLogado,
            Integer condominioIdFiltroTela,
            String buscaUnidade,
            String buscaTitulo,
            OcorrenciaTipo tipo,
            OcorrenciaStatus status,
            LocalDate inicioApos,
            LocalDate fimAntes,
            UsuarioCondominioService usuarioCondominioService,
            boolean otimizarParaContagem
    ) {

        return (root, query, cb) -> {

            if (!otimizarParaContagem && (Long.class != query.getResultType() && long.class != query.getResultType())) {
                root.fetch("unidade", JoinType.INNER).fetch("condominio", JoinType.INNER);
                root.fetch("pessoaRegistro", JoinType.LEFT);
                 query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            Integer condominioIdParaFiltrar = condominioIdFiltroTela;

            if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
                if (condominioIdParaFiltrar != null) {
                    predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioIdParaFiltrar));
                }
            } else {
                 List<UsuarioCondominio> associacoes = usuarioCondominioService.findByPessoa(usuarioLogado);
                 Set<Integer> condominiosGerenciadosIds = associacoes.stream()
                         .filter(uc -> uc.getUscPapel() == UserRole.SINDICO || uc.getUscPapel() == UserRole.ADMIN || uc.getUscPapel() == UserRole.FUNCIONARIO_ADM)
                         .map(UsuarioCondominio::getConCod)
                         .collect(Collectors.toSet());

                 if (!condominiosGerenciadosIds.isEmpty()) {
                     if (condominioIdParaFiltrar != null && !condominiosGerenciadosIds.contains(condominioIdParaFiltrar)) {
                         return cb.disjunction();
                     }
                     if (condominioIdParaFiltrar != null) {
                         predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioIdParaFiltrar));
                     } else {
                         predicates.add(root.get("condominio").get("conCod").in(condominiosGerenciadosIds));
                     }
                 } else {
                     Subquery<Integer> subqueryUnidadesMorador = query.subquery(Integer.class);
                     Root<Ocupante> ocupanteRoot = subqueryUnidadesMorador.from(Ocupante.class);
                     subqueryUnidadesMorador.select(ocupanteRoot.get("unidade").get("uniCod"))
                             .where(cb.equal(ocupanteRoot.get("pessoa").get("pesCod"), usuarioLogado.getPesCod()));

                     predicates.add(root.get("unidade").get("uniCod").in(subqueryUnidadesMorador));

                     if (condominioIdParaFiltrar != null) {
                          Integer conCodUsuario = associacoes.stream().findFirst().map(UsuarioCondominio::getConCod).orElse(null);
                          if (conCodUsuario == null || !condominioIdParaFiltrar.equals(conCodUsuario)) {
                              return cb.disjunction();
                          }
                         predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioIdParaFiltrar));
                     }
                 }
            }

            if (StringUtils.hasText(buscaUnidade)) {
                String buscaUnidadeLower = "%" + buscaUnidade.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("unidade").get("uniNumero")), buscaUnidadeLower),
                        cb.like(cb.lower(root.get("unidade").get("bloco")), buscaUnidadeLower)
                ));
            }

            if (StringUtils.hasText(buscaTitulo)) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + buscaTitulo.toLowerCase() + "%"));
            }

            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (inicioApos != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataRegistro"), LocalDateTime.of(inicioApos, LocalTime.MIN)));
            }

            if (fimAntes != null) {
                 predicates.add(cb.lessThanOrEqualTo(root.get("dataRegistro"), LocalDateTime.of(fimAntes, LocalTime.MAX)));
            }

             if (!otimizarParaContagem) {
                 Order orderStatus;
                 Order orderData = cb.desc(root.get("dataRegistro"));

                 if (status == null) {
                      Expression<Integer> statusOrderExpression = cb.<Integer>selectCase() // Especifica o tipo aqui
                                      .when(cb.equal(root.get("status"), OcorrenciaStatus.ABERTA), 0)
                                      .when(cb.equal(root.get("status"), OcorrenciaStatus.EM_ANALISE), 1)
                                      .when(cb.equal(root.get("status"), OcorrenciaStatus.RESOLVIDA), 2)
                                      .otherwise(3);
                      orderStatus = cb.asc(statusOrderExpression);
                      query.orderBy(orderStatus, orderData);
                 } else {
                     query.orderBy(orderData);
                 }
             }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}