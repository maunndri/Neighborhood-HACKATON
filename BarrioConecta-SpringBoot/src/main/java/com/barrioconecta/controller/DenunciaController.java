package com.barrioconecta.controller;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.service.*;
import com.barrioconecta.util.Catalogos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cualquier vecino puede reportar una publicación o recurso sospechoso para que un administrador lo revise. */
@Controller
public class DenunciaController {
    private final DenunciaRepository denuncias;
    private final ServicioRepository servicios;
    private final FavorRepository favores;
    private final EmergenciaRepository emergencias;
    private final GrupoApoyoRepository grupos;
    private final SesionService sesion;
    private final NotificacionService notificaciones;

    public DenunciaController(DenunciaRepository denuncias, ServicioRepository servicios, FavorRepository favores,
                              EmergenciaRepository emergencias, GrupoApoyoRepository grupos, SesionService sesion,
                              NotificacionService notificaciones) {
        this.denuncias = denuncias; this.servicios = servicios; this.favores = favores; this.emergencias = emergencias;
        this.grupos = grupos; this.sesion = sesion; this.notificaciones = notificaciones;
    }

    @PostMapping("/reportar")
    public String reportar(@RequestParam String tipo, @RequestParam Long id, @RequestParam String motivo,
                           HttpSession session, HttpServletRequest req, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        String titulo = switch (tipo) {
            case "servicio" -> servicios.findById(id).map(PublicacionServicio::getTitulo).orElse(null);
            case "favor" -> favores.findById(id).map(Favor::getTitulo).orElse(null);
            case "emergencia" -> emergencias.findById(id).map(ReporteEmergencia::getTitulo).orElse(null);
            case "recurso" -> grupos.findById(id).map(GrupoApoyo::getNombre).orElse(null);
            default -> null;
        };
        String fallback = "/";
        if (titulo == null) { ra.addFlashAttribute("error", "No encontramos esa publicación."); return "redirect:" + fallback; }
        if (denuncias.existsByTipoAndObjetivoIdAndUsuarioIdAndEstado(tipo, id, u.getId(), "Pendiente")) {
            ra.addFlashAttribute("mensaje", "Ya habías reportado esta publicación. Un administrador la revisará.");
        } else {
            DenunciaContenido d = new DenunciaContenido();
            d.setTipo(tipo); d.setObjetivoId(id); d.setTitulo(titulo); d.setUsuario(u);
            d.setMotivo(Catalogos.limpiar(motivo, 300) == null ? "Sin detalle" : Catalogos.limpiar(motivo, 300));
            denuncias.save(d);
            notificaciones.notificarAdmins("Nueva denuncia sobre «" + titulo + "». Revísala en el panel.", "/admin");
            ra.addFlashAttribute("mensaje", "Gracias por avisar. Un administrador revisará esta publicación.");
        }
        return sesion.volver(req, fallback, null);
    }
}
