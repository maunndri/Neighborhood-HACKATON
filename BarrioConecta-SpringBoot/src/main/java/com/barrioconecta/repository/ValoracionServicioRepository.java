package com.barrioconecta.repository;

import com.barrioconecta.model.ValoracionServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ValoracionServicioRepository extends JpaRepository<ValoracionServicio, Long> {
    List<ValoracionServicio> findByServicioId(Long servicioId);
    List<ValoracionServicio> findByServicioIdOrderByCreadoEnDesc(Long servicioId);
    List<ValoracionServicio> findByServicioIdIn(Collection<Long> servicioIds);
    List<ValoracionServicio> findByServicioUsuarioId(Long usuarioId);
    Optional<ValoracionServicio> findByServicioIdAndUsuarioId(Long servicioId, Long usuarioId);
}
