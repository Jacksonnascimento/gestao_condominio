package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AssembleiaVoto;
import br.com.gestaocondominio.api.domain.entity.AssembleiaVotoId;
import br.com.gestaocondominio.api.domain.repository.AssembleiaVotoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaVotoService {

    private final AssembleiaVotoRepository assembleiaVotoRepository;
    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final PessoaRepository pessoaRepository;

    public AssembleiaVotoService(AssembleiaVotoRepository assembleiaVotoRepository,
                                 AssembleiaTopicoRepository assembleiaTopicoRepository,
                                 PessoaRepository pessoaRepository) {
        this.assembleiaVotoRepository = assembleiaVotoRepository;
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public AssembleiaVoto cadastrarAssembleiaVoto(AssembleiaVoto voto) {
        if (voto.getTopico() == null || voto.getTopico().getAspCod() == null) {
            throw new IllegalArgumentException("Tópico da assembleia deve ser informado para o voto.");
        }
        assembleiaTopicoRepository.findById(voto.getTopico().getAspCod())
                .orElseThrow(() -> new IllegalArgumentException("Tópico da assembleia não encontrado com o ID: " + voto.getTopico().getAspCod()));

        if (voto.getPessoa() == null || voto.getPessoa().getPesCod() == null) {
            throw new  IllegalArgumentException("Pessoa deve ser informada para o voto.");
        }
        pessoaRepository.findById(voto.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + voto.getPessoa().getPesCod()));

        AssembleiaVotoId id = new AssembleiaVotoId(
            voto.getTopico().getAspCod(),
            voto.getPessoa().getPesCod()
        );
        voto.setId(id);

        if (assembleiaVotoRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já votou neste tópico da assembleia.");
        }
        
        if (voto.getVoto() != null) {
            char tipoVoto = Character.toUpperCase(voto.getVoto());
            if (tipoVoto != 'S' && tipoVoto != 'N' && tipoVoto != 'A') {
                throw new IllegalArgumentException("Tipo de voto inválido. Use 'S' (SIM), 'N' (NÃO) ou 'A' (ABSTENÇÃO).");
            }
            voto.setVoto(tipoVoto);
        } else {
             throw new IllegalArgumentException("O voto não pode ser nulo.");
        }

        return assembleiaVotoRepository.save(voto);
    }

    public Optional<AssembleiaVoto> buscarAssembleiaVotoPorId(AssembleiaVotoId id) {
        return assembleiaVotoRepository.findById(id);
    }

    public List<AssembleiaVoto> listarTodosVotos() {
        return assembleiaVotoRepository.findAll();
    }

    public AssembleiaVoto atualizarAssembleiaVoto(AssembleiaVotoId id, AssembleiaVoto votoAtualizado) {
        AssembleiaVoto votoExistente = assembleiaVotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voto de assembleia não encontrado com o ID: " + id));

        if (!votoAtualizado.getTopico().getAspCod().equals(votoExistente.getTopico().getAspCod()) ||
            !votoAtualizado.getPessoa().getPesCod().equals(votoExistente.getPessoa().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Tópico ou a Pessoa de um voto existente.");
        }

        if (votoAtualizado.getVoto() != null) {
            char tipoVoto = Character.toUpperCase(votoAtualizado.getVoto());
            if (tipoVoto != 'S' && tipoVoto != 'N' && tipoVoto != 'A') {
                throw new IllegalArgumentException("Tipo de voto inválido. Use 'S' (SIM), 'N' (NÃO) ou 'A' (ABSTENÇÃO).");
            }
            votoExistente.setVoto(tipoVoto);
        } else {
             throw new IllegalArgumentException("O voto não pode ser nulo na atualização.");
        }

        return assembleiaVotoRepository.save(votoExistente);
    }

   
    public void deletarVoto(AssembleiaVotoId id) {
        assembleiaVotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voto de assembleia não encontrado para exclusão com o ID: " + id));

        assembleiaVotoRepository.deleteById(id);
    }
}