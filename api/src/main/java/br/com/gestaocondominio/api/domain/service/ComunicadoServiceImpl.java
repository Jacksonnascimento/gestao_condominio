package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoResposta;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.exception.StorageException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComunicadoServiceImpl implements ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final CondominioRepository condominioRepository;
    private final FileStorageService fileStorageService;
    private final UsuarioCondominioService usuarioCondominioService;
    private final ComunicadoLeituraRepository comunicadoLeituraRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final OcupanteRepository ocupanteRepository;

    private static final String COMUNICADOS_DIR = "comunicados";
    private static final Set<PublicoDestino> TODOS_OS_PUBLICOS = Set.of(PublicoDestino.values());
    private static final int TAMANHO_DO_NOME_DO_ANEXO = 255;

    public ComunicadoServiceImpl(ComunicadoRepository comunicadoRepository,
                                 CondominioRepository condominioRepository,
                                 FileStorageService fileStorageService,
                                 UsuarioCondominioService usuarioCondominioService,
                                 ComunicadoLeituraRepository comunicadoLeituraRepository,
                                 UsuarioCondominioRepository usuarioCondominioRepository,
                                 OcupanteRepository ocupanteRepository) {
        this.comunicadoRepository = comunicadoRepository;
        this.condominioRepository = condominioRepository;
        this.fileStorageService = fileStorageService;
        this.usuarioCondominioService = usuarioCondominioService;
        this.comunicadoLeituraRepository = comunicadoLeituraRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.ocupanteRepository = ocupanteRepository;
    }

    /** O que a pessoa vê num condomínio em que tem vínculo ativo: se publica nele e os públicos que enxerga. */
    private record Alcance(Integer condominio, boolean gestor, Set<PublicoDestino> publicos) {
    }

    /**
     * Alcance da pessoa no condomínio, ou nulo se ela não tem vínculo ativo nele. Síndico e administração veem todos
     * os públicos e publicam; o morador vê os destinados a todos e ao seu tipo de vínculo com a unidade, e os
     * funcionários, os destinados aos funcionários.
     */
    private Alcance alcanceNo(Pessoa pessoa, Integer conCod) {
        if (conCod == null) {
            return null;
        }
        Set<UserRole> papeis = usuarioCondominioRepository.findByPesCod(pessoa.getPesCod()).stream()
                .filter(uc -> uc.getConCod().equals(conCod) && Boolean.TRUE.equals(uc.getUscAtivoAssociacao()))
                .map(UsuarioCondominio::getUscPapel)
                .collect(Collectors.toSet());
        if (papeis.isEmpty()) {
            return null;
        }
        if (papeis.contains(UserRole.ADMIN) || papeis.contains(UserRole.SINDICO)) {
            return new Alcance(conCod, true, TODOS_OS_PUBLICOS);
        }
        Set<PublicoDestino> publicos = EnumSet.of(PublicoDestino.TODOS);
        if (papeis.contains(UserRole.FUNCIONARIO_ADM) || papeis.contains(UserRole.PORTEIRO)) {
            publicos.add(PublicoDestino.FUNCIONARIOS);
        }
        Set<OcupanteVinculo> vinculos = ocupanteRepository.findByPessoa(pessoa).stream()
                .filter(oc -> oc.getUnidade() != null && oc.getUnidade().getCondominio() != null
                        && oc.getUnidade().getCondominio().getConCod().equals(conCod))
                .map(Ocupante::getOcuVinculo)
                .collect(Collectors.toSet());
        if (vinculos.contains(OcupanteVinculo.PROPRIETARIO)) {
            publicos.add(PublicoDestino.PROPRIETARIOS);
        }
        if (vinculos.contains(OcupanteVinculo.LOCATARIO)) {
            publicos.add(PublicoDestino.INQUILINOS);
        }
        return new Alcance(conCod, false, publicos);
    }

    /**
     * Alcance da pessoa no condomínio escolhido na tela. Sem escolha, vale um dos condomínios dela, e nulo quando ela
     * não tem nenhum; com escolha, o condomínio precisa ser dela.
     */
    private Alcance alcanceDaConsulta(Pessoa pessoa, Integer condominioId) {
        if (condominioId == null) {
            return alcanceNo(pessoa, usuarioCondominioService.getCondominioIdDoUsuario(pessoa));
        }
        Alcance alcance = alcanceNo(pessoa, condominioId);
        if (alcance == null) {
            throw new AccessDeniedException("Você não tem acesso a este condomínio.");
        }
        return alcance;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComunicadoResposta> consultarVisiveis(Pessoa usuarioLogado, Integer condominioId, String titulo,
                                                      String mensagem, PublicoDestino publicoDestino,
                                                      Boolean urgente, Pageable pageable) {
        String publico = publicoDestino == null ? null : publicoDestino.name();
        if (administradorGeral(usuarioLogado)) {
            // O administrador geral vê todos; com um condomínio escolhido, só os destinados a ele
            return comunicadoRepository.findAll(ComunicadoSpecification.filtrar(usuarioLogado, condominioId,
                            TODOS_OS_PUBLICOS, true, titulo, mensagem, publico, urgente), pageable)
                    .map(comunicado -> ComunicadoResposta.de(comunicado, true, true));
        }
        Alcance alcance = alcanceDaConsulta(usuarioLogado, condominioId);
        if (alcance == null) {
            return Page.empty(pageable);
        }
        Integer gerido = alcance.gestor() ? alcance.condominio() : null;
        return comunicadoRepository.findAll(ComunicadoSpecification.filtrar(usuarioLogado, alcance.condominio(),
                        alcance.publicos(), alcance.gestor(), titulo, mensagem, publico, urgente), pageable)
                .map(comunicado -> ComunicadoResposta.de(comunicado, false, publicadoSoPara(comunicado, gerido)));
    }

    @Override
    @Transactional(readOnly = true)
    public ComunicadoResposta buscarVisivel(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = buscarVisivelOuFalhar(id, usuarioLogado);
        return ComunicadoResposta.de(comunicado, administradorGeral(usuarioLogado),
                podeGerenciar(usuarioLogado, comunicado));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean podeGerenciar(Pessoa usuarioLogado, Integer condominioId) {
        if (administradorGeral(usuarioLogado)) {
            return true;
        }
        Integer conCod = condominioId != null ? condominioId
                : usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        Alcance alcance = alcanceNo(usuarioLogado, conCod);
        return alcance != null && alcance.gestor();
    }

    @Override
    @Transactional
    public ComunicadoResposta criar(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado,
                                    Integer condominioId) {
        Set<Condominio> destinos;
        if (administradorGeral(usuarioLogado)) {
            destinos = condominiosEscolhidos(dto.getCondominioIds());
        } else {
            Alcance alcance = alcanceDaConsulta(usuarioLogado, condominioId);
            if (alcance == null || !alcance.gestor()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Apenas o síndico ou a administração do condomínio podem publicar comunicados.");
            }
            destinos = new HashSet<>(Set.of(condominioRepository.findById(alcance.condominio())
                    .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."))));
        }
        Comunicado comunicado = gravarNovo(dto, anexo, usuarioLogado, destinos);
        return ComunicadoResposta.de(comunicado, administradorGeral(usuarioLogado), true);
    }

    @Override
    @Transactional
    public ComunicadoResposta atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo,
                                        Pessoa usuarioLogado) {
        Comunicado comunicado = buscarParaGerenciar(id, usuarioLogado);
        // Síndico e administração não mudam o destino: o comunicado continua no condomínio deles
        Set<Condominio> destinos = administradorGeral(usuarioLogado)
                ? condominiosEscolhidos(dto.getCondominioIds())
                : new HashSet<>(comunicado.getCondominios());
        Comunicado salvo = gravarAlteracao(comunicado, dto, anexo, destinos);
        return ComunicadoResposta.de(salvo, administradorGeral(usuarioLogado), true);
    }

    @Override
    @Transactional
    public void excluir(Integer id, Pessoa usuarioLogado) {
        apagar(buscarParaGerenciar(id, usuarioLogado));
    }

    @Override
    @Transactional(readOnly = true)
    public AnexoDoComunicado carregarAnexo(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = buscarVisivelOuFalhar(id, usuarioLogado);
        if (comunicado.getCaminhoAnexo() == null || comunicado.getCaminhoAnexo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este comunicado não possui anexo.");
        }
        String nomeDoArquivo = Paths.get(comunicado.getCaminhoAnexo()).getFileName().toString();
        try {
            return new AnexoDoComunicado(fileStorageService.loadAsResource(nomeDoArquivo, COMUNICADOS_DIR),
                    ComunicadoDTOs.nomeDoAnexo(comunicado));
        } catch (StorageException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Arquivo não encontrado no servidor. Pode ter sido excluído ou movido.");
        }
    }

    /**
     * O comunicado, se quem está logado o vê em algum dos condomínios de destino, pela mesma regra da listagem
     * (vínculo ativo no condomínio e público que a pessoa enxerga).
     */
    private Comunicado buscarVisivelOuFalhar(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado."));
        if (administradorGeral(usuarioLogado)) {
            return comunicado;
        }
        boolean visivel = comunicado.getCondominios() != null && comunicado.getCondominios().stream()
                .map(condominio -> alcanceNo(usuarioLogado, condominio.getConCod()))
                .anyMatch(alcance -> alcance != null && alcance.publicos().contains(comunicado.getPublicoDestino()));
        if (!visivel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado. Você não tem permissão para ver este comunicado.");
        }
        return comunicado;
    }

    /**
     * O comunicado, se quem está logado pode alterá-lo: o administrador geral altera qualquer um; o síndico e a
     * administração, os publicados só para o condomínio deles. Um comunicado que o administrador geral mandou para
     * vários condomínios só ele altera, porque salvar pela tela do condomínio o tiraria dos outros.
     */
    private Comunicado buscarParaGerenciar(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado."));
        if (!podeGerenciar(usuarioLogado, comunicado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado. Você não tem permissão para alterar este comunicado.");
        }
        return comunicado;
    }

    private boolean podeGerenciar(Pessoa usuarioLogado, Comunicado comunicado) {
        if (administradorGeral(usuarioLogado)) {
            return true;
        }
        Alcance alcance = alcanceNo(usuarioLogado, destinoUnico(comunicado));
        return alcance != null && alcance.gestor();
    }

    /** Se o comunicado foi publicado só para o condomínio {@code conCod} (nulo nunca é). */
    private static boolean publicadoSoPara(Comunicado comunicado, Integer conCod) {
        return conCod != null && conCod.equals(destinoUnico(comunicado));
    }

    /** O condomínio de destino, quando o comunicado foi publicado para um só. */
    private static Integer destinoUnico(Comunicado comunicado) {
        if (comunicado.getCondominios() == null || comunicado.getCondominios().size() != 1) {
            return null;
        }
        return comunicado.getCondominios().iterator().next().getConCod();
    }

    private static boolean administradorGeral(Pessoa pessoa) {
        return Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin());
    }

    /** Condomínios que o administrador geral escolheu: ao menos um, e todos precisam existir. */
    private Set<Condominio> condominiosEscolhidos(List<Integer> condominioIds) {
        if (condominioIds == null || condominioIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um condomínio.");
        }
        Set<Integer> pedidos = new HashSet<>(condominioIds);
        if (pedidos.contains(null)) {
            throw new IllegalArgumentException("Um dos condomínios selecionados não foi encontrado.");
        }
        List<Condominio> encontrados = condominioRepository.findAllById(pedidos);
        if (encontrados.size() != pedidos.size()) {
            throw new IllegalArgumentException("Um dos condomínios selecionados não foi encontrado.");
        }
        return new HashSet<>(encontrados);
    }

    // ---- Gravação ----

    private Comunicado gravarNovo(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa criador,
                                  Set<Condominio> condominiosAlvo) {
        String caminhoAnexo = null;
        try {
            if (anexo != null) {
                if (anexo.isEmpty()) {
                    throw new IllegalArgumentException("Não é possível anexar um arquivo vazio.");
                }
                caminhoAnexo = fileStorageService.store(anexo, COMUNICADOS_DIR);
            }

            Comunicado comunicado = Comunicado.builder()
                    .titulo(dto.getTitulo())
                    .mensagem(dto.getMensagem())
                    .publicoDestino(dto.getPublicoDestino())
                    .isUrgente(dto.getIsUrgente())
                    .caminhoAnexo(caminhoAnexo)
                    .nomeAnexo(caminhoAnexo == null ? null : nomeOriginal(anexo))
                    .criador(criador)
                    .condominios(condominiosAlvo)
                    .build();

            return comunicadoRepository.save(comunicado);

        } catch (RuntimeException e) {
            if (caminhoAnexo != null) {
                String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw e;
        }
    }

    /** Um anexo novo substitui o antigo, que é apagado depois de salvar. */
    private Comunicado gravarAlteracao(Comunicado comunicado, ComunicadoRequestDTO dto, MultipartFile anexo,
                                       Set<Condominio> condominiosAlvo) {
        String novoCaminhoAnexo = null;
        String antigoCaminhoAnexo = comunicado.getCaminhoAnexo();

        try {
            if (anexo != null) {
                if (anexo.isEmpty()) {
                    throw new IllegalArgumentException("Não é possível anexar um arquivo vazio.");
                }
                novoCaminhoAnexo = fileStorageService.store(anexo, COMUNICADOS_DIR);
                comunicado.setCaminhoAnexo(novoCaminhoAnexo);
                comunicado.setNomeAnexo(nomeOriginal(anexo));
            }

            comunicado.setTitulo(dto.getTitulo());
            comunicado.setMensagem(dto.getMensagem());
            comunicado.setPublicoDestino(dto.getPublicoDestino());
            comunicado.setIsUrgente(dto.getIsUrgente());
            comunicado.setCondominios(condominiosAlvo);

            Comunicado comunicadoSalvo = comunicadoRepository.save(comunicado);

            if (novoCaminhoAnexo != null && antigoCaminhoAnexo != null) {
                String simpleFilename = Paths.get(antigoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }

            return comunicadoSalvo;

        } catch (RuntimeException e) {
            if (novoCaminhoAnexo != null && !novoCaminhoAnexo.equals(antigoCaminhoAnexo)) {
                String simpleFilename = Paths.get(novoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw e;
        }
    }

    private void apagar(Comunicado comunicado) {
        String caminhoAnexo = comunicado.getCaminhoAnexo();

        comunicadoLeituraRepository.deleteByComunicadoId(comunicado.getComId());
        comunicado.getCondominios().clear();
        comunicadoRepository.save(comunicado);
        comunicadoRepository.delete(comunicado);

        if (caminhoAnexo != null) {
            String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
            fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
        }
    }

    /**
     * Nome com que o arquivo foi enviado, sem as pastas que alguns navegadores mandam junto e com no máximo 255
     * caracteres, preservando a extensão. Nulo se o navegador não mandou nome.
     */
    static String nomeOriginal(MultipartFile anexo) {
        String nome = anexo.getOriginalFilename();
        if (nome == null) {
            return null;
        }
        nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1)
                .replaceAll("\\p{Cntrl}", "")
                .trim();
        if (nome.isEmpty()) {
            return null;
        }
        if (nome.length() > TAMANHO_DO_NOME_DO_ANEXO) {
            int ponto = nome.lastIndexOf('.');
            String extensao = ponto > 0 && nome.length() - ponto <= 20 ? nome.substring(ponto) : "";
            nome = nome.substring(0, TAMANHO_DO_NOME_DO_ANEXO - extensao.length()) + extensao;
        }
        return nome;
    }
}
