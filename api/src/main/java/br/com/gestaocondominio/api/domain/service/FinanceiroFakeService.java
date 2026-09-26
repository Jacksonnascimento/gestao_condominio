package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.cliente.ClienteAtual;
import br.com.gestaocondominio.api.controller.dto.BoletoDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FinanceiroFakeService {

    /** Papéis que veem as cobranças de todas as unidades do condomínio e geram boleto avulso. */
    public static final UserRole[] PAPEIS_DE_GESTAO = {UserRole.SINDICO, UserRole.ADMIN};

    private static final int LIMITE_AVULSOS_POR_UNIDADE = 50;

    /**
     * Boletos avulsos gerados, por cliente e unidade. A API não tem sessão (o sistema web e o aplicativo entram por
     * token), então eles ficam na memória até a API reiniciar e aparecem para quem vê a unidade.
     */
    private final Map<String, List<BoletoDTO>> boletosAvulsosPorUnidade = new ConcurrentHashMap<>();

    private final UsuarioCondominioService usuarioCondominioService;
    private final CondominioRepository condominioRepository;
    private final UnidadeRepository unidadeRepository;
    private final OcupanteRepository ocupanteRepository;

    public FinanceiroFakeService(UsuarioCondominioService usuarioCondominioService,
                                 CondominioRepository condominioRepository,
                                 UnidadeRepository unidadeRepository,
                                 OcupanteRepository ocupanteRepository) {
        this.usuarioCondominioService = usuarioCondominioService;
        this.condominioRepository = condominioRepository;
        this.unidadeRepository = unidadeRepository;
        this.ocupanteRepository = ocupanteRepository;
    }

    public List<BoletoDTO> gerarBoletosVencidos(List<Unidade> unidades) {
        List<BoletoDTO> boletos = new ArrayList<>();
        if (unidades.isEmpty()) return boletos;

        int qtd = Math.min(unidades.size(), 3);

        for (int i = 0; i < qtd; i++) {
            Unidade unidade = unidades.get(i);
            BoletoDTO boleto = new BoletoDTO();
            boleto.setId(UUID.randomUUID().toString());
            boleto.setUnidadeNome(formatarNomeUnidade(unidade));
            boleto.setNomeTaxa("Taxa Condominial - Atraso");

            BigDecimal valorBase = new BigDecimal("700.00");
            BigDecimal multa = new BigDecimal("35.50");
            boleto.setValor(valorBase.add(multa));

            boleto.setDataVencimento(LocalDate.now().minusMonths(3));
            boleto.setStatus("VENCIDO");
            boleto.setLinhaDigitavel(gerarLinhaDigitavelFake());
            boleto.setCodigoPix(UUID.randomUUID().toString());
            boletos.add(boleto);
        }
        return boletos;
    }

    public List<BoletoDTO> gerarHistorico(List<Unidade> unidades) {
        List<BoletoDTO> boletos = new ArrayList<>();

        for (Unidade unidade : unidades) {
            BoletoDTO boleto = new BoletoDTO();
            boleto.setId(UUID.randomUUID().toString());
            boleto.setUnidadeNome(formatarNomeUnidade(unidade));
            boleto.setNomeTaxa("Taxa Condominial Mensal");
            boleto.setValor(new BigDecimal("700.00"));
            boleto.setDataVencimento(LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
            boleto.setStatus("PAGO");
            boleto.setLinhaDigitavel(gerarLinhaDigitavelFake());
            boleto.setCodigoPix(UUID.randomUUID().toString());
            boletos.add(boleto);
        }
        return boletos;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // API v1. O administrador geral, o síndico e a administração veem as cobranças de todas as unidades ativas do
    // condomínio e geram boleto avulso; os demais vinculados ao condomínio (morador, porteiro, funcionário) veem só as
    // unidades que ocupam.
    // ---------------------------------------------------------------------------------------------------------------

    /** Condomínios em que a pessoa tem algum vínculo ativo; para o administrador geral, todos. */
    public List<Condominio> condominiosDisponiveis(Pessoa usuario) {
        return usuarioCondominioService.condominiosDisponiveis(usuario, UserRole.values());
    }

    /**
     * Condomínio do painel: o pedido, se a pessoa tem vínculo com ele; sem pedido, o primeiro, em ordem de nome, dos
     * que ela alcança.
     */
    public Condominio condominioDoPainel(Pessoa usuario, Integer condominioId) {
        if (condominioId == null) {
            return condominiosDisponiveis(usuario).stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Nenhum condomínio disponível."));
        }
        if (!usuarioCondominioService.possuiPapelNoCondominio(usuario, condominioId, UserRole.values())) {
            throw new AccessDeniedException("Você não tem acesso a este condomínio.");
        }
        return condominioRepository.findById(condominioId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));
    }

    public boolean podeGerarBoleto(Pessoa usuario, Integer condominioId) {
        return usuarioCondominioService.possuiPapelNoCondominio(usuario, condominioId, PAPEIS_DE_GESTAO);
    }

    /** Unidades do condomínio cujas cobranças a pessoa vê. Quem chama já conferiu o acesso ao condomínio. */
    @Transactional(readOnly = true)
    public List<Unidade> unidadesVisiveis(Pessoa usuario, Integer condominioId) {
        if (podeGerarBoleto(usuario, condominioId)) {
            return unidadeRepository.findAtivasByCondominioConCodWithCondominio(condominioId);
        }
        return ocupanteRepository.findByPessoa(usuario).stream()
                .map(Ocupante::getUnidade)
                .filter(Objects::nonNull)
                .filter(unidade -> condominioId.equals(unidade.getCondominio().getConCod()))
                .distinct()
                .toList();
    }

    /** Cobranças do mês em aberto das unidades, mais os boletos avulsos gerados pela API para elas. */
    public List<BoletoDTO> gerarBoletosAbertos(List<Unidade> unidades) {
        List<BoletoDTO> boletos = boletosMensaisAbertos(unidades);
        for (Unidade unidade : unidades) {
            List<BoletoDTO> avulsos = boletosAvulsosPorUnidade.get(chaveDaUnidade(unidade));
            if (avulsos != null) {
                synchronized (avulsos) {
                    boletos.addAll(avulsos);
                }
            }
        }
        return boletos;
    }

    /**
     * Gera um boleto avulso para a unidade. É demonstração: nada vai para o banco de dados nem para um banco de
     * cobrança. Sem vencimento, vale o último dia do mês.
     */
    @Transactional(readOnly = true)
    public BoletoDTO gerarBoletoAvulso(Pessoa usuario, Integer unidadeId, String nomeTaxa, BigDecimal valor,
                                       LocalDate dataVencimento) {
        Unidade unidade = unidadeRepository.findByIdWithCondominio(unidadeId)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));
        if (!podeGerarBoleto(usuario, unidade.getCondominio().getConCod())) {
            throw new AccessDeniedException("Você não gera boletos neste condomínio.");
        }
        LocalDate vencimento = dataVencimento != null
                ? dataVencimento
                : LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        BoletoDTO novo = novoBoletoAvulso(unidade, nomeTaxa, valor, vencimento);
        List<BoletoDTO> avulsos = boletosAvulsosPorUnidade.computeIfAbsent(chaveDaUnidade(unidade),
                chave -> new ArrayList<>());
        synchronized (avulsos) {
            avulsos.add(novo);
            if (avulsos.size() > LIMITE_AVULSOS_POR_UNIDADE) {
                avulsos.remove(0);
            }
        }
        return novo;
    }

    /** Separa os avulsos por cliente, já que cada cliente tem o seu banco e os códigos de unidade se repetem. */
    private static String chaveDaUnidade(Unidade unidade) {
        return Objects.toString(ClienteAtual.identificador(), "") + ":" + unidade.getUniCod();
    }

    private List<BoletoDTO> boletosMensaisAbertos(List<Unidade> unidades) {
        List<BoletoDTO> boletos = new ArrayList<>();

        for (Unidade unidade : unidades) {
            BoletoDTO boleto = new BoletoDTO();
            boleto.setId(UUID.randomUUID().toString());
            boleto.setUnidadeNome(formatarNomeUnidade(unidade));
            boleto.setNomeTaxa("Taxa Condominial Mensal");
            boleto.setValor(new BigDecimal("700.00"));
            boleto.setDataVencimento(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
            boleto.setStatus("ABERTO");
            boleto.setLinhaDigitavel(gerarLinhaDigitavelFake());
            boleto.setCodigoPix(UUID.randomUUID().toString());
            boletos.add(boleto);
        }
        return boletos;
    }

    private BoletoDTO novoBoletoAvulso(Unidade unidade, String nomeTaxa, BigDecimal valor, LocalDate dataVencimento) {
        BoletoDTO novo = new BoletoDTO();
        novo.setId(UUID.randomUUID().toString());
        novo.setUnidadeNome(formatarNomeUnidade(unidade));
        novo.setNomeTaxa(nomeTaxa);
        novo.setValor(valor);
        novo.setDataVencimento(dataVencimento);
        novo.setStatus("ABERTO");
        novo.setLinhaDigitavel(gerarLinhaDigitavelFake());
        novo.setCodigoPix(UUID.randomUUID().toString());
        return novo;
    }

    private String gerarLinhaDigitavelFake() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 47; i++) {
            if (i > 0 && i % 5 == 0) sb.append(".");
            sb.append(ThreadLocalRandom.current().nextInt(0, 9));
        }
        return "34191.79001 01043.510047 91020.150008 8 " + ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L);
    }

   private String formatarNomeUnidade(Unidade unidade) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unidade ").append(unidade.getUniNumero());

        if (unidade.getBloco() != null && !unidade.getBloco().isBlank()) {
            sb.append(", Bloco ").append(unidade.getBloco());
        }

        return sb.toString();
    }
}
