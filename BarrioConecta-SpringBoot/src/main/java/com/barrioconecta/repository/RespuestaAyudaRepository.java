package com.barrioconecta.repository;
import com.barrioconecta.model.RespuestaAyuda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface RespuestaAyudaRepository extends JpaRepository<RespuestaAyuda,Long> {
    List<RespuestaAyuda> findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(String objetivoTipo, Collection<Long> ids);
    List<RespuestaAyuda> findByObjetivoTipoAndObjetivoId(String objetivoTipo, Long objetivoId);
    Optional<RespuestaAyuda> findByObjetivoTipoAndObjetivoIdAndUsuarioId(String objetivoTipo, Long objetivoId, Long usuarioId);
    List<RespuestaAyuda> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}
