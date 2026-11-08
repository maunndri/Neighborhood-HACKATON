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

/** Cadena de favores: "Necesito ayuda" y "Quiero ayudar", con estados y coordinación entre vecinos. */
@Controller
public class FavorController {
    private final FavorRepository favores;
    private final UsuarioRepository usuarios;
    private final SesionService sesion;
    private final AyudaService ayuda;

    public FavorController(FavorRepository favores, UsuarioRepository usuarios, SesionService sesion, AyudaService ayuda) {
        this.favores = favores; this.usuarios = usuarios; this.sesion = sesion; this.ayuda = ayuda;
    }

    @GetMapping("/favores")
    public String lista(@RequestParam(defaultValue="necesito") String vista, @RequestParam(required=false) String categoria,
                        @RequestParam(required=false) String estado, Model model, HttpSession session) {
        Usuario u = sesion.actual(session);
        String tipo = "ofrezco".equals(vista) ? Catalogos.TIPO_OFREZCO : Catalogos.TIPO_SOLICITO;
        List<Favor> todos = favores.findByOcultoFalseAndActivoTrueOrderByCreadoEnDesc();
        long necesitan = todos.stream().filter(f -> !f.isOferta()).count();
        long ofrecen = todos.stream().filter(Favor::isOferta).count();
        List<Favor> lista = todos.stream()
            .filter(f -> tipo.equals(f.getTipo()))
            .filter(f -> categoria == null || categoria.isBlank() || f.getCategoria().equalsIgnoreCase(categoria))
            .filter(f -> estado == null || estado.isBlank() || f.getEstado().equals(estado))
            .toList();
        ayuda.cargarFavores(lista);
        model.addAttribute("favores", lista);
        model.addAttribute("vista", "ofrezco".equals(vista) ? "ofrezco" : "necesito");
        model.addAttribute("categoria", categoria == null ? "" : categoria);
        model.addAttribute("estado", estado == null ? "" : estado);
        model.addAttribute("totalNecesitan", necesitan);
        model.addAttribute("totalOfrecen", ofrecen);
        model.addAttribute("totalResueltos", favores.countByActivoFalseAndOcultoFalse());
        model.addAttribute("usuarioIdActual", u == null ? null : u.getId());
        return "favores";
    }

    @GetMapping("/favores/nuevo")
    public String nuevo(@RequestParam(defaultValue="solicito") String tipo, HttpSession session, Model model) {
        Usuario u = sesion.actual(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("tipo", Catalogos.TIPO_OFREZCO.equals(tipo) ? Catalogos.TIPO_OFREZCO : Catalogos.TIPO_SOLICITO);
        model.addAttribute("telefonoActual", u.getTelefono());
        model.addAttribute("latitudActual", u.getLatitud());
        model.addAttribute("longitudActual", u.getLongitud());
        return "publicar-favor";
    }

    @PostMapping("/favores/nuevo")
    public String guardar(@RequestParam String tipo, @RequestParam String titulo, @RequestParam String descripcion,
            @RequestParam(required=false) String categoria, @RequestParam(required=false) String disponibilidad,
            @RequestParam(required=false) String telefono, @RequestParam(required=false) Double latitud,
            @RequestParam(required=false) Double longitud, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        String tipoFinal = Catalogos.TIPO_OFREZCO.equals(tipo) ? Catalogos.TIPO_OFREZCO : Catalogos.TIPO_SOLICITO;
        String tel = Catalogos.limpiar(telefono, 40);
        if (tel != null) { u.setTelefono(tel); usuarios.save(u); }
        Favor f = new Favor();
        f.setUsuario(u); f.setTipo(tipoFinal); f.setTitulo(titulo.trim()); f.setDescripcion(descripcion.trim());
        f.setCategoria(Catalogos.limpiar(categoria, 100)); f.setDisponibilidad(Catalogos.limpiar(disponibilidad, 100));
        // Ubicación aproximada (~100 m): si no marcó una, se usa la de su perfil.
        f.setLatitud(Catalogos.aproximar(latitud != null ? latitud : u.getLatitud()));
        f.setLongitud(Catalogos.aproximar(longitud != null ? longitud : u.getLongitud()));
        favores.save(f);
        ayuda.registrar(AyudaService.FAVOR, f.getId(), (f.isOferta() ? "Oferta publicada por " : "Solicitud publicada por ") + u.getNombreCorto());
        ra.addFlashAttribute("mensaje", f.isOferta()
            ? "Tu ofrecimiento ya está visible. Te avisaremos cuando un vecino lo necesite."
            : "Tu solicitud ya está visible. Te avisaremos cuando un vecino se ofrezca a ayudar.");
        return "redirect:/favores?vista=" + (f.isOferta() ? "ofrezco" : "necesito");
    }

    @PostMapping("/favores/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        Favor f = favores.findById(id).orElse(null);
        if (f == null || !(f.getUsuario().getId().equals(u.getId()) || u.isAdmin())) ra.addFlashAttribute("error", "No puedes eliminar esta publicación.");
        else { f.setActivo(false); f.setOculto(true); favores.save(f); ra.addFlashAttribute("mensaje", "La publicación fue eliminada."); }
        return "redirect:/favores";
    }
}
