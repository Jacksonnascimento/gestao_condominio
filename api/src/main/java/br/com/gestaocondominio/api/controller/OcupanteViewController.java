package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @PreAuthorize("isAuthenticated()")
    public String listarOcupantes(Model model,
                                      @RequestParam(required = false) Integer condominioId,
                                      @RequestParam(required = false) String busca,
                                      @RequestParam(required = false) OcupanteVinculo vinculo,
                                      @RequestParam(required = false) Integer unidadeId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model);

        List<OcupanteResponseDTO> ocupantes = ocupanteService.consultarOcupantesPorUsuario(usuarioLogado, condominioId, busca, vinculo, unidadeId);
        Map<OcupanteVinculo, Long> totais = ocupanteService.contarOcupantesPorUsuario(usuarioLogado, condominioId);
        
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
        boolean showCondominioInfo = false;
        List<Unidade> unidadesDisponiveis = new ArrayList<>();

        if (isGerencial) {
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
                if (idCondoUsuario != null) {
                    unidadesDisponiveis = unidadeService.findByCondominioId(idCondoUsuario);
                }
            }
        } else { // MORADOR
            Optional<Unidade> unidadeDoMoradorOpt = ocupanteService.findUnidadeByMorador(usuarioLogado);
            if (unidadeDoMoradorOpt.isPresent()) {
                Unidade unidadeDoMorador = unidadeDoMoradorOpt.get();
                unidadesDisponiveis.add(unidadeDoMorador);
                model.addAttribute("unidadeDoMorador", unidadeDoMorador);
            }
        }
        
        model.addAttribute("isGerencial", isGerencial);
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
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
        
        model.addAttribute("isGerencial", isGerencial);
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
        
        if (isGerencial) {
             if (usuarioLogado.getPesIsGlobalAdmin()) {
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
        } else { // MORADOR
             ocupanteService.findUnidadeByMorador(usuarioLogado).ifPresent(unidade -> {
                  dto.setCondominioId(unidade.getCondominio().getConCod());
                  dto.setUnidadeId(unidade.getUniCod());
                  model.addAttribute("unidadesDisponiveis", Collections.singletonList(unidade));
                  model.addAttribute("unidadeDoMorador", unidade);
             });
        }

        model.addAttribute("ocupanteRequestDTO", dto);
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("tiposPeriodoDisponiveis", TipoPeriodoOcupante.values());
        return "fragments/ocupante-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<?> salvarOcupante(OcupanteRequestDTO ocupanteRequestDTO) {
        try {
            OcupanteResponseDTO responseDTO = ocupanteService.cadastrarOcupante(ocupanteRequestDTO);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/editar/{id}")
    public String getFormularioEditarOcupante(@PathVariable Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Ocupante ocupante = ocupanteService.buscarPorIdEValidarAcesso(id, usuarioLogado);

        OcupanteRequestDTO dto = new OcupanteRequestDTO(ocupante);
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);

        model.addAttribute("ocupanteRequestDTO", dto);
        model.addAttribute("ocupanteId", id);
        model.addAttribute("isGerencial", isGerencial);
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("tiposPeriodoDisponiveis", TipoPeriodoOcupante.values());
        
        if (isGerencial) {
            if (usuarioLogado.getPesIsGlobalAdmin()) {
                List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
                model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
                model.addAttribute("showCondominioInfo", condominiosDisponiveis.size() > 1);
            }
            Integer condominioDoOcupanteId = ocupante.getUnidade().getCondominio().getConCod();
            model.addAttribute("unidadesDisponiveis", unidadeService.findByCondominioId(condominioDoOcupanteId));
        } else {
             model.addAttribute("unidadesDisponiveis", Collections.singletonList(ocupante.getUnidade()));
             model.addAttribute("unidadeDoMorador", ocupante.getUnidade());
        }

        return "fragments/ocupante-form :: form-modal-content";
    }

    @PostMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<?> atualizarOcupante(@PathVariable Integer id, OcupanteRequestDTO ocupanteRequestDTO) {
        try {
            OcupanteResponseDTO responseDTO = ocupanteService.editarOcupante(id, ocupanteRequestDTO, pessoaService.getLoggedInUser());
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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