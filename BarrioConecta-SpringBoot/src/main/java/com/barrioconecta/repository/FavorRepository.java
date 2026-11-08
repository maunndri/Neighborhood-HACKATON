package com.barrioconecta.repository;
import com.barrioconecta.model.Favor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FavorRepository extends JpaRepository<Favor,Long> {
    List<Favor> findAllByOrderByCreadoEnDesc();
    List<Favor> findByActivoTrueOrderByCreadoEnDesc();
    List<Favor> findByActivoTrueAndTipoOrderByCreadoEnDesc(String tipo);
    List<Favor> findByOcultoFalseAndActivoTrueOrderByCreadoEnDesc();
    List<Favor> findByOcultoFalseAndActivoTrueAndTipoOrderByCreadoEnDesc(String tipo);
    List<Favor> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
    long countByActivoFalseAndOcultoFalse();
}
