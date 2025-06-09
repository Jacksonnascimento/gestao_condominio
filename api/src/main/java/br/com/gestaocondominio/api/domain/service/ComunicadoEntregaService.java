package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.ComunicadoEntrega;
import br.com.gestaocondominio.api.domain.repository.ComunicadoEntregaRepository;
import br.com.gestaocondominio.api.domain.repository.GestaoComunicacaoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class ComunicadoEntregaService {

    private final ComunicadoEntregaRepository comunicadoEntregaRepository;
    private final GestaoComunicacaoRepository gestaoComunicacaoRepository;
    private final PessoaRepository pessoaRepository;

    public ComunicadoEntregaService(ComunicadoEntregaRepository comunicadoEntregaRepository,
                                    GestaoComunicacaoRepository gestaoComunicacaoRepository,
                                    PessoaRepository pessoaRepository) {
        this.comunicadoEntregaRepository = comunicadoEntregaRepository;
        this.gestaoComunicacaoRepository = gestaoComunicacaoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public ComunicadoEntrega cadastrarComunicadoEntrega(ComunicadoEntrega comunicadoEntrega) {
        if (comunicadoEntrega.getComunicado() == null || comunicadoEntrega.getComunicado().getComCod() == null) {
            throw new IllegalArgumentException("Comunicado deve ser informado para o registro de entrega.");
        }
        gestaoComunicacaoRepository.findById(comunicadoEntrega.getComunicado().getComCod())
                .orElseThrow(() -> new IllegalArgumentException("Comunicado não encontrado com o ID: " + comunicadoEntrega.getComunicado().getComCod()));

        if (comunicadoEntrega.getDestinatario() == null || comunicadoEntrega.getDestinatario().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa destinatária deve ser informada para o registro de entrega.");
        }
        pessoaRepository.findById(comunicadoEntrega.getDestinatario().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa destinatária não encontrada com o ID: " + comunicadoEntrega.getDestinatario().getPesCod()));

        Optional<ComunicadoEntrega> entregaExistente = comunicadoEntregaRepository.findByComunicadoAndDestinatario(
            comunicadoEntrega.getComunicado(),
            comunicadoEntrega.getDestinatario()
        );
        if (entregaExistente.isPresent()) {
            throw new IllegalArgumentException("Este comunicado já foi registrado para esta pessoa destinatária.");
        }

        if (comunicadoEntrega.getStatusLeitura() == null) {
            comunicadoEntrega.setStatusLeitura(false);
        }
        if (comunicadoEntrega.getStatusEnvio() == null || comunicadoEntrega.getStatusEnvio().trim().isEmpty()) {
            comunicadoEntrega.setStatusEnvio("PENDENTE");
        }

        return comunicadoEntregaRepository.save(comunicadoEntrega);
    }

    public Optional<ComunicadoEntrega> buscarComunicadoEntregaPorId(Integer id) {
        return comunicadoEntregaRepository.findById(id);
    }

    public List<ComunicadoEntrega> listarTodasComunicadoEntregas() {
        return comunicadoEntregaRepository.findAll();
    }

    public ComunicadoEntrega atualizarComunicadoEntrega(Integer id, ComunicadoEntrega comunicadoEntregaAtualizada) {
        ComunicadoEntrega comunicadoEntregaExistente = comunicadoEntregaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de entrega de comunicado não encontrado com o ID: " + id));

        if (!comunicadoEntregaAtualizada.getComunicado().getComCod().equals(comunicadoEntregaExistente.getComunicado().getComCod()) ||
            !comunicadoEntregaAtualizada.getDestinatario().getPesCod().equals(comunicadoEntregaExistente.getDestinatario().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Comunicado ou a Pessoa Destinatária de um registro de entrega existente.");
        }

        comunicadoEntregaExistente.setStatusLeitura(comunicadoEntregaAtualizada.getStatusLeitura());
        comunicadoEntregaExistente.setDtLeitura(comunicadoEntregaAtualizada.getDtLeitura());
        comunicadoEntregaExistente.setStatusEnvio(comunicadoEntregaAtualizada.getStatusEnvio());
        comunicadoEntregaExistente.setDtEnvioIndividual(comunicadoEntregaAtualizada.getDtEnvioIndividual());

        return comunicadoEntregaRepository.save(comunicadoEntregaExistente);
    }
}