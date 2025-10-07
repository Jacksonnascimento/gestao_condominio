package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

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

    @GetMapping
    public String redirecionarParaPrimeiroCondominio() {
        List<Condominio> condominios = condominioRepository.findAll();
        if (condominios.isEmpty()) {
            return "redirect:/";
        }
        return "redirect:/unidades/" + condominios.get(0).getConCod();
    }

    @GetMapping("/{condominioId}")
    public String getUnidadesPorCondominio(@PathVariable Integer condominioId, Model model) {
        List<Unidade> unidades = unidadeService.findByCondominioId(condominioId);
        List<Condominio> condominios = condominioRepository.findAll();
        
        model.addAttribute("unidades", unidades);
        model.addAttribute("condominioId", condominioId);
        model.addAttribute("condominiosDisponiveis", condominios);
        model.addAttribute("unidadeDTO", new UnidadeRequestDTO());
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());

        model.addAttribute("totalUnidades", unidades.size());
        model.addAttribute("totalOcupadas", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.OCUPADA).count());
        model.addAttribute("totalVazias", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.VAZIA).count());
        model.addAttribute("totalMultipropriedade", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.MULTIPROPRIEDADE).count());
        model.addAttribute("totalEmReforma", unidades.stream().filter(u -> u.getUniStatusOcupacao() == UnidadeStatusOcupacao.EM_REFORMA).count());
        
        return "unidades";
    }
    
    @GetMapping("/novo")
    public String exibirFormularioNovaUnidade(@RequestParam(value = "condominioId", required = false) Integer condominioId, Model model) {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        if (condominioId != null) {
            dto.setConCod(condominioId);
        }
        model.addAttribute("unidadeDTO", dto);
        model.addAttribute("condominiosDisponiveis", condominioRepository.findAll());
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
        return "fragments/unidade-form :: form-modal-content";
    }

    @GetMapping("/editar/{id}")
    public String exibirFormularioEditarUnidade(@PathVariable("id") Integer id, Model model) {
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
            model.addAttribute("unidadeDTO", dto);
            model.addAttribute("condominiosDisponiveis", condominioRepository.findAll());
            model.addAttribute("tiposUnidade", UnidadeTipo.values());
            model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
            return "fragments/unidade-form :: form-modal-content";
        }
        return "error";
    }

    @PostMapping("/salvar")
    public String salvarUnidade(@ModelAttribute("unidadeDTO") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            unidadeService.cadastrarUnidade(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar unidade: " + e.getMessage());
        }
        return "redirect:/unidades/" + dto.getConCod();
    }
    
    @PostMapping("/editar/{id}")
    public String atualizarUnidade(@PathVariable("id") Integer id, @ModelAttribute("unidadeDTO") UnidadeRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            unidadeService.atualizarUnidade(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Unidade atualizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar unidade: " + e.getMessage());
        }
        return "redirect:/unidades/" + dto.getConCod();
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
                return "redirect:/unidades";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao desativar unidade: " + e.getMessage());
            if (condominioId != null) {
                return "redirect:/unidades/" + condominioId;
            }
            return "redirect:/unidades";
        }
        return "redirect:/unidades/" + condominioId;
    }
}