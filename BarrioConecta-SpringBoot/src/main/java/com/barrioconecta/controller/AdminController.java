package com.barrioconecta.controller;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/** Panel de moderación: denuncias, publicaciones sospechosas y verificación de recursos. Solo para ADMIN. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ServicioRepository servicios;
    private final FavorRepository favores;
    private final EmergenciaRepository emergencias;
    private final GrupoApoyoRepository grupos;
    private final UsuarioRepository usuarios;
    private final DenunciaRepository denuncias;
    private final SesionService sesion;
    private final NotificacionService notificaciones;

    public AdminController(ServicioRepository servicios, FavorRepository favores, EmergenciaRepository emergencias,
                           GrupoApoyoRepository grupos, UsuarioRepository usuarios, DenunciaRepository denuncias,
                           SesionService sesion, NotificacionService notificaciones) {
        this.servicios = servicios; this.favores = favores; this.emergencias = emergencias; this.grupos = grupos;
        this.usuarios = usuarios; this.denuncias = denuncias; this.sesion = sesion; this.notificaciones = notificaciones;
    }

    private Usuario admin(HttpSession session) {
        Usuario u = sesion.actual(session);
        return sesion.esAdmin(u) ? u : null;
    }

    @GetMapping
    public String panel(HttpSession session, Model model, RedirectAttributes ra) {
        if (admin(session) == null) { ra.addFlashAttribute("error", "Esta sección es solo para administradores."); return "redirect:/"; }
        model.addAttribute("denuncias", denuncias.findByEstadoOrderByCreadoEnDesc("Pendiente"));
        model.addAttribute("servicios", servicios.findAllByOrderByCreadoEnDesc());
        model.addAttribute("favores", favores.findAllByOrderByCreadoEnDesc());
        model.addAttribute("emergencias", emergencias.findAllByOrderByCreadoEnDesc());
        model.addAttribute("recursos", grupos.findAllByOrderByCreadoEnDesc());
        model.addAttribute("vecinos", usuarios.findAll());
        return "admin";
    }

    /** Verifica o quita la verificación de una publicación o recurso. No certifica títulos profesionales. */
    @PostMapping("/verificar/{tipo}/{id}")
    public String verificar(@PathVariable String tipo, @PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/";
        if ("servicio".equals(tipo)) {
            PublicacionServicio s = servicios.findById(id).orElse(null);
            if (s != null) {
                s.setVerificado(!s.isVerificado()); s.setVerificadoEn(s.isVerificado() ? LocalDateTime.now() : null); servicios.save(s);
                if (s.isVerificado()) notificaciones.notificar(s.getUsuario(), "Un administrador verificó tu publicación «" + s.getTitulo() + "».", "/servicios/" + s.getId());
            }
        } else if ("recurso".equals(tipo)) {
            GrupoApoyo g = grupos.findById(id).orElse(null);
            if (g != null) {
                g.setVerificado(!g.isVerificado()); grupos.save(g);
                if (g.isVerificado()) notificaciones.notificar(g.getUsuario(), "Un administrador verificó tu recurso «" + g.getNombre() + "».", "/emergencias/grupos");
            }
        }
        ra.addFlashAttribute("mensaje", "Estado de verificación actualizado.");
        return "redirect:/admin";
    }

    /** Oculta o vuelve a mostrar una publicación (no se borra: queda para revisión). */
    @PostMapping("/ocultar/{tipo}/{id}")
    public String ocultar(@PathVariable String tipo, @PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/";
        Usuario dueno = null; String titulo = null; boolean oculto = false;
        switch (tipo) {
            case "servicio" -> { PublicacionServicio s = servicios.findById(id).orElse(null);
                if (s != null) { s.setOculto(!s.isOculto()); servicios.save(s); dueno = s.getUsuario(); titulo = s.getTitulo(); oculto = s.isOculto(); } }
            case "favor" -> { Favor f = favores.findById(id).orElse(null);
                if (f != null) { f.setOculto(!f.isOculto()); favores.save(f); dueno = f.getUsuario(); titulo = f.getTitulo(); oculto = f.isOculto(); } }
            case "emergencia" -> { ReporteEmergencia e = emergencias.findById(id).orElse(null);
                if (e != null) { e.setOculto(!e.isOculto()); emergencias.save(e); dueno = e.getUsuario(); titulo = e.getTitulo(); oculto = e.isOculto(); } }
            case "recurso" -> { GrupoApoyo g = grupos.findById(id).orElse(null);
                if (g != null) { g.setOculto(!g.isOculto()); grupos.save(g); dueno = g.getUsuario(); titulo = g.getNombre(); oculto = g.isOculto(); } }
            default -> { }
        }
        if (dueno != null && oculto)
            notificaciones.notificar(dueno, "Tu publicación «" + titulo + "» fue ocultada por moderación. Si crees que fue un error, contacta a un administrador.", null);
        ra.addFlashAttribute("mensaje", oculto ? "Publicación ocultada." : "Publicación visible nuevamente.");
        return "redirect:/admin";
    }

    @PostMapping("/denuncias/{id}/resolver")
    public String resolverDenuncia(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/";
        denuncias.findById(id).ifPresent(d -> { d.setEstado("Revisada"); denuncias.save(d); });
        ra.addFlashAttribute("mensaje", "Denuncia marcada como revisada.");
        return "redirect:/admin";
    }

    @PostMapping("/usuarios/{id}/suspender")
    public String suspender(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario a = admin(session); if (a == null) return "redirect:/";
        Usuario u = usuarios.findById(id).orElse(null);
        if (u == null || u.isAdmin() || u.getId().equals(a.getId())) { ra.addFlashAttribute("error", "No se puede suspender esta cuenta."); return "redirect:/admin"; }
        u.setSuspendido(!u.isSuspendido()); usuarios.save(u);
        ra.addFlashAttribute("mensaje", u.isSuspendido() ? "Cuenta suspendida." : "Cuenta reactivada.");
        return "redirect:/admin";
    }
}
