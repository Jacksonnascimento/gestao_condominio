package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.domain.service.ComunicadoService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.exception.StorageException;
import org.springframework.core.io.Resource;
import br.com.gestaocondominio.api.domain.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/comunicados")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;
    private final PessoaService pessoaService;
    private final CondominioRepository condominioRepository;
    private final FileStorageService fileStorageService;
    private final UsuarioCondominioRepository usuarioCondominioRepository;

    public ComunicadoController(ComunicadoService comunicadoService,
                                PessoaService pessoaService,
                                CondominioRepository condominioRepository,
                                FileStorageService fileStorageService,
                                UsuarioCondominioRepository usuarioCondominioRepository) {
        this.comunicadoService = comunicadoService;
        this.pessoaService = pessoaService;
        this.condominioRepository = condominioRepository;
        this.fileStorageService = fileStorageService;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
    }

    @GetMapping
    public String getComunicadosPage(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String mensagem,
            @RequestParam(required = false) String publicoDestino,
            @RequestParam(required = false) Boolean isUrgente,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        Pessoa pessoaLogada = pessoaService.getLoggedInUser();
        boolean isGlobalAdmin = pessoaLogada.getPesIsGlobalAdmin();

        Page<Comunicado> comunicadosPage = comunicadoService.consultar(
                titulo, mensagem, publicoDestino, isUrgente, pageable
        );

        model.addAttribute("comunicadosPage", comunicadosPage);
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);

        boolean usuarioPodeGerenciar;
        if (isGlobalAdmin) {
            usuarioPodeGerenciar = true;
        } else {
            List<UsuarioCondominio> associacoes = usuarioCondominioRepository.findByPesCod(pessoaLogada.getPesCod());
            usuarioPodeGerenciar = associacoes.stream()
                    .anyMatch(uc -> uc.getUscPapel() == UserRole.ADMIN ||
                                  uc.getUscPapel() == UserRole.SINDICO);
        }
        model.addAttribute("usuarioPodeGerenciar", usuarioPodeGerenciar);

        if (isGlobalAdmin) {
            List<Condominio> allCondominios = condominioRepository.findAll();
            model.addAttribute("allCondominios", allCondominios);
        }

        model.addAttribute("filtroTitulo", titulo);
        model.addAttribute("filtroMensagem", mensagem);
        model.addAttribute("filtroPublico", publicoDestino);
        model.addAttribute("filtroUrgente", isUrgente);

        return "comunicados";
    }

    @GetMapping("/novo")
    public String getNovoComunicadoForm(Model model) {
        Pessoa pessoaLogada = pessoaService.getLoggedInUser();
        boolean isGlobalAdmin = pessoaLogada.getPesIsGlobalAdmin();

        model.addAttribute("comunicadoDto", new ComunicadoRequestDTO());
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);

        if (isGlobalAdmin) {
            model.addAttribute("allCondominios", condominioRepository.findAll());
        }

        return "fragments/comunicado-form :: comunicado-form-content";
    }

    @GetMapping("/editar/{id}")
    public String getEditarComunicadoForm(@PathVariable Integer id, Model model) {
        Pessoa pessoaLogada = pessoaService.getLoggedInUser();
        boolean isGlobalAdmin = pessoaLogada.getPesIsGlobalAdmin();

        Comunicado comunicado = comunicadoService.getComunicadoById(id);
        ComunicadoRequestDTO dto = new ComunicadoRequestDTO(comunicado);

        model.addAttribute("comunicadoDto", dto);
        model.addAttribute("isGlobalAdmin", isGlobalAdmin);

        if (isGlobalAdmin) {
            model.addAttribute("allCondominios", condominioRepository.findAll());
        }

        return "fragments/comunicado-form :: comunicado-form-content";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> criarComunicado(
            @RequestPart("comunicado") ComunicadoRequestDTO dto,
            @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        try {
            comunicadoService.criar(dto, anexo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<?> atualizarComunicado(
            @PathVariable Integer id,
            @RequestPart("comunicado") ComunicadoRequestDTO dto,
            @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        try {
            comunicadoService.atualizar(id, dto, anexo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/excluir/{id}")
    @ResponseBody
    public ResponseEntity<?> excluirComunicado(@PathVariable Integer id) {
        try {
            comunicadoService.excluir(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/anexo/{id}")
    @ResponseBody
    public ResponseEntity<?> getAnexo(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Comunicado comunicado = comunicadoService.getComunicadoById(id);
            if (comunicado.getCaminhoAnexo() == null || comunicado.getCaminhoAnexo().isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Este comunicado não possui anexo.");
            }

            String filename = Paths.get(comunicado.getCaminhoAnexo()).getFileName().toString();
            Resource file = fileStorageService.loadAsResource(filename, "comunicados");

            String contentType = "application/octet-stream";
            try {
                contentType = request.getServletContext().getMimeType(file.getFile().getAbsolutePath());
            } catch (IOException ex) {
                // Log do erro se necessário, mas mantém o tipo padrão como fallback
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .body(file);

        } catch (StorageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Arquivo não encontrado no servidor. Pode ter sido excluído ou movido.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ocorreu um erro inesperado ao tentar acessar o anexo.");
        }
    }
}