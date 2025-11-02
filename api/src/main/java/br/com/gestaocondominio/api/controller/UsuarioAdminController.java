package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioDTO;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_SINDICO', 'ROLE_ADMIN')")
public class UsuarioAdminController {

    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private CondominioService condominioService;
    @Autowired private OcupanteService ocupanteService;
    @Autowired private PasswordResetService passwordResetService;

    private void carregarDadosPadrao(Model model, Pessoa usuarioLogado) {
        model.addAttribute("currentPage", "usuarios");
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());
        
        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        model.addAttribute("showCondominioInfo", condominiosDisponiveis.size() > 1);

        model.addAttribute("rolesDisponiveis", List.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.MORADOR, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO));
    }

    @GetMapping
    public String getPaginaAdminUsuarios(Model model, @RequestParam(required = false) Integer condominioId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        List<UsuarioCondominioDTO> usuarios;
        Integer idCondominioParaFiltrar = condominioId;

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            if (idCondominioParaFiltrar == null && !model.containsAttribute("showCondominioInfo")) {
                idCondominioParaFiltrar = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            }
        } else {
            idCondominioParaFiltrar = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        }

        if (idCondominioParaFiltrar != null) {
            final Integer finalCondoId = idCondominioParaFiltrar;
            usuarios = usuarioCondominioService.listarTodosUsuariosCondominio(true).stream()
                    .filter(uc -> uc.getCondominio().getConCod().equals(finalCondoId))
                    .map(UsuarioCondominioDTO::new)
                    .collect(Collectors.toList());
        } else {
            usuarios = Collections.emptyList();
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("condominioFiltro", idCondominioParaFiltrar);
        return "usuarios";
    }

    @GetMapping("/novo")
    public String getFormNovoUsuario(Model model, @RequestParam(required = false) Integer condominioId) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        UsuarioCondominioRequestDTO dto = new UsuarioCondominioRequestDTO();
        
        Integer idCondominioParaFiltrar;
        if (usuarioLogado.getPesIsGlobalAdmin()) {
            idCondominioParaFiltrar = condominioId;
        } else {
            idCondominioParaFiltrar = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        }

        if(idCondominioParaFiltrar != null) {
            dto.setCondominioId(idCondominioParaFiltrar);
            List<Ocupante> ocupantes = ocupanteService.findOcupantesSemLoginMoradorByCondominio(idCondominioParaFiltrar);
            model.addAttribute("ocupantesSemLogin", ocupantes.stream().map(OcupanteResponseDTO::new).collect(Collectors.toList()));
        } else {
             model.addAttribute("ocupantesSemLogin", Collections.emptyList());
        }
        
        model.addAttribute("usuarioRequestDTO", dto);
        return "fragments/usuario-form :: form-modal-content";
    }

    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<?> salvarUsuario(@ModelAttribute UsuarioCondominioRequestDTO dto) {
        try {
            Pessoa pessoaParaVincular;

            if (dto.getPessoaId() != null) {
                pessoaParaVincular = pessoaService.buscarPessoaPorId(dto.getPessoaId())
                        .orElseThrow(() -> new IllegalArgumentException("Pessoa (Ocupante) não encontrada."));
            } else {
                Pessoa novaPessoa = new Pessoa();
                novaPessoa.setPesNome(dto.getPesNome());
                novaPessoa.setPesCpfCnpj(dto.getPesCpfCnpj());
                novaPessoa.setPesEmail(dto.getPesEmail());
                novaPessoa.setPesTelefone(dto.getPesTelefone());
                novaPessoa.setPesTipo('F'); // Default
                
                if("CRIAR_SENHA".equals(dto.getAcaoSenha())) {
                    novaPessoa.setPesSenhaLogin(dto.getPesSenhaLogin());
                }
                
                pessoaParaVincular = pessoaService.cadastrarPessoa(novaPessoa);
            }

            Condominio condominio = condominioService.buscarCondominioPorId(dto.getCondominioId())
                    .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado."));

            UsuarioCondominio novoVinculo = new UsuarioCondominio();
            novoVinculo.setPessoa(pessoaParaVincular);
            novoVinculo.setCondominio(condominio);
            novoVinculo.setUscPapel(dto.getPapel());

            UsuarioCondominio salvo = usuarioCondominioService.cadastrarUsuarioCondominio(novoVinculo);

            if ("ENVIAR_LINK".equals(dto.getAcaoSenha())) {
                passwordResetService.createPasswordResetToken(pessoaParaVincular.getPesEmail(), 24); // 24 Horas
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioCondominioDTO(salvo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno: " + e.getMessage()));
        }
    }

    @PostMapping("/enviar-link-reset")
    @ResponseBody
    public ResponseEntity<?> enviarLinkReset(@RequestParam Integer pessoaId) {
        try {
            Pessoa pessoa = pessoaService.buscarPessoaPorId(pessoaId)
                    .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada."));
            
            passwordResetService.createPasswordResetToken(pessoa.getPesEmail(), 24); // 24 Horas
            return ResponseEntity.ok(Map.of("message", "Link de redefinição enviado para " + pessoa.getPesEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/excluir-vinculo")
    @ResponseBody
    public ResponseEntity<?> excluirVinculo(@RequestParam Integer pessoaId, @RequestParam Integer condominioId, @RequestParam UserRole papel) {
         try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            if(usuarioLogado.getPesCod().equals(pessoaId)) {
                 throw new IllegalArgumentException("Não é possível excluir o próprio vínculo de acesso.");
            }
            usuarioCondominioService.deletarUsuarioCondominio(new br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId(pessoaId, condominioId, papel));
            return ResponseEntity.ok(Map.of("message", "Vínculo removido com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}