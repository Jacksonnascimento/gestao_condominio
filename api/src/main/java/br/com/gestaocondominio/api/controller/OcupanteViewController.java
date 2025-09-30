package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ocupantes")
public class OcupanteViewController {

    private final OcupanteService ocupanteService;
    private final CondominioService condominioService;
    private final UnidadeService unidadeService;
    private final PessoaService pessoaService;

    public OcupanteViewController(OcupanteService ocupanteService, CondominioService condominioService, UnidadeService unidadeService, PessoaService pessoaService) {
        this.ocupanteService = ocupanteService;
        this.condominioService = condominioService;
        this.unidadeService = unidadeService;
        this.pessoaService = pessoaService;
    }

    private void addUserDetailsToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            model.addAttribute("nomeUsuarioLogado", userDetails.getPessoa().getPesNome());
        }
    }

    @GetMapping
    public String getOcupantesPage(
            @RequestParam(required = false, defaultValue = "") String busca,
            @RequestParam(required = false, defaultValue = "") String vinculo,
            @RequestParam(required = false) Integer unidadeId,
            Model model) {

        addUserDetailsToModel(model);
        List<Ocupante> todosOcupantes = ocupanteService.consultarOcupantes(null, null, null);
        List<Ocupante> ocupantesFiltrados = ocupanteService.consultarOcupantes(busca, vinculo, unidadeId);
        
        List<OcupanteResponseDTO> ocupantesDTO = ocupantesFiltrados.stream()
                .map(OcupanteResponseDTO::new)
                .collect(Collectors.toList());
        
        model.addAttribute("ocupantes", ocupantesDTO);

        Map<OcupanteVinculo, Long> contagem = todosOcupantes.stream()
                .collect(Collectors.groupingBy(Ocupante::getOcuVinculo, Collectors.counting()));

        model.addAttribute("totalOcupantes", todosOcupantes.size());
        model.addAttribute("totalProprietarios", contagem.getOrDefault(OcupanteVinculo.PROPRIETARIO, 0L));
        model.addAttribute("totalLocatarios", contagem.getOrDefault(OcupanteVinculo.LOCATARIO, 0L));
        model.addAttribute("totalPromitentes", contagem.getOrDefault(OcupanteVinculo.PROMITENTE_COMPRADOR, 0L));
        model.addAttribute("totalCessionarios", contagem.getOrDefault(OcupanteVinculo.CESSIONARIO, 0L));
        model.addAttribute("totalMultiproprietarios", contagem.getOrDefault(OcupanteVinculo.MULTIPROPRIETARIO, 0L));

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
        model.addAttribute("isMultiCondo", condominiosDisponiveis.size() > 1);
        model.addAttribute("vinculosDisponiveis", OcupanteVinculo.values());
        model.addAttribute("unidadesDisponiveis", unidadeService.listarTodasUnidades(true, null, null));
        
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("vinculoFiltro", vinculo);
        model.addAttribute("unidadeFiltro", unidadeId);
        model.addAttribute("currentPage", "ocupantes");
        
        return "ocupantes";
    }

    @GetMapping("/novo")
    public String getFormularioNovoOcupante(Model model) {
        addUserDetailsToModel(model);
       
        model.addAttribute("ocupanteRequestDTO", new OcupanteRequestDTO(null, null, null, null, 'F', null, null, null, null, null, null, null, null));
        model.addAttribute("pessoasDisponiveis", pessoaService.listarPessoasAutorizadas());
        model.addAttribute("unidadesDisponiveis", unidadeService.listarTodasUnidades(true, null, null));
        model.addAttribute("vinculosDisponiveis", Arrays.asList(OcupanteVinculo.values()));
        model.addAttribute("tiposPeriodoDisponiveis", Arrays.asList(TipoPeriodoOcupante.values()));
        model.addAttribute("currentPage", "ocupantes");
        return "fragments/ocupante-form :: form-modal-content";
    }

    @GetMapping("/editar/{id}")
    public String getFormularioEditarOcupante(@PathVariable Integer id, Model model) {
        addUserDetailsToModel(model);
        Ocupante ocupante = ocupanteService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("ID do Ocupante inválido:" + id));
        
        
        OcupanteRequestDTO dto = new OcupanteRequestDTO(
            null, 
            ocupante.getPessoa().getPesCod(),
            ocupante.getPessoa().getPesNome(),
            ocupante.getPessoa().getPesCpfCnpj(),
            ocupante.getPessoa().getPesTipo(),
            ocupante.getPessoa().getPesEmail(),
            ocupante.getPessoa().getPesTelefone(),
            ocupante.getUnidade().getUniCod(),
            ocupante.getOcuVinculo(),
            ocupante.getOcuDtInicioOcupacao(),
            ocupante.getOcuDtFimOcupacao(),
            ocupante.getOcuPeriodoUso(),
            ocupante.getOcuTipoPeriodo()
        );
        
        model.addAttribute("ocupanteRequestDTO", dto);
        model.addAttribute("ocupanteId", id);
        model.addAttribute("unidadesDisponiveis", List.of(ocupante.getUnidade()));
        model.addAttribute("vinculosDisponiveis", Arrays.asList(OcupanteVinculo.values()));
        model.addAttribute("tiposPeriodoDisponiveis", Arrays.asList(TipoPeriodoOcupante.values()));
        model.addAttribute("currentPage", "ocupantes");
        return "fragments/ocupante-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    public String salvarOcupante(OcupanteRequestDTO ocupanteRequestDTO) {
        ocupanteService.cadastrarOcupante(ocupanteRequestDTO);
        return "redirect:/ocupantes";
    }

    @PostMapping("/editar/{id}")
    public String atualizarOcupante(@PathVariable Integer id, OcupanteRequestDTO ocupanteRequestDTO) {
        ocupanteService.editarOcupante(id, ocupanteRequestDTO);
        return "redirect:/ocupantes";
    }

    @PostMapping("/excluir/{id}")
    public String excluirOcupante(@PathVariable Integer id) {
        ocupanteService.excluirOcupante(id);
        return "redirect:/ocupantes";
    }
}