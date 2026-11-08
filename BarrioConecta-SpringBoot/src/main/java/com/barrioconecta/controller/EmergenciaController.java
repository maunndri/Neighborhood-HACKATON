package com.barrioconecta.controller;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.service.*;
import com.barrioconecta.util.Catalogos;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Centro de ayuda vecinal: necesidades en emergencia con estados, responsables y seguimiento. */
@Controller
public class EmergenciaController {
    private final EmergenciaRepository emergencias;
    private final GrupoApoyoRepository gruposApoyo;
    private final SesionService sesion;
    private final AyudaService ayuda;

    public EmergenciaController(EmergenciaRepository emergencias, GrupoApoyoRepository gruposApoyo, SesionService sesion, AyudaService ayuda) {
        this.emergencias = emergencias; this.gruposApoyo = gruposApoyo; this.sesion = sesion; this.ayuda = ayuda;
    }

    @GetMapping("/emergencias")
    public String lista(@RequestParam(required=false) String estado, Model model, HttpSession session) {
        Usuario u = sesion.actual(session);
        List<ReporteEmergencia> activos = emergencias.findByOcultoFalseAndEstadoNotOrderByCreadoEnDesc(Catalogos.ESTADO_RESUELTA);
        long pendientes = activos.stream().filter(e -> Catalogos.ESTADO_PENDIENTE.equals(e.getEstado())).count();
        long coordinando = activos.stream().filter(e -> Catalogos.ESTADO_COORDINANDO.equals(e.getEstado())).count();
        List<ReporteEmergencia> atendidas = emergencias.findByOcultoFalseAndEstadoOrderByActualizadoEnDesc(Catalogos.ESTADO_RESUELTA);
        List<ReporteEmergencia> visibles = activos.stream()
            .filter(e -> estado == null || estado.isBlank() || e.getEstado().equals(estado)).toList();
        ayuda.cargarEmergencias(visibles);
        model.addAttribute("reportes", visibles);
        model.addAttribute("estado", estado == null ? "" : estado);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("coordinando", coordinando);
        model.addAttribute("atendidasTotal", atendidas.size());
        model.addAttribute("atendidas", atendidas.stream().limit(3).toList());
        model.addAttribute("recursos", gruposApoyo.findByActivoTrueAndOcultoFalseOrderByVerificadoDescCreadoEnDesc().stream().limit(6).toList());
        model.addAttribute("usuarioIdActual", u == null ? null : u.getId());
        return "emergencias";
    }

    @GetMapping("/emergencias/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Usuario u = sesion.actual(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("latitudActual", u.getLatitud());
        model.addAttribute("longitudActual", u.getLongitud());
        return "publicar-emergencia";
    }

    @PostMapping("/emergencias/nuevo")
    public String guardar(@RequestParam String categoria, @RequestParam String titulo, @RequestParam String descripcion,
            @RequestParam(required=false) Double latitud, @RequestParam(required=false) Double longitud,
            HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        ReporteEmergencia e = new ReporteEmergencia();
        e.setUsuario(u); e.setCategoria(categoria); e.setTitulo(titulo.trim()); e.setDescripcion(descripcion.trim());
        e.setLatitud(Catalogos.aproximar(latitud != null ? latitud : u.getLatitud()));
        e.setLongitud(Catalogos.aproximar(longitud != null ? longitud : u.getLongitud()));
        emergencias.save(e);
        ayuda.registrar(AyudaService.EMERGENCIA, e.getId(), "Necesidad reportada por " + u.getNombreCorto());
        ra.addFlashAttribute("mensaje", "Tu solicitud quedó registrada como Pendiente. Los vecinos podrán ofrecerte ayuda y te avisaremos.");
        return "redirect:/emergencias";
    }
}
