package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final CondominioService condominioService;
    private final AreaComumService areaComumService;
    private final PessoaService pessoaService;
    private final OcupanteRepository ocupanteRepository;
    private final UsuarioCondominioService usuarioCondominioService;

    @GetMapping
    @Transactional(readOnly = true)
    public String listarReservas(@RequestParam(required = false) Integer conCod, 
                                 @RequestParam(required = false) ReservaStatus status,
                                 @RequestParam(required = false) String busca,
                                 @RequestParam(required = false) Integer areaFiltro,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioApos,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fimAntes,
                                 Model model) {
                                     
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);

        Integer condominioIdFiltro = conCod;
        if (condominioIdFiltro == null && condominiosDisponiveis.size() == 1) {
            condominioIdFiltro = condominiosDisponiveis.get(0).getConCod();
        }

        boolean isGerencial = Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) || 
                usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);

        List<Reserva> reservas;
        if (isGerencial) {
            reservas = (condominioIdFiltro != null) ? reservaService.listarPorCondominio(condominioIdFiltro) : List.of();
        } else {
            reservas = reservaService.listarPorMorador(usuarioLogado.getPesCod());
        }

        // Totais antes dos filtros restritivos (para manter o painel superior coeso)
        long totalReservas = reservas.size();
        long reservasPendentes = reservas.stream().filter(r -> r.getStatus() == ReservaStatus.PENDENTE_APROVACAO).count();
        long reservasAprovadas = reservas.stream().filter(r -> r.getStatus() == ReservaStatus.APROVADA).count();

        // Aplicando Filtro de Status
        if (status != null) {
            reservas = reservas.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
        }

        // Aplicando Filtro de Área Comum
        if (areaFiltro != null) {
            reservas = reservas.stream().filter(r -> r.getAreaComum().getAreCod().equals(areaFiltro)).collect(Collectors.toList());
        }

        // Aplicando Filtro de Busca Textual
        if (busca != null && !busca.trim().isEmpty()) {
            String termo = busca.toLowerCase();
            reservas = reservas.stream()
                .filter(r -> r.getAreaComum().getNome().toLowerCase().contains(termo) ||
                             r.getMorador().getPesNome().toLowerCase().contains(termo) ||
                             r.getUnidade().getUniNumero().toLowerCase().contains(termo))
                .collect(Collectors.toList());
        }

        // Aplicando Filtro de Datas
        if (inicioApos != null) {
            reservas = reservas.stream().filter(r -> !r.getData().isBefore(inicioApos)).collect(Collectors.toList());
        }
        if (fimAntes != null) {
            reservas = reservas.stream().filter(r -> !r.getData().isAfter(fimAntes)).collect(Collectors.toList());
        }

        List<Unidade> unidadesUsuario = ocupanteRepository.findByPessoa(usuarioLogado).stream()
                .map(Ocupante::getUnidade)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<AreaComum> areasAtivas;
        List<AreaComum> areasParaFiltro;

        if (!isGerencial) {
            areasAtivas = unidadesUsuario.stream()
                .map(u -> u.getCondominio().getConCod())
                .distinct()
                .flatMap(cid -> areaComumService.listarAtivasPorCondominio(cid).stream())
                .collect(Collectors.toList());
            
            areasParaFiltro = unidadesUsuario.stream()
                .map(u -> u.getCondominio().getConCod())
                .distinct()
                .flatMap(cid -> areaComumService.listarPorCondominio(cid).stream())
                .collect(Collectors.toList());
        } else {
            if (condominioIdFiltro != null) {
                areasAtivas = areaComumService.listarAtivasPorCondominio(condominioIdFiltro);
                areasParaFiltro = areaComumService.listarPorCondominio(condominioIdFiltro);
            } else {
                areasAtivas = List.of();
                areasParaFiltro = List.of();
            }
        }

        model.addAttribute("reservas", reservas);
        model.addAttribute("areasAtivas", areasAtivas);
        model.addAttribute("areasParaFiltro", areasParaFiltro);
        model.addAttribute("unidadesUsuario", unidadesUsuario);
        model.addAttribute("condominios", condominiosDisponiveis);
        model.addAttribute("conCodSelecionado", condominioIdFiltro);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("areaSelecionada", areaFiltro);
        model.addAttribute("inicioAposFiltro", inicioApos);
        model.addAttribute("fimAntesFiltro", fimAntes);
        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("currentPage", "reservas");
        model.addAttribute("isGerencial", isGerencial);
        model.addAttribute("showCondominioInfo", Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) && condominiosDisponiveis.size() > 1);

        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("totalPendentes", reservasPendentes);
        model.addAttribute("totalAprovadas", reservasAprovadas);

        return "reservas";
    }

    @PostMapping("/solicitar")
    public Object solicitarReserva(@RequestBody ReservaRequestDTO dto, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            dto.setPesCodMorador(usuarioLogado.getPesCod());
            Reserva reservaSalva = reservaService.solicitarReserva(dto);
            adicionarAtributosCard(model, reservaSalva, usuarioLogado);
            return "fragments/reserva-card :: card";
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprovar")
    public Object aprovarReserva(@PathVariable Integer id, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Reserva reservaAprovada = reservaService.aprovarReserva(id, usuarioLogado.getPesCod());
            adicionarAtributosCard(model, reservaAprovada, usuarioLogado);
            return "fragments/reserva-card :: card";
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rejeitar")
    public Object rejeitarReserva(@PathVariable Integer id, @RequestParam String motivo, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Reserva reservaRejeitada = reservaService.rejeitarReserva(id, usuarioLogado.getPesCod(), motivo);
            adicionarAtributosCard(model, reservaRejeitada, usuarioLogado);
            return "fragments/reserva-card :: card";
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancelar")
    public Object cancelarReserva(@PathVariable Integer id, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Reserva reservaCancelada = reservaService.cancelarReserva(id, usuarioLogado.getPesCod());
            adicionarAtributosCard(model, reservaCancelada, usuarioLogado);
            return "fragments/reserva-card :: card";
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private void adicionarAtributosCard(Model model, Reserva reserva, Pessoa usuarioLogado) {
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        boolean isGerencial = Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) || 
                usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
                
        model.addAttribute("reserva", reserva);
        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("isGerencial", isGerencial);
        model.addAttribute("showCondominioInfo", Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) && condominiosDisponiveis.size() > 1);
    }
}