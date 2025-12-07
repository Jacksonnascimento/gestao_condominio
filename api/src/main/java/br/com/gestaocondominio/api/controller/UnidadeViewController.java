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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private boolean getShowCondominioInfo(List<Condominio> condominiosDisponiveis) {
         return condominiosDisponiveis.size() > 1;
    }
    
    private boolean getIsGerencial(Pessoa usuarioLogado) {
        return usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listarUnidades(Model model,
                                     @RequestParam(required = false) Integer condominioId,
                                     @RequestParam(required = false) String busca,
                                     @RequestParam(required = false, name = "status") String statusFiltro) {
        
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model);

        List<Unidade> unidades = unidadeService.listarTodasUnidades(false, statusFiltro, busca);

        if (condominioId != null) {
            unidades = unidades.stream()
                .filter(u -> u.getCondominio().getConCod().equals(condominioId))
                .collect(Collectors.toList());
        }
        
        model.addAttribute("isGerencial", getIsGerencial(usuarioLogado));

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        
        boolean showCondominioInfo = getShowCondominioInfo(condominiosDisponiveis);
        
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
        
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);

        boolean showCondominioInfo = getShowCondominioInfo(condominiosDisponiveis);
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

            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);

            boolean showCondominioInfo = getShowCondominioInfo(condominiosDisponiveis);
            
            model.addAttribute("showCondominioInfo", showCondominioInfo);
            model.addAttribute("unidadeId", id);
            model.addAttribute("unidade", dto);
            carregarDadosPadrao(model);
            return "fragments/unidade-form :: form-modal-content";
        }
        return "error";
    }

    @PostMapping("/salvar")
    public Object salvarUnidade(@ModelAttribute("unidade") UnidadeRequestDTO dto, Model model) {
        try {
            Unidade novaUnidade = unidadeService.cadastrarUnidade(dto);
            
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
            
            model.addAttribute("unidade", novaUnidade);
            model.addAttribute("showCondominioInfo", getShowCondominioInfo(condominiosDisponiveis));
            model.addAttribute("isGerencial", getIsGerencial(pessoaService.getLoggedInUser()));

            return "fragments/unidade-card :: card";
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
    
    @PostMapping("/editar/{id}")
    public Object atualizarUnidade(@PathVariable("id") Integer id, @ModelAttribute("unidade") UnidadeRequestDTO dto, Model model) {
        try {
            Unidade unidadeAtualizada = unidadeService.atualizarUnidade(id, dto);

            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
            
            model.addAttribute("unidade", unidadeAtualizada);
            model.addAttribute("showCondominioInfo", getShowCondominioInfo(condominiosDisponiveis));
            model.addAttribute("isGerencial", getIsGerencial(pessoaService.getLoggedInUser()));
            
            return "fragments/unidade-card :: card";

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/excluir/{id}")
    @ResponseBody
    public ResponseEntity<?> inativarUnidade(@PathVariable("id") Integer id) {
        try {
            unidadeService.inativarUnidade(id);
            return ResponseEntity.ok(Map.of("message", "Unidade desativada com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erro ao desativar unidade: " + e.getMessage()));
        }
    }
}