package com.barrioconecta.controller;

import com.barrioconecta.model.Notificacion;
import com.barrioconecta.model.Usuario;
import com.barrioconecta.repository.NotificacionRepository;
import com.barrioconecta.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Avisos dentro de la app: alguien respondió, aceptó tu ayuda, SOS cercano, moderación, etc. */
@Controller
public class NotificacionController {
    private final NotificacionRepository notificaciones;
    private final SesionService sesion;

    public NotificacionController(NotificacionRepository notificaciones, SesionService sesion) {
        this.notificaciones = notificaciones; this.sesion = sesion;
    }

    @GetMapping("/notificaciones")
    public String lista(HttpSession session, Model model) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        List<Notificacion> lista = notificaciones.findTop30ByUsuarioIdOrderByCreadoEnDesc(u.getId());
        Set<Long> nuevas = new HashSet<>();
        for (Notificacion n : lista) if (!n.isLeida()) nuevas.add(n.getId());
        model.addAttribute("notificaciones", lista);
        model.addAttribute("nuevas", nuevas);
        // Se marcan como leídas después de mostrarlas por primera vez.
        List<Notificacion> pendientes = notificaciones.findByUsuarioIdAndLeidaFalse(u.getId());
        pendientes.forEach(n -> n.setLeida(true));
        notificaciones.saveAll(pendientes);
        return "notificaciones";
    }

    @GetMapping("/api/notificaciones/resumen")
    @ResponseBody
    public Map<String, Object> resumen(HttpSession session) {
        Usuario u = sesion.actual(session);
        Map<String, Object> out = new LinkedHashMap<>();
        if (u == null) { out.put("noLeidas", 0); out.put("ultimas", List.of()); return out; }
        List<Map<String, Object>> ultimas = new ArrayList<>();
        for (Notificacion n : notificaciones.findTop30ByUsuarioIdOrderByCreadoEnDesc(u.getId())) {
            if (n.isLeida() || ultimas.size() >= 5) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId()); m.put("mensaje", n.getMensaje()); m.put("enlace", n.getEnlace());
            ultimas.add(m);
        }
        out.put("noLeidas", notificaciones.countByUsuarioIdAndLeidaFalse(u.getId()));
        out.put("ultimas", ultimas);
        return out;
    }
}
