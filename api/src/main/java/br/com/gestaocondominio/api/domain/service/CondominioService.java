package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.GestaoComunicacaoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.SolicitacaoManutencaoRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CondominioService {

    private final CondominioRepository condominioRepository;
    private final AdministradoraRepository administradoraRepository;
    private final UnidadeRepository unidadeRepository;
    private final GestaoComunicacaoRepository gestaoComunicacaoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final AreaComumRepository areaComumRepository;
    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final DocumentoRepository documentoRepository;

    public CondominioService(CondominioRepository condominioRepository,
                             AdministradoraRepository administradoraRepository,
                             UnidadeRepository unidadeRepository,
                             GestaoComunicacaoRepository gestaoComunicacaoRepository,
                             AssembleiaRepository assembleiaRepository,
                             AreaComumRepository areaComumRepository,
                             SolicitacaoManutencaoRepository solicitacaoManutencaoRepository,
                             UsuarioCondominioRepository usuarioCondominioRepository,
                             DocumentoRepository documentoRepository) {
        this.condominioRepository = condominioRepository;
        this.administradoraRepository = administradoraRepository;
        this.unidadeRepository = unidadeRepository;
        this.gestaoComunicacaoRepository = gestaoComunicacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.areaComumRepository = areaComumRepository;
        this.solicitacaoManutencaoRepository = solicitacaoManutencaoRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.documentoRepository = documentoRepository;
    }

    public Condominio cadastrarCondominio(Condominio condominio) {
        if (condominio.getAdministradora() == null || condominio.getAdministradora().getAdmCod() == null) {
            throw new IllegalArgumentException("Administradora deve ser informada para o condomínio.");
        }
        administradoraRepository.findById(condominio.getAdministradora().getAdmCod())
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + condominio.getAdministradora().getAdmCod()));

        if (condominio.getConPais() == null || condominio.getConPais().trim().isEmpty()) {
            condominio.setConPais("Brasil");
        }

        condominio.setConDtCadastro(LocalDateTime.now());
        condominio.setConDtAtualizacao(LocalDateTime.now());

        if (condominio.getConAtivo() == null) {
            condominio.setConAtivo(true);
        }

        return condominioRepository.save(condominio);
    }

    public Optional<Condominio> buscarCondominioPorId(Integer id) {
        return condominioRepository.findById(id);
    }

    public List<Condominio> listarTodosCondominiosAtivos() {
        return condominioRepository.findByConAtivo(true);
    }

    public List<Condominio> listarTodosCondominios(boolean incluirInativas) {
        if (incluirInativas) {
            return condominioRepository.findAll();
        }
        return condominioRepository.findByConAtivo(true);
    }

    public Condominio atualizarCondominio(Integer id, Condominio condominioAtualizado) { 
        Condominio condominioExistente = condominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + id));

        
        if (condominioAtualizado.getAdministradora() != null &&
            (condominioExistente.getAdministradora() == null || !condominioAtualizado.getAdministradora().getAdmCod().equals(condominioExistente.getAdministradora().getAdmCod()))) {
            administradoraRepository.findById(condominioAtualizado.getAdministradora().getAdmCod())
                .orElseThrow(() -> new IllegalArgumentException("Nova Administradora não encontrada com o ID: " + condominioAtualizado.getAdministradora().getAdmCod()));
            condominioExistente.setAdministradora(condominioAtualizado.getAdministradora());
        }
        
        
        if (condominioAtualizado.getConNome() != null) {
            condominioExistente.setConNome(condominioAtualizado.getConNome());
        }
        if (condominioAtualizado.getConLogradouro() != null) {
            condominioExistente.setConLogradouro(condominioAtualizado.getConLogradouro());
        }
        if (condominioAtualizado.getConNumero() != null) {
            condominioExistente.setConNumero(condominioAtualizado.getConNumero());
        }
        if (condominioAtualizado.getConComplemento() != null) {
            condominioExistente.setConComplemento(condominioAtualizado.getConComplemento());
        }
        if (condominioAtualizado.getConBairro() != null) {
            condominioExistente.setConBairro(condominioAtualizado.getConBairro());
        }
        if (condominioAtualizado.getConCidade() != null) {
            condominioExistente.setConCidade(condominioAtualizado.getConCidade());
        }
        if (condominioAtualizado.getConEstado() != null) {
            condominioExistente.setConEstado(condominioAtualizado.getConEstado());
        }
        if (condominioAtualizado.getConCep() != null) {
            condominioExistente.setConCep(condominioAtualizado.getConCep());
        }
        if (condominioAtualizado.getConPais() != null) {
            condominioExistente.setConPais(condominioAtualizado.getConPais());
        }
        if (condominioAtualizado.getConReferencia() != null) {
            condominioExistente.setConReferencia(condominioAtualizado.getConReferencia());
        }
        if (condominioAtualizado.getConNumeroUnidades() != null) {
            condominioExistente.setConNumeroUnidades(condominioAtualizado.getConNumeroUnidades());
        }
        if (condominioAtualizado.getConTipologia() != null) {
            condominioExistente.setConTipologia(condominioAtualizado.getConTipologia());
        }
        if (condominioAtualizado.getConDtVencimentoTaxa() != null) {
            condominioExistente.setConDtVencimentoTaxa(condominioAtualizado.getConDtVencimentoTaxa());
        }
        
       
        if (condominioAtualizado.getConAtivo() != null) {
            condominioExistente.setConAtivo(condominioAtualizado.getConAtivo());
        }

        condominioExistente.setConDtAtualizacao(LocalDateTime.now());
        return condominioRepository.save(condominioExistente);
    }

    public Condominio inativarCondominio(Integer id) {
        Condominio condominio = condominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + id));
        
        
        if (!unidadeRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem unidades vinculadas a ele.");
        }
        if (!gestaoComunicacaoRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem comunicados vinculados a ele.");
        }
        if (!assembleiaRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem assembleias vinculadas a ele.");
        }
        if (!areaComumRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem áreas comuns vinculadas a ele.");
        }
        if (!solicitacaoManutencaoRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem solicitações de manutenção vinculadas a ele.");
        }
        if (!usuarioCondominioRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem usuários/papéis vinculados a ele.");
        }
        if (!documentoRepository.findByCondominio(condominio).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o condomínio, pois existem documentos vinculados a ele.");
        }

        condominio.setConAtivo(false);
        condominio.setConDtAtualizacao(LocalDateTime.now());
        return condominioRepository.save(condominio);
    }

    public Condominio ativarCondominio(Integer id) {
        Condominio condominio = condominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + id));
        condominio.setConAtivo(true);
        condominio.setConDtAtualizacao(LocalDateTime.now());
        return condominioRepository.save(condominio);
    }
}