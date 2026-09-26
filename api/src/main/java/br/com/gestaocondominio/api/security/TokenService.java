package br.com.gestaocondominio.api.security;

import br.com.gestaocondominio.api.cliente.ClienteAtual;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Tokens de acesso à API, usados pelo sistema web e pelo aplicativo.
 *
 * <p>São dois: o de acesso, curto, que vai em toda chamada ({@code Authorization: Bearer ...}), e o de renovação,
 * longo, que só serve para pedir um novo par em {@code /api/v1/auth/renovar}. Assim o aplicativo mantém a pessoa
 * logada por semanas sem que um token de acesso vazado valha por tanto tempo.</p>
 *
 * <p>Os dois carregam o cliente em que foram emitidos (os códigos de pessoa se repetem de um banco para outro) e uma
 * marca da senha atual: trocar ou redefinir a senha derruba todos os tokens já emitidos para a pessoa.</p>
 */
@Service
public class TokenService {

    static final String EMISSOR = "condigtal-api";
    static final String CLAIM_TIPO = "tipo";
    static final String CLAIM_CLIENTE = "cliente";
    static final String CLAIM_SENHA = "senha";
    static final String TIPO_ACESSO = "acesso";
    static final String TIPO_RENOVACAO = "renovacao";

    private final Algorithm algoritmo;
    private final Duration validadeAcesso;
    private final Duration validadeRenovacao;

    public TokenService(@Value("${condigtal.token.segredo}") String segredo,
                        @Value("${condigtal.token.validade-minutos:120}") long validadeMinutos,
                        @Value("${condigtal.token.renovacao-validade-dias:30}") long renovacaoDias) {
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException("Defina TOKEN_SEGREDO no .env com pelo menos 32 caracteres. Ele assina "
                    + "os logins da API; quem o conhece consegue entrar como qualquer pessoa.");
        }
        this.algoritmo = Algorithm.HMAC256(segredo);
        this.validadeAcesso = Duration.ofMinutes(validadeMinutos);
        this.validadeRenovacao = Duration.ofDays(renovacaoDias);
    }

    public TokensEmitidos emitir(Pessoa pessoa) {
        Instant agora = Instant.now();
        Instant expiraAcesso = agora.plus(validadeAcesso);
        return new TokensEmitidos(
                gerar(pessoa, TIPO_ACESSO, agora, expiraAcesso),
                gerar(pessoa, TIPO_RENOVACAO, agora, agora.plus(validadeRenovacao)),
                expiraAcesso);
    }

    private String gerar(Pessoa pessoa, String tipo, Instant emitidoEm, Instant expiraEm) {
        return JWT.create()
                .withIssuer(EMISSOR)
                .withSubject(String.valueOf(pessoa.getPesCod()))
                .withClaim(CLAIM_TIPO, tipo)
                .withClaim(CLAIM_CLIENTE, ClienteAtual.identificador())
                .withClaim(CLAIM_SENHA, marcaDaSenha(pessoa))
                .withIssuedAt(emitidoEm)
                .withExpiresAt(expiraEm)
                .sign(algoritmo);
    }

    /** Token de acesso válido para o cliente atual, ou {@code null}. */
    public DecodedJWT validarAcesso(String token) {
        return validar(token, TIPO_ACESSO);
    }

    /** Token de renovação válido para o cliente atual, ou {@code null}. */
    public DecodedJWT validarRenovacao(String token) {
        return validar(token, TIPO_RENOVACAO);
    }

    private DecodedJWT validar(String token, String tipo) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            DecodedJWT decodificado = JWT.require(algoritmo)
                    .withIssuer(EMISSOR)
                    .withClaim(CLAIM_TIPO, tipo)
                    .build()
                    .verify(token);
            String cliente = ClienteAtual.identificador();
            String doToken = decodificado.getClaim(CLAIM_CLIENTE).asString();
            boolean mesmoCliente = cliente == null ? doToken == null : cliente.equals(doToken);
            return mesmoCliente ? decodificado : null;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /** Se a senha da pessoa ainda é a mesma de quando o token foi emitido. */
    public boolean senhaConfere(DecodedJWT token, Pessoa pessoa) {
        return marcaDaSenha(pessoa).equals(token.getClaim(CLAIM_SENHA).asString());
    }

    /** Trecho do SHA-256 do hash da senha: muda quando a senha muda, e não revela nada sobre ela. */
    private static String marcaDaSenha(Pessoa pessoa) {
        String hash = pessoa.getPesSenhaLogin() == null ? "" : pessoa.getPesSenhaLogin();
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record TokensEmitidos(String token, String tokenRenovacao, Instant expiraEm) {
    }
}
