package com.barrioconecta.repository;
import com.barrioconecta.model.ReporteEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EmergenciaRepository extends JpaRepository<ReporteEmergencia,Long> {
    List<ReporteEmergencia> findAllByOrderByCreadoEnDesc();
    List<ReporteEmergencia> findByEstadoNotOrderByCreadoEnDesc(String estado);
    List<ReporteEmergencia> findByOcultoFalseAndEstadoNotOrderByCreadoEnDesc(String estado);
    List<ReporteEmergencia> findByOcultoFalseAndEstadoOrderByActualizadoEnDesc(String estado);
    List<ReporteEmergencia> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}
