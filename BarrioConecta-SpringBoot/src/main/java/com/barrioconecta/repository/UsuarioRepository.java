package com.barrioconecta.repository;
import com.barrioconecta.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    List<Usuario> findByRol(String rol);
}
