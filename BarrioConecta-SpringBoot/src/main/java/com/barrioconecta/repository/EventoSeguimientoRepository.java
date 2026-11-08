package com.barrioconecta.repository;
import com.barrioconecta.model.EventoSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
public interface EventoSeguimientoRepository extends JpaRepository<EventoSeguimiento,Long> {
    List<EventoSeguimiento> findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(String objetivoTipo, Collection<Long> ids);
}
