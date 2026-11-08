package com.barrioconecta.repository;
import com.barrioconecta.model.PublicacionServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ServicioRepository extends JpaRepository<PublicacionServicio,Long> {
    List<PublicacionServicio> findAllByOrderByCreadoEnDesc();
    List<PublicacionServicio> findByTipoOrderByCreadoEnDesc(String tipo);
    List<PublicacionServicio> findByOcultoFalseOrderByCreadoEnDesc();
    long countByUsuarioIdAndOcultoFalse(Long usuarioId);
    List<PublicacionServicio> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    /** Búsqueda pública: tipo, categoría y texto libre son opcionales (cadena vacía = sin filtro). */
    @Query("select s from PublicacionServicio s where s.oculto = false "
         + "and (:tipo = '' or s.tipo = :tipo) "
         + "and (:categoria = '' or lower(s.categoria) = lower(:categoria)) "
         + "and (:q = '' or lower(s.titulo) like lower(concat('%', :q, '%')) "
         + "  or lower(s.descripcion) like lower(concat('%', :q, '%')) "
         + "  or lower(s.categoria) like lower(concat('%', :q, '%'))) "
         + "order by s.creadoEn desc")
    List<PublicacionServicio> buscar(@Param("tipo") String tipo, @Param("categoria") String categoria, @Param("q") String q);
}
