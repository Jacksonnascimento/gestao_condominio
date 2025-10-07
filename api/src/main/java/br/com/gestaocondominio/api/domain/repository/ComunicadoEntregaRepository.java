package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.ComunicadoEntrega;
import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComunicadoEntregaRepository extends JpaRepository<ComunicadoEntrega, Integer> {
    List<ComunicadoEntrega> findByDestinatario(Pessoa destinatario);
    List<ComunicadoEntrega> findByComunicado(GestaoComunicacao comunicado);
    Optional<ComunicadoEntrega> findByComunicadoAndDestinatario(GestaoComunicacao comunicado, Pessoa destinatario);
}