package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/unidades")
public class UnidadeViewController {

    @Autowired private UnidadeService unidadeService;
    @Autowired private CondominioService condominioService;
    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;

    private void carregarDadosPadrao(Model model) {
        model.addAttribute("currentPage", "unidades");
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listarUnidades(Model model,
                                     @RequestParam(required = false) Integer condominioId,
                                     @RequestParam(required = false) String busca,
                                     @RequestParam(required = false, name = "status") String statusFiltro) {
        
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model);

        List<Unidade> unidades = unidadeService.listarTodasUnidades(true, statusFiltro, busca);

        if (condominioId != null) {
            unidades = unidades.stream()
                .filter(u -> u.getCondominio().getConCod().equals(condominioId))
                .collect(Collectors.toList());
        }
        
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
        model.addAttribute("isGerencial", isGerencial);

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false); // Usando o método existente
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        
        boolean showCondominioInfo = condominiosDisponiveis.size() > 1;
        
        model.addAttribute("unidades", unidades);
        model.addAttribute("condominioFiltro", condominioId);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("statusFiltro", statusFiltro);
        model.addAttribute("showCondominioInfo", showCondominioInfo);
        
        model.addAttribute("totalUnidades", unidades.size());
        model.addAttribute("totalOcupadas", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.OCUPADA).count());
        model.addAttribute("totalVazias", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.VAZIA).count());
        model.addAttribute("totalMultipropriedade", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.MULTIPROPRIEDADE).count());
        model.addAttribute("totalEmReforma", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.EM_REFORMA).count());

        return "unidades";
    }

    @GetMapping("/novo")
    public String mostrarFormularioNovaUnidade(Model model) {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false); // Usando o método existente
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);

        boolean showCondominioInfo = condominiosDisponiveis.size() > 1;
        if (!showCondominioInfo && !condominiosDisponiveis.isEmpty()) {
            dto.setConCod(condominiosDisponiveis.get(0).getConCod());
        }

        model.addAttribute("showCondominioInfo", showCondominioInfo);
        model.addAttribute("unidade", dto);
        carregarDadosPadrao(model);
        return "fragments/unidade-form :: form-modal-content";
    }
    
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarUnidade(@PathVariable("id") Integer id, Model model) {
        Optional<Unidade> unidadeOpt = unidadeService.buscarUnidadePorId(id);

        if (unidadeOpt.isPresent()) {
            Unidade unidade = unidadeOpt.get();
            UnidadeRequestDTO dto = new UnidadeRequestDTO(unidade);

            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false); // Usando o método existente
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);

            boolean showCondominioInfo = condominiosDisponiveis.size() > 1;
            
            model.addAttribute("showCondominioInfo", showCondominioInfo);
            model.addAttribute("unidadeId", id);
            model.addAttribute("unidade", dto);
            carregarDadosPadrao(model);
            return "fragments/unidade-form :: form-modal-content";
        }
        return "error";
    }

    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<?> salvarUnidade(@ModelAttribute("unidade") UnidadeRequestDTO dto) {
        try {
            Unidade novaUnidade = unidadeService.cadastrarUnidade(dto);
            return ResponseEntity.ok(new UnidadeRequestDTO(novaUnidade));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
    
    @PostMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<?> atualizarUnidade(@PathVariable("id") Integer id, @ModelAttribute("unidade") UnidadeRequestDTO dto) {
        try {
            Unidade unidadeAtualizada = unidadeService.atualizarUnidade(id, dto);
            return ResponseEntity.ok(new UnidadeRequestDTO(unidadeAtualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/excluir/{id}")
    public String inativarUnidade(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            unidadeService.inativarUnidade(id);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade desativada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao desativar unidade: " + e.getMessage());
        }
        return "redirect:/unidades";
    }
}