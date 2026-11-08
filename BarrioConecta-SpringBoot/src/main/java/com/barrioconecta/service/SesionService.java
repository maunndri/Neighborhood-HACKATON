package com.barrioconecta.service;

import com.barrioconecta.model.Usuario;
import com.barrioconecta.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.net.URI;

/** Utilidades de sesión compartidas por todos los controladores. */
@Component
public class SesionService {
    private final UsuarioRepository usuarios;

    public SesionService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    /** Usuario autenticado o null. Las cuentas suspendidas se tratan como no autenticadas. */
    public Usuario actual(HttpSession session) {
        if (session == null) return null;
        Object id = session.getAttribute("usuarioId");
        if (!(id instanceof Long)) return null;
        return usuarios.findById((Long) id).filter(u -> !u.isSuspendido()).orElse(null);
    }

    public boolean esAdmin(Usuario u) { return u != null && u.isAdmin(); }

    public void iniciar(HttpSession session, Usuario u) {
        session.setAttribute("usuarioId", u.getId());
        session.setAttribute("usuarioNombre", u.getNombre());
        session.setAttribute("usuarioRol", u.getRol());
    }

    /** Redirige a la página de origen (si es del mismo sitio) o a la ruta por defecto. */
    public String volver(HttpServletRequest req, String porDefecto, String fragmento) {
        String ancla = fragmento == null || fragmento.isBlank() ? "" : "#" + fragmento;
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                URI u = URI.create(ref);
                if (req.getServerName().equalsIgnoreCase(u.getHost()) && u.getRawPath() != null) {
                    String ruta = u.getRawPath() + (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                    return "redirect:" + ruta + ancla;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        return "redirect:" + porDefecto + ancla;
    }
}
