package com.neighborhood.controller;

import com.neighborhood.model.*;
import com.neighborhood.repository.*;
import com.neighborhood.service.SesionService;
import com.neighborhood.util.Catalogos;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Directorio y salas de Grupos de WhatsApp Vecinales de todo Arequipa (por Distrito y Urbanización).
 * Reglas del sistema:
 * 1. Solo pueden acceder o unirse las personas conectadas (con sesión activa) en el sistema web.
 * 2. El acceso se valida según la localización de la persona (coordenadas GPS o distrito/urbanización).
 * 3. Límite anticongestión: una persona NO puede pertenecer a más de 2 grupos de WhatsApp a la vez.
 */
@Controller
@RequestMapping("/comunidades")
public class ComunidadController {
    public static final int MAX_GRUPOS_POR_VECINO = 2;

    private final ComunidadVecinalRepository comunidades;
    private final MensajeComunidadRepository mensajes;
    private final MiembroComunidadRepository miembros;
    private final UsuarioRepository usuarios;
    private final SesionService sesion;

    /** Coordenadas referenciales de los distritos de Arequipa para validación rápida y selector vecinal. */
    public static final Map<String, double[]> DISTRITOS_AREQUIPA = new LinkedHashMap<>();
    static {
        DISTRITOS_AREQUIPA.put("Tiabaya", new double[]{-16.4580, -71.5885});
        DISTRITOS_AREQUIPA.put("Cercado", new double[]{-16.4090, -71.5375});
        DISTRITOS_AREQUIPA.put("Yanahuara", new double[]{-16.3920, -71.5460});
        DISTRITOS_AREQUIPA.put("Cayma", new double[]{-16.3820, -71.5480});
        DISTRITOS_AREQUIPA.put("Cerro Colorado", new double[]{-16.3740, -71.5640});
        DISTRITOS_AREQUIPA.put("J.L. Bustamante y Rivero", new double[]{-16.4290, -71.5190});
        DISTRITOS_AREQUIPA.put("Paucarpata", new double[]{-16.4250, -71.5030});
        DISTRITOS_AREQUIPA.put("Sachaca", new double[]{-16.4260, -71.5650});
        DISTRITOS_AREQUIPA.put("Miraflores", new double[]{-16.3940, -71.5170});
        DISTRITOS_AREQUIPA.put("Alto Selva Alegre", new double[]{-16.3810, -71.5220});
        DISTRITOS_AREQUIPA.put("Socabaya", new double[]{-16.4580, -71.5280});
        DISTRITOS_AREQUIPA.put("Jacobo Hunter", new double[]{-16.4450, -71.5550});
        DISTRITOS_AREQUIPA.put("Mariano Melgar", new double[]{-16.4040, -71.5030});
        DISTRITOS_AREQUIPA.put("Uchumayo", new double[]{-16.4220, -71.6300});
        DISTRITOS_AREQUIPA.put("Characato y Sabandía", new double[]{-16.4670, -71.4880});
    }

