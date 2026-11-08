package com.barrioconecta.repository;
import com.barrioconecta.model.GrupoApoyo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface GrupoApoyoRepository extends JpaRepository<GrupoApoyo,Long> {
    List<GrupoApoyo> findByActivoTrueOrderByCreadoEnDesc();
    List<GrupoApoyo> findByActivoTrueAndCategoriaIgnoreCaseOrderByCreadoEnDesc(String categoria);
    List<GrupoApoyo> findByActivoTrueAndOcultoFalseOrderByVerificadoDescCreadoEnDesc();
    List<GrupoApoyo> findByActivoTrueAndOcultoFalseAndCategoriaIgnoreCaseOrderByVerificadoDescCreadoEnDesc(String categoria);
    List<GrupoApoyo> findAllByOrderByCreadoEnDesc();
}
