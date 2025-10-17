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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                                 @RequestParam(required = false) String status) {
        
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model);

        List<Unidade> unidades = unidadeService.listarTodasUnidades(true, status, busca);

        if (condominioId != null) {
            unidades = unidades.stream()
                .filter(u -> u.getCondominio().getConCod().equals(condominioId))
                .collect(Collectors.toList());
        }
        
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
        model.addAttribute("isGerencial", isGerencial);

        boolean showCondominioInfo = false;
        if (usuarioLogado.getPesIsGlobalAdmin()) {
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
            if (condominiosDisponiveis.size() > 1) {
                showCondominioInfo = true;
            }
        }
        
        model.addAttribute("unidades", unidades);
        model.addAttribute("condominioFiltro", condominioId);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("showCondominioInfo", showCondominioInfo);
        
        long totalUnidades = unidades.size();
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("totalOcupadas", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.OCUPADA).count());
        model.addAttribute("totalVazias", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.VAZIA).count());
        model.addAttribute("totalMultipropriedade", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.MULTIPROPRIEDADE).count());
        model.addAttribute("totalEmReforma", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.EM_REFORMA).count());

        return "unidades";
    }

    @GetMapping("/novo")
    public String mostrarFormularioNovaUnidade(Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        
        boolean isGlobalAdmin = usuarioLogado.getPesIsGlobalAdmin();
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);
        
        boolean showCondominioInfo = false;
        if(isGlobalAdmin){
             List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
             model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
             if (condominiosDisponiveis.size() > 1) {
                showCondominioInfo = true;
             }
        } else {
            Integer idCondoUsuario = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            dto.setConCod(idCondoUsuario);
        }
        model.addAttribute("showCondominioInfo", showCondominioInfo);
        
        model.addAttribute("unidade", dto);
        carregarDadosPadrao(model);
        return "fragments/unidade-form :: form-modal-content";
    }
    
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarUnidade(@PathVariable("id") Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Optional<Unidade> unidadeOpt = unidadeService.buscarUnidadePorId(id);

        if (unidadeOpt.isPresent()) {
            Unidade unidade = unidadeOpt.get();
            UnidadeRequestDTO dto = new UnidadeRequestDTO(unidade);

            boolean isGlobalAdmin = usuarioLogado.getPesIsGlobalAdmin();
            model.addAttribute("isGlobalAdmin", isGlobalAdmin);
            
            boolean showCondominioInfo = false;
            if(isGlobalAdmin){
                List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
                model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
                if (condominiosDisponiveis.size() > 1) {
                    showCondominioInfo = true;
                }
            }
            model.addAttribute("showCondominioInfo", showCondominioInfo);
            
            model.addAttribute("unidadeId", id);
            model.addAttribute("unidade", dto);
            carregarDadosPadrao(model);
            return "fragments/unidade-form :: form-modal-content";
        }
        return "error";
    }

    @PostMapping("/salvar")
    public String salvarUnidade(@ModelAttribute("unidade") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            unidadeService.cadastrarUnidade(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar unidade: " + e.getMessage());
        }
        return "redirect:/unidades";
    }
    
    @PostMapping("/editar/{id}")
    public String atualizarUnidade(@PathVariable("id") Integer id, @ModelAttribute("unidade") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            unidadeService.atualizarUnidade(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade atualizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar unidade: " + e.getMessage());
        }
        return "redirect:/unidades";
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