    public ComunidadController(ComunidadVecinalRepository comunidades, MensajeComunidadRepository mensajes,
                               MiembroComunidadRepository miembros, UsuarioRepository usuarios, SesionService sesion) {
        this.comunidades = comunidades;
        this.mensajes = mensajes;
        this.miembros = miembros;
        this.usuarios = usuarios;
        this.sesion = sesion;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String distrito,
                         @RequestParam(required = false) String nivel,
                         @RequestParam(required = false) String q,
                         Model model, HttpSession session) {
        Usuario u = sesion.actual(session);
        UbicacionVecino ub = obtenerUbicacionVecino(u, session);

        List<ComunidadVecinal> todas = comunidades.findAllByOrderByDistritoAscNivelAscNombreAsc();
        List<MiembroComunidad> misMembresias = u == null ? Collections.emptyList() : miembros.findByUsuarioId(u.getId());
        Set<Long> misComunidadesIds = misMembresias.stream().map(m -> m.getComunidad().getId()).collect(Collectors.toSet());

        for (ComunidadVecinal c : todas) {
            completarEstadoComunidad(c, u, ub, misComunidadesIds);
        }

        String distFiltro = distrito == null ? "" : distrito.trim();
        String nivFiltro = nivel == null ? "" : nivel.trim();
        String texto = q == null ? "" : q.trim().toLowerCase();

        List<ComunidadVecinal> filtradas = todas.stream()
            .filter(c -> distFiltro.isEmpty() || c.getDistrito().equalsIgnoreCase(distFiltro))
            .filter(c -> nivFiltro.isEmpty() || c.getNivel().equalsIgnoreCase(nivFiltro))
            .filter(c -> texto.isEmpty()
                || c.getNombre().toLowerCase().contains(texto)
                || c.getDistrito().toLowerCase().contains(texto)
                || c.getUrbanizacion().toLowerCase().contains(texto)
                || c.getDescripcion().toLowerCase().contains(texto))
            .sorted(Comparator
                .comparing(ComunidadVecinal::isUnido).reversed()
                .thenComparing(ComunidadVecinal::isAccesoPermitido).reversed()
                .thenComparingDouble(c -> c.getDistanciaKm() != null ? c.getDistanciaKm() : 999.0))
            .toList();

        List<ComunidadVecinal> habilitadas = todas.stream()
            .filter(ComunidadVecinal::isAccesoPermitido)
            .sorted(Comparator
                .comparing(ComunidadVecinal::isUnido).reversed()
                .thenComparingDouble(c -> c.getDistanciaKm() != null ? c.getDistanciaKm() : 0.0))
            .toList();

        List<ComunidadVecinal> misGrupos = todas.stream().filter(ComunidadVecinal::isUnido).toList();

        model.addAttribute("comunidades", filtradas);
        model.addAttribute("comunidadesHabilitadas", habilitadas);
        model.addAttribute("misGrupos", misGrupos);
        model.addAttribute("totalUnidos", misGrupos.size());
        model.addAttribute("maxGrupos", MAX_GRUPOS_POR_VECINO);
        model.addAttribute("cupoLleno", misGrupos.size() >= MAX_GRUPOS_POR_VECINO);
        model.addAttribute("distritosArequipa", DISTRITOS_AREQUIPA.keySet());
        model.addAttribute("distritoSeleccionado", distFiltro);
        model.addAttribute("nivelSeleccionado", nivFiltro);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("ubicacionZona", ub.zona);
        model.addAttribute("ubicacionDistrito", ub.distritoDetectado);
        model.addAttribute("ubicacionLat", ub.lat);
        model.addAttribute("ubicacionLng", ub.lng);
        model.addAttribute("usuarioActual", u);
        return "comunidades";
    }

    /** Permite al vecino verificar o cambiar su distrito/localización GPS en Arequipa para desbloquear su grupo vecinal. */
    @PostMapping("/ubicacion")
    public String actualizarUbicacion(@RequestParam(required = false) String distrito,
                                      @RequestParam(required = false) String urbanizacion,
                                      @RequestParam(required = false) Double latitud,
                                      @RequestParam(required = false) Double longitud,
                                      HttpSession session, RedirectAttributes ra) {
        String distElegido = (distrito != null && !distrito.isBlank()) ? distrito.trim() : null;
        Double lat = latitud;
        Double lng = longitud;

        if (lat != null && lng != null && (distElegido == null || distElegido.isEmpty())) {
            distElegido = detectarDistritoMasCercano(lat, lng);
        } else if (distElegido != null && (lat == null || lng == null)) {
            double[] coords = DISTRITOS_AREQUIPA.get(distElegido);
            if (coords != null) {
                lat = coords[0];
                lng = coords[1];
            }
        }

        if (distElegido == null) distElegido = "Tiabaya";
        String nuevaZona = (urbanizacion != null && !urbanizacion.isBlank())
            ? urbanizacion.trim() + " - " + distElegido + ", Arequipa"
            : distElegido + ", Arequipa";

        session.setAttribute("ubicacionZona", nuevaZona);
        session.setAttribute("ubicacionDistrito", distElegido);
        if (lat != null && lng != null) {
            session.setAttribute("ubicacionLat", lat);
            session.setAttribute("ubicacionLng", lng);
        }

        Usuario u = sesion.actual(session);
        if (u != null) {
            u.setZona(nuevaZona);
            if (lat != null && lng != null) {
                u.setLatitud(lat);
                u.setLongitud(lng);
            }
            usuarios.save(u);
        }

        ra.addFlashAttribute("mensaje", "📍 Tu localización actual se actualizó a «" + nuevaZona + "». El mapa y los grupos de WhatsApp de tu zona ya están sincronizados.");
        return "redirect:/comunidades";
    }

