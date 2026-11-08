package com.neighborhood.repository;

import com.neighborhood.model.MensajeComunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensajeComunidadRepository extends JpaRepository<MensajeComunidad, Long> {
    List<MensajeComunidad> findByComunidadIdOrderByCreadoEnAsc(Long comunidadId);
    long countByComunidadId(Long comunidadId);
    MensajeComunidad findTopByComunidadIdOrderByCreadoEnDesc(Long comunidadId);
}
