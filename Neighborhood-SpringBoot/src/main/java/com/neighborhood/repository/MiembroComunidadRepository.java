package com.neighborhood.repository;

import com.neighborhood.model.MiembroComunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MiembroComunidadRepository extends JpaRepository<MiembroComunidad, Long> {
    Optional<MiembroComunidad> findByComunidadIdAndUsuarioId(Long comunidadId, Long usuarioId);
    List<MiembroComunidad> findByComunidadIdOrderByUnidoEnDesc(Long comunidadId);
    List<MiembroComunidad> findByUsuarioId(Long usuarioId);
    long countByComunidadId(Long comunidadId);
    long countByUsuarioId(Long usuarioId);
}
