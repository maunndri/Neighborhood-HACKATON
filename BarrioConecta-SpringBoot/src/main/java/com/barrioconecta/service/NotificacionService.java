package com.barrioconecta.service;

import com.barrioconecta.model.Notificacion;
import com.barrioconecta.model.Usuario;
import com.barrioconecta.repository.NotificacionRepository;
import com.barrioconecta.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;

    public NotificacionService(NotificacionRepository notificaciones, UsuarioRepository usuarios) {
        this.notificaciones = notificaciones; this.usuarios = usuarios;
    }

    public void notificar(Usuario destino, String mensaje, String enlace) {
        if (destino == null || mensaje == null) return;
        Notificacion n = new Notificacion();
        n.setUsuario(destino);
        n.setMensaje(mensaje.length() > 290 ? mensaje.substring(0, 290) : mensaje);
        n.setEnlace(enlace);
        notificaciones.save(n);
    }

    public void notificarAdmins(String mensaje, String enlace) {
        for (Usuario admin : usuarios.findByRol("ADMIN")) notificar(admin, mensaje, enlace);
    }
}
