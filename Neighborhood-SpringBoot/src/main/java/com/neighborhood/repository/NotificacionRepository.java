package com.neighborhood.repository;
import com.neighborhood.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificacionRepository extends JpaRepository<Notificacion,Long> {
    List<Notificacion> findTop30ByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
