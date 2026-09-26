package br.com.gestaocondominio.api.controller.v1.dto;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Item de uma lista de escolha (tipo, situação, papel...): o {@code valor} é o que volta nos pedidos, e a
 * {@code descricao} é o texto que a tela mostra.
 */
public record Opcao(String valor, String descricao) {

    public static <E extends Enum<E>> List<Opcao> de(Class<E> enumeracao, Function<E, String> descricao) {
        return Arrays.stream(enumeracao.getEnumConstants())
                .map(item -> new Opcao(item.name(), descricao.apply(item)))
                .toList();
    }
}
