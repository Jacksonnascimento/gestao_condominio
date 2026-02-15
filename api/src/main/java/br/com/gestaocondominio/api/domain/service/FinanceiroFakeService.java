package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.BoletoDTO;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FinanceiroFakeService {

    private static final String SESSION_KEY_BOLETOS = "BOLETOS_SESSION_FAKE";

    public List<BoletoDTO> gerarBoletosAbertos(List<Unidade> unidades, HttpSession session) {
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

        List<BoletoDTO> boletosSession = recuperarBoletosSessao(session);
        for (BoletoDTO manual : boletosSession) {
             boolean pertenceUnidadeListada = unidades.stream()
                     .anyMatch(u -> formatarNomeUnidade(u).equals(manual.getUnidadeNome()));
             
             if (pertenceUnidadeListada) {
                 boletos.add(manual);
             }
        }

        return boletos;
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

    public void salvarBoletoManual(HttpSession session, Unidade unidade, String nomeTaxa, BigDecimal valor, LocalDate dataVencimento) {
        List<BoletoDTO> boletos = recuperarBoletosSessao(session);

        BoletoDTO novo = new BoletoDTO();
        novo.setId(UUID.randomUUID().toString());
        novo.setUnidadeNome(formatarNomeUnidade(unidade));
        novo.setNomeTaxa(nomeTaxa);
        novo.setValor(valor);
        novo.setDataVencimento(dataVencimento);
        novo.setStatus("ABERTO");
        novo.setLinhaDigitavel(gerarLinhaDigitavelFake());
        novo.setCodigoPix(UUID.randomUUID().toString());

        boletos.add(novo);
        session.setAttribute(SESSION_KEY_BOLETOS, boletos);
    }

    @SuppressWarnings("unchecked")
    private List<BoletoDTO> recuperarBoletosSessao(HttpSession session) {
        List<BoletoDTO> boletos = (List<BoletoDTO>) session.getAttribute(SESSION_KEY_BOLETOS);
        if (boletos == null) {
            boletos = new ArrayList<>();
        }
        return boletos;
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