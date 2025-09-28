package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.repository.UnidadeTipoRepository;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/unidades")
public class UnidadeViewController {

    @Autowired
    private UnidadeService unidadeService;

    @Autowired
    private UnidadeTipoRepository unidadeTipoRepository;


    @GetMapping
    public String getUnidadesPage(
            @RequestParam(required = false, defaultValue = "Todos") String status,
            @RequestParam(required = false, defaultValue = "") String busca,
            Model model) {

        List<Unidade> todasUnidadesAtivas = unidadeService.listarTodasUnidades(false, "Todos", null);
        
        Map<UnidadeStatusOcupacao, Long> contagemStatus = todasUnidadesAtivas.stream()
                .filter(u -> u.getUniStatusOcupacao() != null)
                .collect(Collectors.groupingBy(Unidade::getUniStatusOcupacao, Collectors.counting()));

        model.addAttribute("totalUnidades", todasUnidadesAtivas.size());
        model.addAttribute("totalOcupadas", contagemStatus.getOrDefault(UnidadeStatusOcupacao.OCUPADA, 0L));
        model.addAttribute("totalVazias", contagemStatus.getOrDefault(UnidadeStatusOcupacao.VAZIA, 0L));
        model.addAttribute("totalMultipropriedade", contagemStatus.getOrDefault(UnidadeStatusOcupacao.MULTIPROPRIEDADE, 0L));
        model.addAttribute("totalEmReforma", contagemStatus.getOrDefault(UnidadeStatusOcupacao.EM_REFORMA, 0L));
        
        List<Unidade> unidadesFiltradas = unidadeService.listarTodasUnidades(false, status, busca);

        model.addAttribute("unidades", unidadesFiltradas);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("buscaFiltro", busca);
        
        return "unidades";
    }

    @GetMapping("/novo")
    public String getFormularioNovaUnidade(Model model) {
        model.addAttribute("unidadeDTO", new UnidadeRequestDTO());
        model.addAttribute("tiposUnidade", unidadeTipoRepository.findAll());
        model.addAttribute("statusOcupacao", UnidadeStatusOcupacao.values());
        return "fragments/unidade-form :: form-modal-content";
    }

    @GetMapping("/editar/{id}")
    public String getFormularioEditarUnidade(@PathVariable Integer id, Model model) {
        Unidade unidade = unidadeService.buscarUnidadePorId(id)
                .orElseThrow(() -> new IllegalArgumentException("ID da Unidade inválido:" + id));
        
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        BeanUtils.copyProperties(unidade, dto);
        if (unidade.getUnidadeTipo() != null) {
            dto.setUtiCod(unidade.getUnidadeTipo().getUtiCod());
        }
        
        model.addAttribute("unidadeDTO", dto);
        model.addAttribute("unidadeId", id);
        model.addAttribute("tiposUnidade", unidadeTipoRepository.findAll());
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