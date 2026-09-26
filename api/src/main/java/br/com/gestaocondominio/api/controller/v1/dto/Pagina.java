package br.com.gestaocondominio.api.controller.v1.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de uma listagem, no formato que o sistema web e o aplicativo leem. A primeira página é a 0.
 * Pedido: {@code ?pagina=0&tamanho=20}.
 */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {

    public static <T> Pagina<T> de(Page<T> page) {
        return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> conversor) {
        return de(page.map(conversor));
    }
}
