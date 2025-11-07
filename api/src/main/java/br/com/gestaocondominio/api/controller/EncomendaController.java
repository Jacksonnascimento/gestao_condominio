package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.EncomendaDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRetiradaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaStatusRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Encomenda;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import br.com.gestaocondominio.api.domain.enums.EncomendaTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/encomendas")
public class EncomendaController {

    @Autowired private EncomendaService encomendaService;
    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private CondominioService condominioService;
    @Autowired private UnidadeService unidadeService;

    private void carregarDadosPadrao(Model model, Pessoa usuarioLogado) {
        model.addAttribute("currentPage", "encomendas");
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());

        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
        model.addAttribute("usuarioPodeGerenciarEncomendas", isGerencial);

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        model.addAttribute("showCondominioInfo", usuarioLogado.getPesIsGlobalAdmin() && condominiosDisponiveis.size() > 1);

        model.addAttribute("statusDisponiveis", Stream.of(EncomendaStatus.values())
                .collect(Collectors.toMap(EncomendaStatus::name, EncomendaStatus::getDescricao)));
    }

    @GetMapping
    public String getPaginaEncomendas(Model model,
                                      @RequestParam(required = false) Integer condominioId,
                                      @RequestParam(required = false) String busca,
                                      @RequestParam(required = false) Integer unidadeId,
                                      @RequestParam(required = false) EncomendaStatus status,
                                      @PageableDefault(size = 9, sort = "dataRecebimento", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        model.addAttribute("encomendasPage", encomendaService.consultarEncomendas(usuarioLogado, condominioId, busca, unidadeId, status, pageable));
        model.addAttribute("totais", encomendaService.contarStatusEncomendas(usuarioLogado, condominioId, busca, unidadeId, status));

        List<Unidade> unidadesDisponiveis = Collections.emptyList();
        
        // CORREÇÃO (Linha 75): Checagem segura de boolean (Null-safe)
        if (Boolean.TRUE.equals(model.getAttribute("usuarioPodeGerenciarEncomendas"))) {
            Integer idCondoParaFiltro = condominioId;
            if (idCondoParaFiltro == null && !usuarioLogado.getPesIsGlobalAdmin()) {
                idCondoParaFiltro = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            }
            if (idCondoParaFiltro != null) {
                unidadesDisponiveis = unidadeService.findAtivasByCondominioId(idCondoParaFiltro);
            }
        }
        model.addAttribute("unidadesDisponiveis", unidadesDisponiveis);
        
        model.addAttribute("condominioFiltro", condominioId);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("unidadeFiltro", unidadeId);
        model.addAttribute("statusFiltro", status);

        return "encomendas";
    }

    @GetMapping("/novo")
    @Transactional(readOnly = true) // CORREÇÃO: Adicionado para evitar LazyInit no findAtivasByCondominioId
    public String getFormNovaEncomenda(Model model, @RequestParam(required = false) Integer condominioId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        EncomendaRequestDTO dto = new EncomendaRequestDTO();
        dto.setDataRecebimento(LocalDate.now());
        dto.setHoraRecebimento(LocalTime.now().withSecond(0).withNano(0));
        dto.setNomeRecebidoPor(usuarioLogado.getPesNome());
        dto.setTipo(EncomendaTipo.CORREIOS);

        List<Unidade> unidadesSelecionaveis = Collections.emptyList();
        Integer idCondoParaForm = condominioId;

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            if (idCondoParaForm != null) {
                unidadesSelecionaveis = unidadeService.findAtivasByCondominioId(idCondoParaForm);
                dto.setCondominioId(idCondoParaForm);
            }
        } else {
            idCondoParaForm = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            if (idCondoParaForm != null) {
                unidadesSelecionaveis = unidadeService.findAtivasByCondominioId(idCondoParaForm);
                dto.setCondominioId(idCondoParaForm);
            }
        }

        model.addAttribute("encomendaRequestDTO", dto);
        model.addAttribute("unidadesSelecionaveis", unidadesSelecionaveis);
        model.addAttribute("tiposEncomenda", EncomendaTipo.values());

        return "fragments/encomenda-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<?> salvarEncomenda(@Valid @ModelAttribute EncomendaRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("message", errors));
        }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            encomendaService.criarEncomenda(dto, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Encomenda registrada com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/retirar")
    @Transactional(readOnly = true) // CORREÇÃO: Adicionado para corrigir LazyInitializationException
    public String getFormRetirarEncomenda(@PathVariable Long id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Encomenda encomenda = encomendaService.buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        EncomendaRetiradaRequestDTO dto = new EncomendaRetiradaRequestDTO();
        dto.setDataRetirada(LocalDate.now());
        dto.setHoraRetirada(LocalTime.now().withSecond(0).withNano(0));

        // CORREÇÃO (Linha 153): Classe EncomendaDTO agora é importada
        model.addAttribute("encomenda", new EncomendaDTO(encomenda));
        model.addAttribute("retiradaRequestDTO", dto);
        return "fragments/encomenda-retirada-form :: form-modal-content";
    }

    @PostMapping("/{id}/retirar")
    @ResponseBody
    public ResponseEntity<?> registrarRetirada(@PathVariable Long id, @Valid @ModelAttribute EncomendaRetiradaRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("message", errors));
        }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            encomendaService.registrarRetirada(id, dto, usuarioLogado);
            return ResponseEntity.ok(Map.of("message", "Retirada registrada com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/atualizar-status")
    @Transactional(readOnly = true) // CORREÇÃO: Adicionado para corrigir LazyInitializationException
    public String getFormAtualizarStatus(@PathVariable Long id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Encomenda encomenda = encomendaService.buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        EncomendaStatusRequestDTO dto = new EncomendaStatusRequestDTO();
        dto.setObservacoes(encomenda.getObservacaoAtualizacao());
        dto.setNovoStatus(encomenda.getStatus());

        // CORREÇÃO (Linha 183): Classe EncomendaDTO agora é importada
        model.addAttribute("encomenda", new EncomendaDTO(encomenda));
        model.addAttribute("statusRequestDTO", dto);
        model.addAttribute("statusUpdateDisponiveis", List.of(EncomendaStatus.PENDENTE, EncomendaStatus.DEVOLVIDA, EncomendaStatus.EXTRAVIADA));
        return "fragments/encomenda-status-form :: form-modal-content";
    }

    @PostMapping("/{id}/atualizar-status")
    @ResponseBody
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id, @Valid @ModelAttribute EncomendaStatusRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("message", errors));
        }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            encomendaService.atualizarStatus(id, dto, usuarioLogado);
            return ResponseEntity.ok(Map.of("message", "Status atualizado com sucesso."));
        } catch (ResponseStatusException e) {
             return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}