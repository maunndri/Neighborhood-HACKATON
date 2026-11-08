package com.barrioconecta.repository;
import com.barrioconecta.model.DenunciaContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DenunciaRepository extends JpaRepository<DenunciaContenido,Long> {
    List<DenunciaContenido> findByEstadoOrderByCreadoEnDesc(String estado);
    boolean existsByTipoAndObjetivoIdAndUsuarioIdAndEstado(String tipo, Long objetivoId, Long usuarioId, String estado);
    long countByEstado(String estado);
}
