package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/unidades")
public class UnidadeViewController {

    private final UnidadeService unidadeService;
    private final CondominioRepository condominioRepository;

    public UnidadeViewController(@Qualifier("unidadeServiceImpl") UnidadeService unidadeService,
                                 CondominioRepository condominioRepository) {
        this.unidadeService = unidadeService;
        this.condominioRepository = condominioRepository;
    }

    private List<Condominio> getCondominiosAcessiveis(Authentication auth) {
        if (hasAuthority(auth, "ROLE_GLOBAL_ADMIN")) {
            return condominioRepository.findAll();
        }
        Set<Integer> condoIds = getCondoIdsFromRoles(auth, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (condoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return condominioRepository.findAllById(condoIds);
    }

    private void preencherModelComListas(Model model, List<Condominio> condominiosAcessiveis) {
        model.addAttribute("condominiosDisponiveis", condominiosAcessiveis);
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
        model.addAttribute("isMultiCondo", condominiosAcessiveis.size() > 1);
    }

    @GetMapping
    public String redirecionarParaCondominioPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Condominio> condominios = getCondominiosAcessiveis(authentication);
        if (condominios.isEmpty()) {
            return "redirect:/dashboard"; 
        }
        return "redirect:/unidades/" + condominios.get(0).getConCod();
    }

    @GetMapping("/{condominioId}")
    public String getUnidadesPorCondominio(@PathVariable Integer condominioId, 
                                           @RequestParam(value = "busca", required = false) String busca, 
                                           @RequestParam(value = "status", required = false) String statusFiltro,
                                           Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Condominio> condominiosAcessiveis = getCondominiosAcessiveis(authentication);
        
        if (condominiosAcessiveis.stream().noneMatch(c -> c.getConCod().equals(condominioId))) {
             throw new SecurityException("Acesso negado a este condomínio.");
        }

        List<Unidade> unidades = unidadeService.listarTodasUnidades(false, statusFiltro, busca)
                                    .stream()
                                    .filter(u -> u.getCondominio().getConCod().equals(condominioId))
                                    .collect(Collectors.toList());
        
        model.addAttribute("unidades", unidades);
        model.addAttribute("condominioId", condominioId);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("statusFiltro", statusFiltro);
        
        preencherModelComListas(model, condominiosAcessiveis);
        
        long totalUnidades = unidadeService.findByCondominioId(condominioId).size();
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("totalOcupadas", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.OCUPADA).count());
        model.addAttribute("totalVazias", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.VAZIA).count());
        model.addAttribute("totalMultipropriedade", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.MULTIPROPRIEDADE).count());
        model.addAttribute("totalEmReforma", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.EM_REFORMA).count());
        
        return "unidades";
    }
    
    @GetMapping("/novo")
    public String mostrarFormularioNovaUnidade(@RequestParam(value = "condominioId", required = false) Integer condominioId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Condominio> condominiosAcessiveis = getCondominiosAcessiveis(authentication);
        
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        if (condominiosAcessiveis.size() == 1) {
            dto.setConCod(condominiosAcessiveis.get(0).getConCod());
        } else if (condominioId != null) {
            dto.setConCod(condominioId);
        }

        model.addAttribute("unidade", dto);
        model.addAttribute("unidadeId", null);
        preencherModelComListas(model, condominiosAcessiveis);
        return "fragments/unidade-form :: form-modal-content";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarUnidade(@PathVariable("id") Integer id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Condominio> condominiosAcessiveis = getCondominiosAcessiveis(authentication);
        
        Optional<Unidade> unidadeOpt = unidadeService.buscarUnidadePorId(id);
        if (unidadeOpt.isPresent()) {
            Unidade unidade = unidadeOpt.get();
            UnidadeRequestDTO dto = new UnidadeRequestDTO();
            
            dto.setUniNumero(unidade.getUniNumero());
            dto.setBloco(unidade.getBloco());
            dto.setAndar(unidade.getAndar());
            dto.setFracaoIdeal(unidade.getFracaoIdeal());
            dto.setAreaPrivada(unidade.getAreaPrivada());
            dto.setUnidadeTipo(unidade.getUnidadeTipo());
            dto.setUniStatusOcupacao(unidade.getUniStatusOcupacao());
            dto.setObservacao(unidade.getObservacao());
            dto.setConCod(unidade.getCondominio().getConCod());
            
            model.addAttribute("unidadeId", id);
            model.addAttribute("unidade", dto);
            preencherModelComListas(model, condominiosAcessiveis);
            return "fragments/unidade-form :: form-modal-content";
        }
        return "error";
    }

    @PostMapping("/salvar")
    public String salvarUnidade(@ModelAttribute("unidade") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            Unidade unidadeSalva = unidadeService.cadastrarUnidade(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade salva com sucesso!");
            return "redirect:/unidades/" + unidadeSalva.getCondominio().getConCod();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar unidade: " + e.getMessage());
            if (dto.getConCod() != null) {
                return "redirect:/unidades/" + dto.getConCod();
            }
            return "redirect:/unidades";
        }
    }
    
    @PostMapping("/editar/{id}")
    public String atualizarUnidade(@PathVariable("id") Integer id, @ModelAttribute("unidade") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            Unidade unidadeAtualizada = unidadeService.atualizarUnidade(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade atualizada com sucesso!");
            return "redirect:/unidades/" + unidadeAtualizada.getCondominio().getConCod();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar unidade: " + e.getMessage());
            if (dto.getConCod() != null) {
                return "redirect:/unidades/" + dto.getConCod();
            }
            return "redirect:/unidades";
        }
    }

    @PostMapping("/excluir/{id}")
    public String inativarUnidade(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Integer condominioId = null;
        try {
            Optional<Unidade> unidadeOpt = unidadeService.buscarUnidadePorId(id);
            if (unidadeOpt.isPresent()) {
                condominioId = unidadeOpt.get().getCondominio().getConCod();
                unidadeService.inativarUnidade(id);
                redirectAttributes.addFlashAttribute("successMessage", "Unidade desativada com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Erro: Unidade não encontrada.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao desativar unidade: " + e.getMessage());
        }

        if (condominioId != null) {
            return "redirect:/unidades/" + condominioId;
        }
        return "redirect:/unidades";
    }
    
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}