    /**
     * Vista del Grupo de WhatsApp Vecinal:
     * 1. Requiere estar conectado con cuenta activa en el sistema web.
     * 2. Requiere pertenecer a la localización del grupo.
     * 3. Respeta el tope máximo de 2 grupos por vecino para evitar congestión.
     */
    @GetMapping("/{id}")
    public String verGrupo(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) {
            ra.addFlashAttribute("error", "🔑 Para acceder a un grupo de WhatsApp vecinal y ver los mensajes debes haber iniciado sesión en el sistema web.");
            return "redirect:/login";
        }

        ComunidadVecinal c = comunidades.findById(id).orElse(null);
        if (c == null) {
            ra.addFlashAttribute("error", "La comunidad solicitada no existe.");
            return "redirect:/comunidades";
        }

        UbicacionVecino ub = obtenerUbicacionVecino(u, session);
        Set<Long> misIds = miembros.findByUsuarioId(u.getId()).stream().map(m -> m.getComunidad().getId()).collect(Collectors.toSet());
        completarEstadoComunidad(c, u, ub, misIds);

        if (!c.isAccesoPermitido()) {
            ra.addFlashAttribute("error", "🔒 Acceso restringido por localización: El grupo «" + c.getNombre()
                + "» es exclusivo para vecinos ubicados en " + c.getDistrito() + " (" + c.getUrbanizacion()
                + "). Tu ubicación actual es «" + ub.zona + "».");
            return "redirect:/comunidades";
        }

        if (!c.isUnido() && misIds.size() >= MAX_GRUPOS_POR_VECINO && !u.isAdmin()) {
            ra.addFlashAttribute("error", "⚠️ Límite anticongestión (máx. " + MAX_GRUPOS_POR_VECINO + " grupos por persona): Ya estás unido a "
                + misIds.size() + " grupos de WhatsApp. Para evitar congestionar los grupos, sal primero de uno de tus grupos actuales si deseas ingresar a «" + c.getNombre() + "».");
            return "redirect:/comunidades";
        }

        List<MensajeComunidad> listaMensajes = mensajes.findByComunidadIdOrderByCreadoEnAsc(id);
        List<MiembroComunidad> listaMiembros = miembros.findByComunidadIdOrderByUnidoEnDesc(id);
        List<ComunidadVecinal> otrosDelDistrito = comunidades.findByDistritoIgnoreCaseOrderByNivelAscNombreAsc(c.getDistrito())
            .stream().filter(o -> !o.getId().equals(c.getId())).toList();

