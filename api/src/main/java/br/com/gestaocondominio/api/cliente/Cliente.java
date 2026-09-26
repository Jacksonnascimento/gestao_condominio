package br.com.gestaocondominio.api.cliente;

import java.util.List;

/**
 * Um cliente atendido por esta instalação (uma administradora ou um condomínio que contrata o sistema sozinho),
 * com o banco que guarda os dados dele.
 *
 * @param identificador nome curto do cliente, que também é o subdomínio pelo qual ele é acessado
 *                      ({@code residencialflores} em {@code residencialflores.condigtal.com.br})
 * @param banco         nome do banco de dados do cliente no servidor PostgreSQL da instalação
 * @param dominios      endereços adicionais que também levam a este cliente, como um domínio próprio ou o
 *                      {@code localhost} do desenvolvimento
 */
public record Cliente(String identificador, String banco, List<String> dominios) {

    public Cliente {
        dominios = List.copyOf(dominios);
    }
}
