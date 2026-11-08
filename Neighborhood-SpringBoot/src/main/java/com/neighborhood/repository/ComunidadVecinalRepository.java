package com.neighborhood.repository;

import com.neighborhood.model.ComunidadVecinal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComunidadVecinalRepository extends JpaRepository<ComunidadVecinal, Long> {
    List<ComunidadVecinal> findAllByOrderByDistritoAscNivelAscNombreAsc();
    List<ComunidadVecinal> findByDistritoIgnoreCaseOrderByNivelAscNombreAsc(String distrito);
}