        model.addAttribute("c", c);
        model.addAttribute("mensajes", listaMensajes);
        model.addAttribute("miembros", listaMiembros);
        model.addAttribute("otrosDelDistrito", otrosDelDistrito);
        model.addAttribute("totalUnidos", misIds.size());
        model.addAttribute("maxGrupos", MAX_GRUPOS_POR_VECINO);
        model.addAttribute("ubicacionZona", ub.zona);
        model.addAttribute("usuarioActual", u);
        return "comunidad-detalle";
    }

    /** Unirse al grupo de WhatsApp de la comunidad (validando sesión web, localización y límite de 2 grupos). */
    @PostMapping("/{id}/unirse")
    public String unirse(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) {
            ra.addFlashAttribute("error", "🔑 Inicia sesión en el sistema web para unirte al grupo de WhatsApp de tu comunidad.");
            return "redirect:/login";
        }

        ComunidadVecinal c = comunidades.findById(id).orElse(null);
        if (c == null) return "redirect:/comunidades";

        UbicacionVecino ub = obtenerUbicacionVecino(u, session);
        Set<Long> misIds = miembros.findByUsuarioId(u.getId()).stream().map(m -> m.getComunidad().getId()).collect(Collectors.toSet());
        completarEstadoComunidad(c, u, ub, misIds);

        if (!c.isAccesoPermitido()) {
            ra.addFlashAttribute("error", "🔒 Solo pueden unirse a este grupo de WhatsApp los vecinos ubicados en " + c.getDistrito() + ".");
            return "redirect:/comunidades";
        }

        if (!misIds.contains(id)) {
            if (misIds.size() >= MAX_GRUPOS_POR_VECINO) {
                ra.addFlashAttribute("error", "⚠️ No puedes estar en más de " + MAX_GRUPOS_POR_VECINO
                    + " grupos de WhatsApp a la vez para evitar congestionar las comunidades. Sal de uno de tus grupos actuales si deseas unirte a este.");
                return "redirect:/comunidades";
            }
            miembros.save(new MiembroComunidad(c, u));
            MensajeComunidad bienvenida = new MensajeComunidad();
            bienvenida.setComunidad(c);
            bienvenida.setUsuario(u);
            bienvenida.setAutorNombre(u.getNombreCorto());
            bienvenida.setAutorZona(u.getZona() != null ? u.getZona() : c.getDistrito());
            bienvenida.setTipo("general");
            bienvenida.setContenido("👋 ¡Hola vecinos de " + c.getDistrito() + "! Me acabo de unir al grupo vecinal desde el sistema web Neighborhood.");
            mensajes.save(bienvenida);
        }

        ra.addFlashAttribute("mensaje", "✅ ¡Te uniste al grupo «" + c.getNombre() + "» (" + (misIds.contains(id) ? misIds.size() : misIds.size() + 1) + "/" + MAX_GRUPOS_POR_VECINO + " grupos permitidos)!");
        return "redirect:/comunidades/" + id;
    }

    /** Salir de un grupo de WhatsApp para liberar uno de los 2 cupos permitidos por vecino. */
    @PostMapping("/{id}/salir")
    public String salirDelGrupo(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) return "redirect:/login";

        miembros.findByComunidadIdAndUsuarioId(id, u.getId()).ifPresent(miembros::delete);
        ra.addFlashAttribute("mensaje", "Saliste del grupo de WhatsApp. Ahora tienes un cupo libre para unirte a otra comunidad de tu zona.");
        return "redirect:/comunidades";
    }

    /** Enviar un mensaje al grupo de WhatsApp de la comunidad (requiere sesión en el sistema web y respetar el límite de 2 grupos). */
    @PostMapping("/{id}/mensaje")
    public String enviarMensaje(@PathVariable Long id,
                                @RequestParam String contenido,
                                @RequestParam(defaultValue = "general") String tipo,
                                HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) {
            ra.addFlashAttribute("error", "🔑 Debes estar conectado con tu cuenta en el sistema web para enviar mensajes al grupo.");
            return "redirect:/login";
        }

        ComunidadVecinal c = comunidades.findById(id).orElse(null);
        if (c == null) return "redirect:/comunidades";

        UbicacionVecino ub = obtenerUbicacionVecino(u, session);
        Set<Long> misIds = miembros.findByUsuarioId(u.getId()).stream().map(m -> m.getComunidad().getId()).collect(Collectors.toSet());
        completarEstadoComunidad(c, u, ub, misIds);

        if (!c.isAccesoPermitido()) {
            ra.addFlashAttribute("error", "🔒 No perteneces a la localización de este grupo vecinal (" + c.getDistrito() + ").");
            return "redirect:/comunidades";
        }

        if (!misIds.contains(id)) {
            if (misIds.size() >= MAX_GRUPOS_POR_VECINO) {
                ra.addFlashAttribute("error", "⚠️ Ya perteneces al máximo de " + MAX_GRUPOS_POR_VECINO + " grupos de WhatsApp permitidos.");
                return "redirect:/comunidades";
            }
            miembros.save(new MiembroComunidad(c, u));
        }

        String textoLimpio = Catalogos.limpiar(contenido, 1000);
        if (textoLimpio == null) {
            ra.addFlashAttribute("error", "Escribe un mensaje antes de enviar.");
            return "redirect:/comunidades/" + id;
        }

        MensajeComunidad m = new MensajeComunidad();
        m.setComunidad(c);
        m.setUsuario(u);
        m.setAutorNombre(u.getNombreCorto());
        m.setAutorZona(u.getZona() != null && !u.getZona().isBlank() ? u.getZona() : c.getUrbanizacion());
        m.setTipo(Set.of("general", "alerta", "servicio", "favor").contains(tipo) ? tipo : "general");
        m.setContenido(textoLimpio);
        mensajes.save(m);

        return "redirect:/comunidades/" + id + "#ultimo-mensaje";
    }

    /** Crear un nuevo grupo de WhatsApp para una urbanización o junta vecinal de Arequipa. */
    @PostMapping("/nueva")
    public String crearComunidad(@RequestParam String nombre,
                                 @RequestParam String distrito,
                                 @RequestParam String urbanizacion,
                                 @RequestParam(defaultValue = "Urbanización / Junta Vecinal") String nivel,
                                 @RequestParam String descripcion,
                                 HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) {
            ra.addFlashAttribute("error", "🔑 Inicia sesión en el sistema web para registrar una nueva comunidad.");
            return "redirect:/login";
        }

        long actuales = miembros.countByUsuarioId(u.getId());
        if (actuales >= MAX_GRUPOS_POR_VECINO) {
            ra.addFlashAttribute("error", "⚠️ Ya perteneces a " + MAX_GRUPOS_POR_VECINO + " grupos de WhatsApp (límite máximo por vecino para evitar congestión). Sal de uno antes de crear y unirte a otro.");
            return "redirect:/comunidades";
        }

        String nom = Catalogos.limpiar(nombre, 150);
        String dist = Catalogos.limpiar(distrito, 80);
        String urb = Catalogos.limpiar(urbanizacion, 180);
        String desc = Catalogos.limpiar(descripcion, 550);
        if (nom == null || dist == null || urb == null || desc == null) {
            ra.addFlashAttribute("error", "Completa todos los datos de la comunidad vecinal.");
            return "redirect:/comunidades";
        }

        double[] coords = DISTRITOS_AREQUIPA.getOrDefault(dist, new double[]{-16.4580, -71.5885});
        ComunidadVecinal c = new ComunidadVecinal();
        c.setNombre(nom);
        c.setDistrito(dist);
        c.setUrbanizacion(urb);
        c.setNivel("Distrital".equalsIgnoreCase(nivel) ? "Distrital" : "Urbanización / Junta Vecinal");
        c.setDescripcion(desc);
        String slug = norm(dist + urb).replaceAll("[^a-zA-Z0-9]", "");
        if (slug.length() > 14) slug = slug.substring(0, 14);
        c.setCodigoWhatsapp("chat.whatsapp.com/Nb" + slug + "AQP");
        c.setLatitud(coords[0]);
        c.setLongitud(coords[1]);
        c.setRadioKm(3.8);
        c.setMiembrosBase(24);
        c = comunidades.save(c);

        MensajeComunidad m = new MensajeComunidad();
        m.setComunidad(c);
        m.setUsuario(u);
        m.setAutorNombre(u.getNombreCorto());
        m.setAutorZona(urb + " · " + dist);
        m.setTipo("general");
        m.setContenido("🎉 Bienvenidos al grupo oficial de WhatsApp Vecinal de " + urb + " (" + dist + "). Espacio para avisos de la cuadra, seguridad, oficios recomendados y ayuda mutua.");
        mensajes.save(m);

        miembros.save(new MiembroComunidad(c, u));

        ra.addFlashAttribute("mensaje", "✅ Se creó la comunidad de WhatsApp «" + c.getNombre() + "» en " + dist + ".");
        return "redirect:/comunidades/" + c.getId();
    }

    // ---------- Utilidades estáticas de geolocalización para Registro / Login / Mapa ----------

    /** Devuelve coordenadas [lat, lng] a partir del nombre de la zona/distrito en Arequipa. */
    public static double[] coordenadasParaZona(String zona) {
        String d = detectarDistritoDesdeTexto(zona);
        return DISTRITOS_AREQUIPA.getOrDefault(d, new double[]{-16.4580, -71.5885});
    }

    public static String detectarDistritoDesdeTexto(String texto) {
        if (texto == null || texto.isBlank()) return "Tiabaya";
        String n = norm(texto);
        for (String d : DISTRITOS_AREQUIPA.keySet()) {
            if (n.contains(norm(d))) return d;
        }
        if (n.contains("tunales") || n.contains("alata") || n.contains("patasagua") || n.contains("camarones") || n.contains("cural")) return "Tiabaya";
        if (n.contains("bustamante") || n.contains("lambramani") || n.contains("dolores")) return "J.L. Bustamante y Rivero";
        if (n.contains("hunter") || n.contains("tingo")) return "Jacobo Hunter";
        if (n.contains("selva alegre") || n.contains("chilina") || n.contains("graficos")) return "Alto Selva Alegre";
        if (n.contains("vallecito") || n.contains("centenario") || n.contains("centro")) return "Cercado";
        if (n.contains("umacollo") || n.contains("piedra santa")) return "Yanahuara";
        if (n.contains("tahuaycani") || n.contains("huaranguillo") || n.contains("arancota")) return "Sachaca";
        if (n.contains("lara") || n.contains("campina")) return "Socabaya";
        if (n.contains("challapampa") || n.contains("zamacola")) return "Cerro Colorado";
        if (n.contains("ciudad blanca")) return "Paucarpata";
        if (n.contains("characato") || n.contains("sabandia") || n.contains("yumina")) return "Characato y Sabandía";
        return "Tiabaya";
    }

    public static String detectarDistritoMasCercano(double lat, double lng) {
        String mejor = "Tiabaya";
        double min = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> e : DISTRITOS_AREQUIPA.entrySet()) {
            double d = distanciaKm(lat, lng, e.getValue()[0], e.getValue()[1]);
            if (d < min) {
                min = d;
                mejor = e.getKey();
            }
        }
        return mejor;
    }

    // ---------- Lógica interna de verificación ----------

    private void completarEstadoComunidad(ComunidadVecinal c, Usuario u, UbicacionVecino ub, Set<Long> misIds) {
        long cantMensajes = mensajes.countByComunidadId(c.getId());
        long cantMiembrosBd = miembros.countByComunidadId(c.getId());
        MensajeComunidad ultimo = mensajes.findTopByComunidadIdOrderByCreadoEnDesc(c.getId());

        c.setTotalMensajes(cantMensajes);
        c.setTotalMiembros(c.getMiembrosBase() + (int) cantMiembrosBd);
        if (ultimo != null) {
            c.setUltimoMensajeAutor(ultimo.getAutorNombre());
            c.setUltimoMensajeTexto(ultimo.getContenido());
        }

        boolean unido = misIds.contains(c.getId());
        c.setUnido(unido);

        Double dist = null;
        if (ub.lat != null && ub.lng != null && c.getLatitud() != null && c.getLongitud() != null) {
            dist = distanciaKm(ub.lat, ub.lng, c.getLatitud(), c.getLongitud());
            c.setDistanciaKm(dist);
        }

        boolean coincideDistrito = coincideTextoZona(ub.zona, c.getDistrito())
            || coincideTextoZona(ub.distritoDetectado, c.getDistrito())
            || coincideTextoZona(ub.zona, c.getUrbanizacion());

        boolean dentroRadioGps = dist != null && dist <= c.getRadioKm();
        boolean esAdmin = u != null && u.isAdmin();

        if (coincideDistrito) {
            c.setAccesoPermitido(true);
            c.setRazonAcceso("Vives en " + c.getDistrito() + " (Localización verificada)");
        } else if (dentroRadioGps) {
            c.setAccesoPermitido(true);
            c.setRazonAcceso("Te encuentras dentro del radio vecinal (" + c.getDistanciaFormato() + ")");
        } else if (unido || esAdmin) {
            c.setAccesoPermitido(true);
            c.setRazonAcceso(esAdmin ? "Acceso de moderación" : "Miembro verificado de la comunidad");
        } else {
            c.setAccesoPermitido(false);
            c.setRazonAcceso("Exclusivo para vecinos de " + c.getDistrito());
        }
    }

    private UbicacionVecino obtenerUbicacionVecino(Usuario u, HttpSession session) {
        String zonaSesion = (String) session.getAttribute("ubicacionZona");
        String distSesion = (String) session.getAttribute("ubicacionDistrito");
        Double latSesion = (Double) session.getAttribute("ubicacionLat");
        Double lngSesion = (Double) session.getAttribute("ubicacionLng");

        String zona = null;
        Double lat = null;
        Double lng = null;

        if (u != null) {
            if (u.getZona() != null && !u.getZona().isBlank()) zona = u.getZona();
            if (u.getLatitud() != null) lat = u.getLatitud();
            if (u.getLongitud() != null) lng = u.getLongitud();
        }
        if (zonaSesion != null && !zonaSesion.isBlank()) zona = zonaSesion;
        if (latSesion != null) lat = latSesion;
        if (lngSesion != null) lng = lngSesion;

        if (zona == null || zona.isBlank()) {
            zona = "Tiabaya, Arequipa";
        }
        String dist = distSesion != null ? distSesion : detectarDistritoDesdeTexto(zona);
        if (lat == null || lng == null) {
            double[] coords = DISTRITOS_AREQUIPA.getOrDefault(dist, new double[]{-16.4580, -71.5885});
            lat = coords[0];
            lng = coords[1];
        }
        if (dist == null || dist.isBlank()) {
            dist = detectarDistritoMasCercano(lat, lng);
        }
        return new UbicacionVecino(zona, dist, lat, lng);
    }

    private static boolean coincideTextoZona(String zonaUsuario, String objetivo) {
        if (zonaUsuario == null || objetivo == null || zonaUsuario.isBlank() || objetivo.isBlank()) return false;
        String z = norm(zonaUsuario);
        String o = norm(objetivo);
        return z.contains(o) || o.contains(z);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .trim();
    }

    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record UbicacionVecino(String zona, String distritoDetectado, Double lat, Double lng) {}
}
