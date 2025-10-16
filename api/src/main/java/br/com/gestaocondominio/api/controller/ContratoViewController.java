package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.ContratoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.ContratoService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contratos")
public class ContratoViewController {

    @Autowired private ContratoService contratoService;
    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private CondominioService condominioService;

    private void carregarDadosPadrao(Model model, Pessoa usuarioLogado) {
        model.addAttribute("nomeUsuarioLogado", usuarioLogado.getPesNome());
        model.addAttribute("currentPage", "contratos");
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
    }

    private void checarPermissaoAcesso(Pessoa usuarioLogado) {
        if (usuarioLogado.getPesIsGlobalAdmin()) {
            return;
        }

        Set<UserRole> roles = usuarioCondominioService.findByPessoa(usuarioLogado)
                .stream()
                .map(UsuarioCondominio::getUscPapel)
                .collect(Collectors.toSet());

        if (Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado");
        }
    }

    @GetMapping
    public String listarContratos(Model model,
                                  @RequestParam(required = false) Integer condominioId,
                                  @RequestParam(required = false) String busca,
                                  @RequestParam(required = false) StatusContrato status) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        checarPermissaoAcesso(usuarioLogado);
        carregarDadosPadrao(model, usuarioLogado);

        List<Contrato> contratos;
        Map<StatusContrato, Long> totais;
        boolean showCondominioInfo = false;

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(true);
            model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
            if (condominiosDisponiveis.size() > 1) {
                showCondominioInfo = true;
            }
            contratos = contratoService.listarContratos(condominioId, busca, status, null, null);
            totais = contratoService.contarContratosPorStatus(condominioId);
            model.addAttribute("condominioFiltro", condominioId);
        } else {
            Integer idCondoUsuario = usuarioCondominioService.findByPessoa(usuarioLogado).stream()
                .findFirst().map(uc -> uc.getCondominio().getConCod()).orElse(null);
            if (idCondoUsuario == null) {
                contratos = Collections.emptyList();
                totais = Collections.emptyMap();
            } else {
                contratos = contratoService.listarContratos(idCondoUsuario, busca, status, null, null);
                totais = contratoService.contarContratosPorStatus(idCondoUsuario);
            }
        }

        model.addAttribute("contratos", contratos);
        model.addAttribute("totalContratos", contratos.stream().count());
        model.addAttribute("totalAtivos", totais.getOrDefault(StatusContrato.ATIVO, 0L));
        model.addAttribute("totalAVencer", totais.getOrDefault(StatusContrato.A_VENCER, 0L));
        model.addAttribute("totalFinalizados", totais.getOrDefault(StatusContrato.FINALIZADO, 0L));
        model.addAttribute("totalRescindidos", totais.getOrDefault(StatusContrato.RESCINDIDO, 0L));
        model.addAttribute("buscaFiltro", busca);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("showCondominioInfo", showCondominioInfo);

        return "contratos";
    }

    @GetMapping("/novo")
    public String mostrarFormNovo(Model model, @RequestParam(required = false) Integer condominioId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        checarPermissaoAcesso(usuarioLogado);
        ContratoRequestDTO contratoDTO = new ContratoRequestDTO();

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            model.addAttribute("condominiosDisponiveis", condominioService.listarTodosCondominios(true));
            contratoDTO.setCondominioId(condominioId);
        } else {
            Integer idCondoUsuario = usuarioCondominioService.findByPessoa(usuarioLogado).stream()
                .findFirst().map(uc -> uc.getCondominio().getConCod())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não associado a um condomínio."));
            contratoDTO.setCondominioId(idCondoUsuario);
        }
        
        model.addAttribute("contrato", contratoDTO);
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
        return "fragments/contrato-form :: contratoForm";
    }

    @PostMapping("/novo")
    public String salvarNovoContrato(ContratoRequestDTO contratoDTO) {
        checarPermissaoAcesso(pessoaService.getLoggedInUser());
        contratoService.criarContrato(contratoDTO.getCondominioId(), contratoDTO);
        return "redirect:/contratos";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormEditar(@PathVariable Long id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        checarPermissaoAcesso(usuarioLogado);
        Contrato contrato = contratoService.obterPorId(id);
        ContratoRequestDTO dto = new ContratoRequestDTO();
        dto.fromEntity(contrato);
        
        model.addAttribute("contrato", dto);
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
        if (usuarioLogado.getPesIsGlobalAdmin()) {
            model.addAttribute("condominiosDisponiveis", condominioService.listarTodosCondominios(true));
        }
        return "fragments/contrato-form :: contratoForm";
    }

    @PostMapping("/editar/{id}")
    public String salvarEdicaoContrato(@PathVariable Long id, ContratoRequestDTO contratoDTO) {
        checarPermissaoAcesso(pessoaService.getLoggedInUser());
        contratoService.atualizarContrato(id, contratoDTO);
        return "redirect:/contratos";
    }

    @PostMapping("/excluir/{id}")
    public String excluirContrato(@PathVariable Long id) {
        checarPermissaoAcesso(pessoaService.getLoggedInUser());
        contratoService.deletarContrato(id);
        return "redirect:/contratos";
    }
}