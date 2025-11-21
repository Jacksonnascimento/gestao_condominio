package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.VisitanteDTO;
import br.com.gestaocondominio.api.controller.dto.VisitanteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import br.com.gestaocondominio.api.domain.service.VisitanteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/visitantes")
public class VisitanteController {

    @Autowired private VisitanteService visitanteService;
    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private CondominioService condominioService;
    @Autowired private UnidadeService unidadeService;

    private void carregarDadosPadrao(Model model, Pessoa usuarioLogado) {
        model.addAttribute("currentPage", "visitantes");
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());

        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
        model.addAttribute("usuarioPodeGerenciarVisitantes", isGerencial);

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        model.addAttribute("showCondominioInfo", usuarioLogado.getPesIsGlobalAdmin() && condominiosDisponiveis.size() > 1);
    }

  
    private void popularModelParaFragmento(Model model, VisitanteDTO visitanteDTO, Pessoa usuarioLogado) {
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        boolean showCondominioInfo = usuarioLogado.getPesIsGlobalAdmin() && condominiosDisponiveis.size() > 1;
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);

        model.addAttribute("vis", visitanteDTO);
        model.addAttribute("showCondominioInfo", showCondominioInfo);
        model.addAttribute("usuarioPodeGerenciarVisitantes", isGerencial);
    }

    @GetMapping
    public String getPaginaVisitantes(Model model,
                                      @RequestParam(required = false) Integer condominioId,
                                      @RequestParam(required = false) String busca,
                                      @RequestParam(required = false) Integer unidadeId,
                                      @PageableDefault(size = 9, sort = "dataEntrada", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        Page<VisitanteDTO> visitantesPage = visitanteService.consultarVisitantes(usuarioLogado, condominioId, busca, unidadeId, pageable);
        Map<String, Long> totais = visitanteService.contarVisitantes(usuarioLogado, condominioId, busca, unidadeId);

        model.addAttribute("visitantesPage", visitantesPage);
        model.addAttribute("totais", totais);

        
        List<Unidade> unidadesDisponiveis = Collections.emptyList();
        Integer idCondoParaFiltro = condominioId;
        if (idCondoParaFiltro == null && !usuarioLogado.getPesIsGlobalAdmin()) {
            idCondoParaFiltro = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        }
        if (idCondoParaFiltro != null) {
            unidadesDisponiveis = unidadeService.findAtivasByCondominioId(idCondoParaFiltro);
        }
        model.addAttribute("unidadesDisponiveis", unidadesDisponiveis);

        model.addAttribute("condominioFiltro", condominioId);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("unidadeFiltro", unidadeId);

        return "visitantes";
    }

    @GetMapping("/novo")
    @Transactional(readOnly = true)
    public String getFormNovoVisitante(Model model, @RequestParam(required = false) Integer condominioId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        VisitanteRequestDTO dto = new VisitanteRequestDTO();
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

        model.addAttribute("visitanteRequestDTO", dto);
        model.addAttribute("unidadesSelecionaveis", unidadesSelecionaveis);
    

        return "fragments/visitante-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    public Object salvarVisitante(@Valid @ModelAttribute VisitanteRequestDTO dto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("message", errors));
        }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Visitante visitanteSalvo = visitanteService.cadastrarVisitante(dto, usuarioLogado);

            VisitanteDTO visitanteDTO = visitanteService.buscarPorIdDTO(visitanteSalvo.getVisCod(), usuarioLogado);
            popularModelParaFragmento(model, visitanteDTO, usuarioLogado);

            return "fragments/visitante-card :: card";

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/editar/{id}")
    @Transactional(readOnly = true)
    public String getFormEditarVisitante(@PathVariable Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        Visitante visitante = visitanteService.buscarPorIdEValidarAcesso(id, usuarioLogado, true);
        
        VisitanteRequestDTO dto = new VisitanteRequestDTO();
        dto.setNome(visitante.getNome());
        dto.setCpf(visitante.getCpf());
        dto.setRg(visitante.getRg());
        dto.setTelefone(visitante.getTelefone());
        dto.setUnidadeId(visitante.getUnidade().getUniCod());
        dto.setCondominioId(visitante.getCondominio().getConCod());
        dto.setObservacoes(visitante.getObservacoes());
        if(visitante.getMoradorAutorizou() != null) {
            dto.setMoradorId(visitante.getMoradorAutorizou().getPesCod());
        }

      
        List<Unidade> unidadesSelecionaveis = unidadeService.findAtivasByCondominioId(visitante.getCondominio().getConCod());
        
        model.addAttribute("visitanteRequestDTO", dto);
        model.addAttribute("visitanteId", id); 
        model.addAttribute("unidadesSelecionaveis", unidadesSelecionaveis);
        
        
        return "fragments/visitante-form :: form-modal-content";
    }

    @PostMapping("/editar/{id}")
    public Object atualizarVisitante(@PathVariable Integer id, @Valid @ModelAttribute VisitanteRequestDTO dto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("message", errors));
        }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Visitante visitanteAtualizado = visitanteService.atualizarVisitante(id, dto, usuarioLogado);

            VisitanteDTO visitanteDTO = visitanteService.buscarPorIdDTO(visitanteAtualizado.getVisCod(), usuarioLogado);
            popularModelParaFragmento(model, visitanteDTO, usuarioLogado);

            return "fragments/visitante-card :: card";

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/saida/{id}")
    @Transactional(readOnly = true)
    public String getModalRegistrarSaida(@PathVariable Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Visitante visitante = visitanteService.buscarPorIdEValidarAcesso(id, usuarioLogado, true);
        VisitanteDTO dto = new VisitanteDTO(visitante);
        model.addAttribute("vis", dto);
        return "fragments/visitante-saida-modal :: modal-content";
    }

    @PostMapping("/saida/{id}")
    public Object registrarSaida(@PathVariable Integer id, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Visitante visitanteAtualizado = visitanteService.registrarSaida(id, usuarioLogado);

            VisitanteDTO visitanteDTO = visitanteService.buscarPorIdDTO(visitanteAtualizado.getVisCod(), usuarioLogado);
            popularModelParaFragmento(model, visitanteDTO, usuarioLogado);

            return "fragments/visitante-card :: card";

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}