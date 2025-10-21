package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ComunicadoSpecification {

    private static final Set<PublicoDestino> TODOS_OS_PUBLICOS = Set.of(PublicoDestino.values());

    public static Specification<Comunicado> filtrar(
            Pessoa pessoa, // Usado para saber se é Global Admin
            Integer conCodAtivo, // Condomínio a filtrar (null se Global Admin)
            Set<PublicoDestino> publicosPermitidosParaVisualizar, // Públicos que o usuário PODE ver
            boolean isUsuarioAdminCondo, // Se é admin/síndico do conCodAtivo
            String tituloFiltroTela,
            String mensagemFiltroTela,
            String publicoDestinoFiltroTela,
            Boolean isUrgenteFiltroTela) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro de Condomínio
            if (Boolean.FALSE.equals(pessoa.getPesIsGlobalAdmin())) {
                if (conCodAtivo != null) {
                    // Junta com a tabela de condomínios e filtra pelo ID ativo
                    predicates.add(cb.equal(root.join("condominios").get("conCod"), conCodAtivo));
                } else {
                    // Usuário não-global sem condomínio ativo não deve ver nada
                    return cb.disjunction(); // Condição sempre falsa
                }
            }
            // Se for Global Admin, conCodAtivo será null e NÃO adiciona filtro de condomínio

            // 2. Filtro de Visibilidade por Público Destino (Regra de Negócio)
            // Não aplicamos este filtro se o usuário for Admin Global ou Admin/Síndico do condomínio
            if (!Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin()) && !isUsuarioAdminCondo) {
                 if (publicosPermitidosParaVisualizar != null && !publicosPermitidosParaVisualizar.isEmpty() && !publicosPermitidosParaVisualizar.equals(TODOS_OS_PUBLICOS)) {
                    // Adiciona a cláusula WHERE publicoDestino IN ('TODOS', 'PROPRIETARIOS', ...)
                    predicates.add(root.get("publicoDestino").in(publicosPermitidosParaVisualizar));
                } else if (publicosPermitidosParaVisualizar == null || publicosPermitidosParaVisualizar.isEmpty()) {
                     // Se por algum motivo não calculou públicos permitidos, não mostra nada
                     return cb.disjunction();
                 }
                 // Se publicosPermitidosParaVisualizar contém todos os públicos (caso Admin/Sindico), não adiciona o '.in()'
            }


            // 3. Filtros da Tela (Opcionais)
            if (tituloFiltroTela != null && !tituloFiltroTela.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + tituloFiltroTela.toLowerCase() + "%"));
            }

            if (mensagemFiltroTela != null && !mensagemFiltroTela.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("mensagem")), "%" + mensagemFiltroTela.toLowerCase() + "%"));
            }

            if (publicoDestinoFiltroTela != null && !publicoDestinoFiltroTela.isBlank()) {
                try {
                    // Filtra pelo público específico selecionado na tela
                    predicates.add(cb.equal(root.get("publicoDestino"), PublicoDestino.valueOf(publicoDestinoFiltroTela)));
                } catch (IllegalArgumentException e) {
                    // Valor inválido no filtro da tela, ignora ou loga o erro
                }
            }

            if (isUrgenteFiltroTela != null) {
                predicates.add(cb.equal(root.get("isUrgente"), isUrgenteFiltroTela));
            }

            // Ordenação padrão
            query.orderBy(cb.desc(root.get("dataCadastro")));

            // Combina todos os predicados com AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}