package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/unidades")
public class UnidadeViewController {

    private static final Logger logger = LoggerFactory.getLogger(UnidadeViewController.class);

    @Autowired
    private UnidadeService unidadeService;

    @Autowired
    private CondominioService condominioService;


    private void addUserDetailsToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        model.addAttribute("nomeUsuarioLogado", userDetails.getPessoa().getPesNome());
    }

    @GetMapping
    public String getUnidadesPage(
            @RequestParam(required = false, defaultValue = "Todos") String status,
            @RequestParam(required = false, defaultValue = "") String busca,
            Model model) {
        
        long startTime = System.currentTimeMillis();
        logger.info("Iniciando o carregamento da página de Unidades...");

        addUserDetailsToModel(model);
        
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
        model.addAttribute("isMultiCondo", condominiosDisponiveis.size() > 1);

        List<Unidade> todasUnidadesAutorizadas = unidadeService.listarTodasUnidades(false, "Todos", null);
        
        Map<UnidadeStatusOcupacao, Long> contagemStatus = todasUnidadesAutorizadas.stream()
                .filter(u -> u.getUniStatusOcupacao() != null)
                .collect(Collectors.groupingBy(Unidade::getUniStatusOcupacao, Collectors.counting()));

        model.addAttribute("totalUnidades", todasUnidadesAutorizadas.size());
        model.addAttribute("totalOcupadas", contagemStatus.getOrDefault(UnidadeStatusOcupacao.OCUPADA, 0L));
        model.addAttribute("totalVazias", contagemStatus.getOrDefault(UnidadeStatusOcupacao.VAZIA, 0L));
        model.addAttribute("totalMultipropriedade", contagemStatus.getOrDefault(UnidadeStatusOcupacao.MULTIPROPRIEDADE, 0L));
        model.addAttribute("totalEmReforma", contagemStatus.getOrDefault(UnidadeStatusOcupacao.EM_REFORMA, 0L));
        
        Stream<Unidade> streamFiltrado = todasUnidadesAutorizadas.stream();

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("Todos")) {
            UnidadeStatusOcupacao statusEnum = UnidadeStatusOcupacao.valueOf(status.toUpperCase());
            streamFiltrado = streamFiltrado.filter(u -> u.getUniStatusOcupacao() == statusEnum);
        }

        if (busca != null && !busca.isBlank()) {
            String buscaLower = busca.toLowerCase();
            streamFiltrado = streamFiltrado.filter(u -> 
                (u.getUniNumero() != null && u.getUniNumero().toLowerCase().contains(buscaLower)) ||
                (u.getBloco() != null && u.getBloco().toLowerCase().contains(buscaLower))
            );
        }

        model.addAttribute("unidades", streamFiltrado.collect(Collectors.toList()));
        model.addAttribute("statusFiltro", status);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("currentPage", "unidades");
        
        long endTime = System.currentTimeMillis();
        logger.info("Página de Unidades carregada em {} ms", (endTime - startTime));
        
        return "unidades";
    }

    @GetMapping("/novo")
    public String getFormularioNovaUnidade(Model model) {
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);

        model.addAttribute("unidadeDTO", new UnidadeRequestDTO());
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
        return "fragments/unidade-form :: form-modal-content";
    }

    @GetMapping("/editar/{id}")
    public String getFormularioEditarUnidade(@PathVariable Integer id, Model model) {
        Unidade unidade = unidadeService.buscarUnidadePorId(id)
                .orElseThrow(() -> new IllegalArgumentException("ID da Unidade inválido:" + id));
        
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        BeanUtils.copyProperties(unidade, dto);
        
        model.addAttribute("unidadeDTO", dto);
        model.addAttribute("unidadeId", id);
        model.addAttribute("tiposUnidade", UnidadeTipo.values());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
        return "fragments/unidade-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    public String salvarUnidade(UnidadeRequestDTO unidadeDTO) {
        unidadeService.cadastrarUnidade(unidadeDTO);
        return "redirect:/unidades";
    }

    @PostMapping("/editar/{id}")
    public String atualizarUnidade(@PathVariable Integer id, UnidadeRequestDTO unidadeDTO) {
        unidadeService.atualizarUnidade(id, unidadeDTO);
        return "redirect:/unidades";
    }

    @PostMapping("/excluir/{id}")
    public String excluirUnidade(@PathVariable Integer id) {
        unidadeService.inativarUnidade(id);
        return "redirect:/unidades";
    }
}