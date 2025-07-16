package br.com.gestaocondominio.api.scheduler;

import br.com.gestaocondominio.api.controller.dto.GerarCobrancaLoteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.TipoCobrancaRepository;
import br.com.gestaocondominio.api.domain.service.FinanceiroCobrancaService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CobrancaAgendadaScheduler {

    private final CondominioRepository condominioRepository;
    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final FinanceiroCobrancaService financeiroCobrancaService;

    public CobrancaAgendadaScheduler(CondominioRepository condominioRepository,
                                     TipoCobrancaRepository tipoCobrancaRepository,
                                     FinanceiroCobrancaService financeiroCobrancaService) {
        this.condominioRepository = condominioRepository;
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.financeiroCobrancaService = financeiroCobrancaService;
    }

    // --- MÉTODOS DE INICIALIZAÇÃO ---

    @PostConstruct
    public void executarRotinasAoIniciar() {
        System.out.println("Executando rotinas agendadas na inicialização do sistema...");
        executarGeracaoAgendada();
        verificarEAtualizarStatusCobrancasVencidas();
        System.out.println("Rotinas de inicialização concluídas.");
    }

    // --- MÉTODOS AGENDADOS ---

    /**
     * Executa a geração de cobranças em lote para condomínios configurados.
     * Esta rotina é executada diariamente às 2h da manhã e verifica se é o dia correto para gerar as cobranças do mês.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executarGeracaoAgendada() {
        int diaDoMes = LocalDate.now().getDayOfMonth();
        List<Condominio> condominiosParaGerar = condominioRepository.findByConGeracaoAutoAtivaIsTrueAndConDiaGeracaoCobranca(diaDoMes);

        if (condominiosParaGerar.isEmpty()) {
            System.out.println("Nenhum condomínio para geração de cobrança agendada hoje.");
            return;
        }

        for (Condominio condominio : condominiosParaGerar) {
            List<TipoCobranca> tiposDeCobranca = tipoCobrancaRepository.findByCondominioConCodAndTicGeracaoAutomaticaIsTrueAndTicAtivaIsTrue(condominio.getConCod());
            
            for (TipoCobranca tipo : tiposDeCobranca) {
                GerarCobrancaLoteRequestDTO request = new GerarCobrancaLoteRequestDTO();
                request.setCondominioId(condominio.getConCod());
                request.setTipoCobrancaId(tipo.getTicCod());
                
                LocalDate dataVencimento = LocalDate.now().withDayOfMonth(condominio.getConDtVencimentoTaxa());
                request.setDataVencimento(dataVencimento);

                try {
                    System.out.println("Iniciando geração agendada para o condomínio: " + condominio.getConNome());
                    financeiroCobrancaService.gerarCobrancasEmLote(request);
                    System.out.println("Finalizada geração agendada para o condomínio: " + condominio.getConNome());
                } catch (Exception e) {
                    System.err.println("Falha ao gerar cobranças em lote para o condomínio " + condominio.getConCod() + " e tipo " + tipo.getTicCod() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Verifica e atualiza o status de cobranças 'A_VENCER' para 'VENCIDA'.
     * Esta rotina é executada diariamente à meia-noite (00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void verificarEAtualizarStatusCobrancasVencidas() {
        try {
            System.out.println("Iniciando rotina para verificar e atualizar status de cobranças vencidas...");
            financeiroCobrancaService.verificarEAtualizarCobrancasVencidas();
            System.out.println("Rotina de verificação de cobranças vencidas finalizada com sucesso.");
        } catch (Exception e) {
            System.err.println("Falha ao executar a rotina de verificação de cobranças vencidas: " + e.getMessage());
        }
    }
}