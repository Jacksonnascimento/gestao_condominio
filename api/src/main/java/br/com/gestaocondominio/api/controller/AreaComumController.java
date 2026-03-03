package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.controller.dto.AreaComumTurnoDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.AreaComumService;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/areas-comuns")
@RequiredArgsConstructor
public class AreaComumController {

    private final AreaComumService areaComumService;
    private final CondominioService condominioService;
    private final PessoaService pessoaService;

    @GetMapping
    public String listarAreasComuns(@RequestParam(required = false) Integer conCod, 
                                    @RequestParam(required = false) String busca, 
                                    Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);

        Integer condominioIdFiltro = conCod;
        if (condominioIdFiltro == null && condominiosDisponiveis.size() == 1) {
            condominioIdFiltro = condominiosDisponiveis.get(0).getConCod();
        }

        List<AreaComum> areas = (condominioIdFiltro != null) ? areaComumService.listarPorCondominio(condominioIdFiltro) : List.of();
        
        long totalAreas = areas.size();
        long areasAtivas = areas.stream().filter(AreaComum::getAtiva).count();
        long areasInativas = totalAreas - areasAtivas;

        if (busca != null && !busca.trim().isEmpty()) {
            String termo = busca.toLowerCase();
            areas = areas.stream()
                .filter(a -> a.getNome().toLowerCase().contains(termo) || 
                            (a.getDescricao() != null && a.getDescricao().toLowerCase().contains(termo)))
                .collect(Collectors.toList());
        }

        model.addAttribute("areas", areas);
        model.addAttribute("condominios", condominiosDisponiveis);
        model.addAttribute("conCodSelecionado", condominioIdFiltro);
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("currentPage", "areas-comuns");
        model.addAttribute("showCondominioInfo", Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) && condominiosDisponiveis.size() > 1);
        
        model.addAttribute("totalAreas", totalAreas);
        model.addAttribute("totalAtivas", areasAtivas);
        model.addAttribute("totalInativas", areasInativas);

        return "areas-comuns";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<AreaComumRequestDTO> buscarAreaPorId(@PathVariable Integer id) {
        AreaComum area = areaComumService.buscarPorId(id);
        AreaComumRequestDTO dto = new AreaComumRequestDTO();
        dto.setAreCod(area.getAreCod());
        dto.setConCod(area.getCondominio().getConCod());
        dto.setNome(area.getNome());
        dto.setDescricao(area.getDescricao());
        dto.setTermosUso(area.getTermosUso());
        dto.setCapacidadeMaxima(area.getCapacidadeMaxima());
        dto.setPermiteConvidados(area.getPermiteConvidados());
        dto.setLimiteConvidados(area.getLimiteConvidados());
        dto.setTaxaValor(area.getTaxaValor());
        dto.setDiasAntecedenciaMin(area.getDiasAntecedenciaMin());
        dto.setDiasAntecedenciaMax(area.getDiasAntecedenciaMax());
        dto.setAtiva(area.getAtiva());
        
        if (area.getTurnos() != null) {
            dto.setTurnos(area.getTurnos().stream().map(t -> {
                AreaComumTurnoDTO tdto = new AreaComumTurnoDTO();
                tdto.setTurCod(t.getTurCod());
                tdto.setNome(t.getNome());
                tdto.setHoraInicio(t.getHoraInicio());
                tdto.setHoraFim(t.getHoraFim());
                tdto.setAtivo(t.getAtivo());
                return tdto;
            }).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/salvar")
    public Object salvarAreaComum(@RequestBody AreaComumRequestDTO dto, Model model) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            AreaComum areaSalva = areaComumService.salvar(dto);
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
            
            model.addAttribute("area", areaSalva);
            model.addAttribute("showCondominioInfo", Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) && condominiosDisponiveis.size() > 1);
            return "fragments/area-comum-card :: card";
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/excluir")
    public Object excluirAreaComum(@PathVariable Integer id) {
        try {
            areaComumService.excluir(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}