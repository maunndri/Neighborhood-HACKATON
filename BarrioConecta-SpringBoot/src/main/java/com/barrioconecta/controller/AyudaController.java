package com.barrioconecta.controller;

import com.barrioconecta.model.RespuestaAyuda;
import com.barrioconecta.model.Usuario;
import com.barrioconecta.repository.RespuestaAyudaRepository;
import com.barrioconecta.service.AyudaService;
import com.barrioconecta.service.SesionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Acciones de coordinación: responder, aceptar, rechazar y resolver. Valen para favores y emergencias. */
@Controller
@RequestMapping("/ayuda")
public class AyudaController {
    private final AyudaService ayuda;
    private final SesionService sesion;
    private final RespuestaAyudaRepository respuestas;

    public AyudaController(AyudaService ayuda, SesionService sesion, RespuestaAyudaRepository respuestas) {
        this.ayuda = ayuda; this.sesion = sesion; this.respuestas = respuestas;
    }

    private static String ancla(String tipo, Long id) { return tipo + "-" + id; }
    private static String base(String tipo) { return AyudaService.EMERGENCIA.equals(tipo) ? "/emergencias" : "/favores"; }

    @PostMapping("/{tipo}/{id}/responder")
    public String responder(@PathVariable String tipo, @PathVariable Long id, @RequestParam(required=false) String mensaje,
                            HttpSession session, HttpServletRequest req, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        String error = ayuda.responder(u, tipo, id, mensaje);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("mensaje", "Enviado. El vecino recibió un aviso y decidirá si acepta tu ayuda.");
        return sesion.volver(req, base(tipo), ancla(tipo, id));
    }

    @PostMapping("/respuesta/{id}/aceptar")
    public String aceptar(@PathVariable Long id, HttpSession session, HttpServletRequest req, RedirectAttributes ra) {
        return decidir(id, true, session, req, ra);
    }

    @PostMapping("/respuesta/{id}/rechazar")
    public String rechazar(@PathVariable Long id, HttpSession session, HttpServletRequest req, RedirectAttributes ra) {
        return decidir(id, false, session, req, ra);
    }

    private String decidir(Long id, boolean aceptar, HttpSession session, HttpServletRequest req, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        RespuestaAyuda r = respuestas.findById(id).orElse(null);
        String error = ayuda.decidir(u, id, aceptar);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("mensaje", aceptar
            ? "Ayuda aceptada. La publicación pasó a «En coordinación»: ahora pueden contactarse."
            : "Respuesta rechazada. Se avisó al vecino.");
        return r == null ? "redirect:/favores" : sesion.volver(req, base(r.getObjetivoTipo()), ancla(r.getObjetivoTipo(), r.getObjetivoId()));
    }

    @PostMapping("/{tipo}/{id}/resolver")
    public String resolver(@PathVariable String tipo, @PathVariable Long id, HttpSession session,
                           HttpServletRequest req, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        String error = ayuda.resolver(u, tipo, id);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("mensaje", "¡Qué bueno! Quedó marcada como resuelta y se avisó a quienes te ayudaron.");
        return sesion.volver(req, base(tipo), ancla(tipo, id));
    }
}
