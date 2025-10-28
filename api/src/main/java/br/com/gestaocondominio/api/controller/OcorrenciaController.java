package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.*;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.service.*;
import br.com.gestaocondominio.api.exception.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityNotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    @Autowired private OcorrenciaService ocorrenciaService;
    @Autowired private PessoaService pessoaService;
    @Autowired private CondominioService condominioService;
    @Autowired private UnidadeService unidadeService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;
    @Autowired private OcupanteRepository ocupanteRepository;

    private void carregarDadosPadrao(Model model, Pessoa usuarioLogado) {
        model.addAttribute("currentPage", "ocorrencias");
        model.addAttribute("tiposOcorrencia", OcorrenciaTipo.values());
        model.addAttribute("statusOcorrencia", OcorrenciaStatus.values());
        model.addAttribute("isGlobalAdmin", usuarioLogado.getPesIsGlobalAdmin());

        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);
        model.addAttribute("isGerencial", isGerencial);

        List<Condominio> condominiosDisponiveis = condominioService.listarTodosCondominios(false);
        model.addAttribute("condominiosDisponiveis", condominiosDisponiveis);
        model.addAttribute("showCondominioInfo", condominiosDisponiveis.size() > 1);
    }

    @GetMapping
    public String listarOcorrencias(
            @RequestParam(required = false) Integer condominioId,
            @RequestParam(required = false) String buscaUnidade,
            @RequestParam(required = false) String buscaTitulo,
            @RequestParam(required = false) OcorrenciaTipo tipo,
            @RequestParam(required = false) OcorrenciaStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioApos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fimAntes,
            @PageableDefault(size = 9, sort = {}) Pageable pageable,
            Model model) {

        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        Page<OcorrenciaResumoDTO> ocorrenciasPage = ocorrenciaService.consultarOcorrencias(
                usuarioLogado, condominioId, buscaUnidade, buscaTitulo, tipo, status, inicioApos, fimAntes, pageable
        );

        Map<String, Long> totais = ocorrenciaService.contarOcorrenciasPorStatusEPeriodo(
                usuarioLogado, condominioId, buscaUnidade, buscaTitulo, tipo, status, inicioApos, fimAntes
        );

        model.addAttribute("ocorrenciasPage", ocorrenciasPage);
        model.addAttribute("totais", totais);

        model.addAttribute("condominioFiltro", condominioId);
        model.addAttribute("buscaUnidadeFiltro", buscaUnidade);
        model.addAttribute("buscaTituloFiltro", buscaTitulo);
        model.addAttribute("tipoFiltro", tipo);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("inicioAposFiltro", inicioApos);
        model.addAttribute("fimAntesFiltro", fimAntes);

        List<Unidade> unidadesDisponiveisParaFiltro = obterUnidadesParaFiltro(usuarioLogado, condominioId);
        model.addAttribute("unidadesDisponiveis", unidadesDisponiveisParaFiltro);

        return "ocorrencias";
    }

    @GetMapping("/novo")
    public String getFormNovaOcorrencia(Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);

        OcorrenciaRequestDTO dto = new OcorrenciaRequestDTO();
        List<Unidade> unidadesSelecionaveis = obterUnidadesParaCadastro(usuarioLogado);

        model.addAttribute("ocorrenciaRequestDTO", dto);
        model.addAttribute("unidadesSelecionaveis", unidadesSelecionaveis);
        model.addAttribute("isCadastro", true);

        return "fragments/ocorrencia-form :: form-modal-content";
    }

    @PostMapping("/novo")
    @ResponseBody
    public ResponseEntity<?> criarOcorrencia(@Valid @ModelAttribute OcorrenciaRequestDTO dto, BindingResult bindingResult) {
         if (bindingResult.hasErrors()) {
             String errors = bindingResult.getAllErrors().stream()
                 .map(e -> e.getDefaultMessage())
                 .collect(Collectors.joining(", "));
             return ResponseEntity.badRequest().body(Map.of("message", errors));
         }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            Ocorrencia novaOcorrencia = ocorrenciaService.criarOcorrencia(dto, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(new OcorrenciaResumoDTO(novaOcorrencia));
        } catch (EntityNotFoundException | ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao criar ocorrência."));
        }
    }

    @GetMapping("/detalhes/{id}")
    public String getDetalhesOcorrencia(@PathVariable Integer id, Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        carregarDadosPadrao(model, usuarioLogado);
        try {
            OcorrenciaDetalhesDTO detalhesDTO = ocorrenciaService.buscarPorIdDetalhes(id, usuarioLogado);
            model.addAttribute("ocorrencia", detalhesDTO);
            model.addAttribute("comentarioRequestDTO", new OcorrenciaComentarioRequestDTO());
            model.addAttribute("finalizarRequestDTO", new OcorrenciaFinalizarRequestDTO());
            return "fragments/ocorrencia-detalhes :: detalhes-modal-content";
        } catch (EntityNotFoundException e) {
             model.addAttribute("errorMessage", "Ocorrência não encontrada.");
             return "fragments/modal-error :: error-modal-content";
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getReason());
            return "fragments/modal-error :: error-modal-content";
        }
    }

    @PostMapping("/{id}/comentar")
    @ResponseBody
    public ResponseEntity<?> adicionarComentario(@PathVariable Integer id, @Valid @ModelAttribute OcorrenciaComentarioRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
             String errors = bindingResult.getAllErrors().stream()
                 .map(e -> e.getDefaultMessage())
                 .collect(Collectors.joining(", "));
             return ResponseEntity.badRequest().body(Map.of("message", errors));
         }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            OcorrenciaComentarioDTO novoComentario = ocorrenciaService.adicionarComentario(id, dto, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoComentario);
        } catch (EntityNotFoundException | ResponseStatusException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro ao adicionar comentário."));
        }
    }

     @PostMapping("/{id}/anexar")
     @ResponseBody
     public ResponseEntity<?> adicionarAnexo(@PathVariable Integer id, @RequestParam("anexoFile") MultipartFile anexo) {
         try {
             Pessoa usuarioLogado = pessoaService.getLoggedInUser();
             OcorrenciaAnexoDTO novoAnexo = ocorrenciaService.adicionarAnexo(id, anexo, usuarioLogado);
             return ResponseEntity.status(HttpStatus.CREATED).body(novoAnexo);
         } catch (IllegalArgumentException | EntityNotFoundException | ResponseStatusException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
         } catch (StorageException e) {
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro ao salvar o arquivo: " + e.getMessage()));
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao adicionar anexo."));
         }
     }

     @PostMapping("/{ocorrenciaId}/anexo/{anexoId}/excluir")
     @ResponseBody
     public ResponseEntity<?> excluirAnexo(@PathVariable Integer ocorrenciaId, @PathVariable Integer anexoId) {
         try {
             Pessoa usuarioLogado = pessoaService.getLoggedInUser();
             ocorrenciaService.excluirAnexo(ocorrenciaId, anexoId, usuarioLogado);
             return ResponseEntity.ok(Map.of("message", "Anexo excluído com sucesso."));
         } catch (EntityNotFoundException | ResponseStatusException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro ao excluir anexo."));
         }
     }

     @GetMapping("/{ocorrenciaId}/anexo/{anexoId}")
     @ResponseBody
     public ResponseEntity<Resource> baixarAnexo(@PathVariable Integer ocorrenciaId, @PathVariable Integer anexoId, HttpServletRequest request) {
         try {
             Pessoa usuarioLogado = pessoaService.getLoggedInUser();
             Resource resource = ocorrenciaService.carregarAnexoComoRecurso(ocorrenciaId, anexoId, usuarioLogado);
             String nomeOriginal = ocorrenciaService.getNomeOriginalAnexo(ocorrenciaId, anexoId, usuarioLogado);

             String contentType = "application/octet-stream";
             try {
                 contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                 if (contentType == null) {
                     contentType = "application/octet-stream";
                 }
             } catch (IOException ex) {
             }

             return ResponseEntity.ok()
                     .contentType(MediaType.parseMediaType(contentType))
                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeOriginal + "\"")
                     .body(resource);
         } catch (EntityNotFoundException | ResponseStatusException e) {
              throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
         } catch (Exception e) {
              throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao baixar anexo.");
         }
     }

    @PostMapping("/{id}/finalizar")
    @ResponseBody
    public ResponseEntity<?> finalizarOcorrencia(@PathVariable Integer id, @Valid @ModelAttribute OcorrenciaFinalizarRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
             String errors = bindingResult.getAllErrors().stream()
                 .map(e -> e.getDefaultMessage())
                 .collect(Collectors.joining(", "));
             return ResponseEntity.badRequest().body(Map.of("message", errors));
         }
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();
            ocorrenciaService.finalizarOcorrencia(id, dto, usuarioLogado);
            return ResponseEntity.ok(Map.of("message", "Ocorrência finalizada com sucesso."));
        } catch (EntityNotFoundException | ResponseStatusException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro ao finalizar ocorrência."));
        }
    }

    private List<Unidade> obterUnidadesParaFiltro(Pessoa usuarioLogado, Integer condominioIdSelecionado) {
         if (usuarioLogado.getPesIsGlobalAdmin()) {
             if (condominioIdSelecionado != null) {
                 return unidadeService.findByCondominioId(condominioIdSelecionado);
             } else {
                 return unidadeService.listarTodasUnidades(false, null, null);
             }
         } else if (usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            Integer userCondoId = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            if (userCondoId != null) {
                 return unidadeService.findByCondominioId(userCondoId);
            }
         } else {
             return ocupanteRepository.findByPessoa(usuarioLogado).stream()
                     .map(Ocupante::getUnidade)
                     .filter(Objects::nonNull)
                     .distinct()
                     .collect(Collectors.toList());
         }
         return Collections.emptyList();
    }

     private List<Unidade> obterUnidadesParaCadastro(Pessoa usuarioLogado) {
          if (usuarioLogado.getPesIsGlobalAdmin()) {
               return unidadeService.listarTodasUnidades(false, null, null);
          } else if (usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
              Integer userCondoId = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
               if (userCondoId != null) {
                   return unidadeService.findByCondominioId(userCondoId);
               }
          } else {
               return ocupanteRepository.findByPessoa(usuarioLogado).stream()
                       .map(Ocupante::getUnidade)
                       .filter(Objects::nonNull)
                       .distinct()
                       .collect(Collectors.toList());
          }
          return Collections.emptyList();
     }
}