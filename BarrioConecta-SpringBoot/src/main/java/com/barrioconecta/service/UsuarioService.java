package com.barrioconecta.service;

import com.barrioconecta.model.Usuario;
import com.barrioconecta.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public Usuario registrar(String nombre, String correo, String password, String zona) {
        if (usuarios.existsByCorreo(correo)) throw new IllegalArgumentException("Ese correo ya está registrado.");
        Usuario u = new Usuario();
        u.setNombre(nombre.trim());
        u.setCorreo(correo.trim().toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setZona(zona == null ? "" : zona.trim());
        return usuarios.save(u);
    }

    public Usuario autenticar(String correo, String password) {
        return usuarios.findByCorreo(correo.trim().toLowerCase())
            .filter(u -> encoder.matches(password, u.getPassword()) && !u.isSuspendido())
            .orElse(null);
    }
}
