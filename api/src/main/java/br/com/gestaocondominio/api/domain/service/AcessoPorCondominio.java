package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Papéis de quem está logado em cada condomínio, lidos das authorities do login ({@code ROLE_GLOBAL_ADMIN} e
 * {@code ROLE_<PAPEL>_<conCod>}). Só os vínculos ativos viram authority, então um vínculo inativado deixa de valer no
 * próximo login ou renovação de token.
 */
final class AcessoPorCondominio {

    /** Gestão do condomínio: vê as unidades e gerencia os ocupantes. */
    static final UserRole[] GESTAO = {UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM};
    /** Quem cadastra, edita e inativa unidades. */
    static final UserRole[] SINDICO_OU_ADMINISTRADORA = {UserRole.SINDICO, UserRole.ADMIN};

    private AcessoPorCondominio() {
    }

    static boolean administradorGeral() {
        return authorities().contains("ROLE_GLOBAL_ADMIN");
    }

    /** O administrador geral tem todos os papéis em todos os condomínios. */
    static boolean temPapelNoCondominio(Integer conCod, UserRole... papeis) {
        return conCod != null && (administradorGeral() || condominiosComPapel(papeis).contains(conCod));
    }

    /** Se a pessoa tem algum dos papéis em pelo menos um condomínio (ou é administrador geral). */
    static boolean temPapelEmAlgumCondominio(UserRole... papeis) {
        return administradorGeral() || !condominiosComPapel(papeis).isEmpty();
    }

    /** Condomínios em que a pessoa tem algum dos papéis informados; sem papéis, os de qualquer vínculo ativo. */
    static Set<Integer> condominiosComPapel(UserRole... papeis) {
        UserRole[] procurados = papeis.length == 0 ? UserRole.values() : papeis;
        return authorities().stream()
                .map(authority -> condominioDaAuthority(authority, procurados))
                .filter(conCod -> conCod != null)
                .collect(Collectors.toSet());
    }

    private static Integer condominioDaAuthority(String authority, UserRole[] papeis) {
        for (UserRole papel : papeis) {
            String prefixo = "ROLE_" + papel.name() + "_";
            if (authority.startsWith(prefixo)) {
                try {
                    return Integer.valueOf(authority.substring(prefixo.length()));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Set<String> authorities() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null) {
            return Set.of();
        }
        return autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

}
