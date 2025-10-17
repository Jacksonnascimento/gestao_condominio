package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ocupantes")
public class OcupanteViewController {

    @Autowired private OcupanteService ocupanteService;
    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private CondominioService condominioService;
    @Autowired private UnidadeService unidadeService;

    private void carregarDadosPadrao(Model model) {
        model.addAttribute("currentPage", "ocupantes");
    }

    @GetMapping
    public String listarOcupantes(Model model,
                                  @RequestParam(required = false) Integer condominioId,
                                  @RequestParam(required = false) String busca,
                                  @RequestParam(required = false) OcupanteVinculo vinculo,
                                  @RequestParam(required = false) Integer unidadeId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model);

        // ===== LINHA CORRIGIDA =====
        List<OcupanteResponseDTO> ocupantes = ocupanteService.consultarOcupantesPorUsuario(usuarioLogado, condominioId, busca, vinculo);
        
        Map<OcupanteVinculo, Long> totais = ocupanteService.contarOcupantesPorUsuario(usuarioLogado, condominioId);
        boolean showCondominioInfo = false;
        List<Unidade> unidadesDisponiveis = new ArrayList<>();

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
            if (condominiosDisponiveis.size() > 1) {
                showCondominioInfo = true;
            }
            if (condominioId != null) {
                unidadesDisponiveis = unidadeService.findByCondominioId(condominioId);
            }
            model.addAttribute("condominioFiltro", condominioId);
        } else {
             Integer idCondoUsuario = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
             if(idCondoUsuario != null) {
                unidadesDisponiveis = unidadeService.findByCondominioId(idCondoUsuario);
             }
        }

        model.addAttribute("ocupantes", ocupantes);
        model.addAttribute("totalOcupantes", (long) ocupantes.size());
        model.addAttribute("totalProprietarios", totais.getOrDefault(OcupanteVinculo.PROPRIETARIO, 0L));
        model.addAttribute("totalLocatarios", totais.getOrDefault(OcupanteVinculo.LOCATARIO, 0L));
        model.addAttribute("totalPromitentes", totais.getOrDefault(OcupanteVinculo.PROMITENTE_COMPRADOR, 0L));
        model.addAttribute("totalCessionarios", totais.getOrDefault(OcupanteVinculo.CESSIONARIO, 0L));
        model.addAttribute("totalMultiproprietarios", totais.getOrDefault(OcupanteVinculo.MULTIPROPRIETARIO, 0L));
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("vinculoFiltro", vinculo);
        model.addAttribute("unidadeFiltro", unidadeId);
        model.addAttribute("showCondominioInfo", showCondominioInfo);
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("unidadesDisponiveis", unidadesDisponiveis);
        
        return "ocupantes";
    }

    @GetMapping("/novo")
    public String mostrarFormularioNovoOcupante(@RequestParam(required = false) Integer condominioId, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        OcupanteRequestDTO dto = new OcupanteRequestDTO();
        boolean isGlobalAdmin = usuarioLogado.getPesIsGlobalAdmin();
        
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);

        if (isGlobalAdmin) {
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
            model.addAttribute("showCondominioInfo", condominiosDisponiveis.size() > 1);
            if (condominioId != null) {
                dto.setCondominioId(condominioId);
                model.addAttribute("unidadesDisponiveis", unidadeService.findByCondominioId(condominioId));
            } else {
                model.addAttribute("unidadesDisponiveis", Collections.emptyList());
            }
        } else {
            Integer idCondoUsuario = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            dto.setCondominioId(idCondoUsuario);
            model.addAttribute("unidadesDisponiveis", unidadeService.findByCondominioId(idCondoUsuario));
        }

        model.addAttribute("ocupanteRequestDTO", dto);
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("tiposPeriodoDisponiveis", TipoPeriodoOcupante.values());
        return "fragments/ocupante-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    public String salvarOcupante(OcupanteRequestDTO ocupanteRequestDTO, RedirectAttributes redirectAttributes) {
        try {
            ocupanteService.cadastrarOcupante(ocupanteRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Ocupante salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar ocupante: " + e.getMessage());
        }
        return "redirect:/ocupantes";
    }

    @GetMapping("/editar/{id}")
    public String getFormularioEditarOcupante(@PathVariable Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Ocupante ocupante = ocupanteService.buscarPorIdEValidarAcesso(id, usuarioLogado);

        OcupanteRequestDTO dto = new OcupanteRequestDTO(ocupante);
        boolean isGlobalAdmin = usuarioLogado.getPesIsGlobalAdmin();

        model.addAttribute("ocupanteRequestDTO", dto);
        model.addAttribute("ocupanteId", id);
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("tiposPeriodoDisponiveis", TipoPeriodoOcupante.values());

        if (isGlobalAdmin) {
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
            model.addAttribute("showCondominioInfo", condominiosDisponiveis.size() > 1);
        }
        
        Integer condominioDoOcupanteId = ocupante.getUnidade().getCondominio().getConCod();
        model.addAttribute("unidadesDisponiveis", unidadeService.findByCondominioId(condominioDoOcupanteId));

        return "fragments/ocupante-form :: form-modal-content";
    }

    @PostMapping("/editar/{id}")
    public String atualizarOcupante(@PathVariable Integer id, OcupanteRequestDTO ocupanteRequestDTO, RedirectAttributes redirectAttributes) {
        try {
            ocupanteService.editarOcupante(id, ocupanteRequestDTO, pessoaService.getLoggedInUser());
            redirectAttributes.addFlashAttribute("successMessage", "Ocupante atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar ocupante: " + e.getMessage());
        }
        return "redirect:/ocupantes";
    }

    @PostMapping("/excluir/{id}")
    public String excluirOcupante(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            ocupanteService.excluirOcupante(id, pessoaService.getLoggedInUser());
            redirectAttributes.addFlashAttribute("successMessage", "Ocupante excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao excluir ocupante: " + e.getMessage());
        }
        return "redirect:/ocupantes";
    }
}