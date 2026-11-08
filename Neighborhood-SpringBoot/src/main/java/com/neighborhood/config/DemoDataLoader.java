package com.neighborhood.config;

import com.neighborhood.model.*;
import com.neighborhood.repository.*;
import com.neighborhood.util.Catalogos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Crea (1) la cuenta de administración si no existe y (2) datos de demostración si la base está vacía,
 * para poder mostrar de inmediato todas las funciones conectadas. Se desactiva con
 * neighborhood.demo-data=false en application.properties.
 */
@Component
public class DemoDataLoader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final ValoracionServicioRepository valoraciones;
    private final FavorRepository favores;
    private final EmergenciaRepository emergencias;
    private final GrupoApoyoRepository grupos;
    private final RespuestaAyudaRepository respuestas;
    private final EventoSeguimientoRepository eventos;
    private final PasswordEncoder encoder;
    @Value("${neighborhood.demo-data:true}") private boolean demo;
    @Value("${neighborhood.admin.correo:admin@neighborhood.pe}") private String adminCorreo;
    @Value("${neighborhood.admin.password:Admin12345}") private String adminPassword;

    public DemoDataLoader(UsuarioRepository usuarios, ServicioRepository servicios, ValoracionServicioRepository valoraciones,
                          FavorRepository favores, EmergenciaRepository emergencias, GrupoApoyoRepository grupos,
                          RespuestaAyudaRepository respuestas, EventoSeguimientoRepository eventos, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.servicios = servicios; this.valoraciones = valoraciones; this.favores = favores;
        this.emergencias = emergencias; this.grupos = grupos; this.respuestas = respuestas; this.eventos = eventos; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        boolean vacia = usuarios.count() == 0;
        if (!usuarios.existsByCorreo(adminCorreo)) {
            Usuario a = nuevoUsuario("Administración Neighborhood", adminCorreo, adminPassword, "Arequipa", -16.4090, -71.5375);
            a.setRol("ADMIN"); usuarios.save(a);
            log.info("Cuenta de administración creada: {} (cambia la contraseña en application.properties).", adminCorreo);
        }
        if (demo && vacia) cargarDemo();
    }

    private Usuario nuevoUsuario(String nombre, String correo, String pass, String zona, double lat, double lng) {
        Usuario u = new Usuario();
        u.setNombre(nombre); u.setCorreo(correo); u.setPassword(encoder.encode(pass)); u.setZona(zona);
        u.setLatitud(lat); u.setLongitud(lng);
        return u;
    }

    private Usuario vecino(String nombre, String correo, String zona, double lat, double lng) {
        return usuarios.save(nuevoUsuario(nombre, correo, "demo1234", zona, lat, lng));
    }

    private PublicacionServicio servicio(Usuario u, String titulo, String tipo, String categoria, String desc, Double precio, double lat, double lng) {
        PublicacionServicio s = new PublicacionServicio();
        s.setUsuario(u); s.setTitulo(titulo); s.setTipo(tipo); s.setCategoria(categoria); s.setDescripcion(desc);
        s.setPrecio(precio); s.setLatitud(lat); s.setLongitud(lng);
        return servicios.save(s);
    }

    private void voto(PublicacionServicio s, Usuario u, int puntos, String comentario, int diasAtras) {
        ValoracionServicio v = new ValoracionServicio();
        v.setServicio(s); v.setUsuario(u); v.setPuntuacion(puntos); v.setComentario(comentario);
        v.setCreadoEn(LocalDateTime.now().minusDays(diasAtras));
        valoraciones.save(v);
    }

    private Favor favor(Usuario u, String tipo, String categoria, String titulo, String desc, String disp, double lat, double lng) {
        Favor f = new Favor();
        f.setUsuario(u); f.setTipo(tipo); f.setCategoria(categoria); f.setTitulo(titulo); f.setDescripcion(desc);
        f.setDisponibilidad(disp); f.setLatitud(lat); f.setLongitud(lng);
        f = favores.save(f);
        eventos.save(new EventoSeguimiento("favor", f.getId(), (Catalogos.TIPO_OFREZCO.equals(tipo) ? "Oferta publicada por " : "Solicitud publicada por ") + u.getNombreCorto()));
        return f;
    }

    private RespuestaAyuda respuesta(String tipo, Long id, Usuario u, String msg, String estado) {
        RespuestaAyuda r = new RespuestaAyuda();
        r.setObjetivoTipo(tipo); r.setObjetivoId(id); r.setUsuario(u); r.setMensaje(msg); r.setEstado(estado);
        r = respuestas.save(r);
        eventos.save(new EventoSeguimiento(tipo, id, u.getNombreCorto() + " se ofreció a ayudar"));
        return r;
    }

    private void cargarDemo() {
        // Zona de referencia: Tiabaya y alrededores (Arequipa).
        Usuario rosa = vecino("Rosa Quispe", "rosa@demo.pe", "Tiabaya, Arequipa", -16.4585, -71.5890);
        rosa.setAyudasCompletadas(5); rosa.setTelefono("+51 987 654 321"); usuarios.save(rosa);
        Usuario carlos = vecino("Carlos Mamani", "carlos@demo.pe", "Tiabaya, Arequipa", -16.4560, -71.5860);
        carlos.setAyudasCompletadas(2); carlos.setTelefono("+51 987 111 222"); usuarios.save(carlos);
        Usuario lucia = vecino("Lucía Huamán", "lucia@demo.pe", "Tiabaya, Arequipa", -16.4610, -71.5915);
        lucia.setAyudasCompletadas(3); lucia.setTelefono("+51 987 333 444"); usuarios.save(lucia);
        Usuario jorge = vecino("Jorge Ccori", "jorge@demo.pe", "Sachaca, Arequipa", -16.4200, -71.5640);
        Usuario marta = vecino("Marta Flores", "marta@demo.pe", "Tiabaya, Arequipa", -16.4570, -71.5875);
        marta.setAyudasCompletadas(1); marta.setTelefono("+51 987 555 666"); usuarios.save(marta);

        PublicacionServicio gas = servicio(rosa, "Gasfitería a domicilio", "servicio", "Gasfitería",
            "Reparación de fugas, cambio de caños y mantenimiento de termas. Atiendo en Tiabaya y zonas cercanas.", 40.0, -16.4585, -71.5890);
        PublicacionServicio elec = servicio(carlos, "Electricista residencial", "servicio", "Electricidad",
            "Instalaciones, tableros y revisión de cortocircuitos. Presupuesto sin costo.", 50.0, -16.4560, -71.5860);
        PublicacionServicio clases = servicio(lucia, "Clases de matemática y comunicación", "servicio", "Clases y tutorías",
            "Refuerzo escolar para primaria y secundaria, a domicilio o en mi casa.", 20.0, -16.4610, -71.5915);
        PublicacionServicio carp = servicio(jorge, "Carpintería y muebles a medida", "servicio", "Carpintería",
            "Puertas, closets y reparación de muebles.", null, -16.4200, -71.5640);
        servicio(marta, "Queso y yogurt artesanal", "producto", "Alimentos",
            "Productos de mi chacra, entrega en el barrio.", 12.0, -16.4570, -71.5875);

        voto(gas, carlos, 5, "Resolvió una fuga el mismo día. Muy puntual.", 6);
        voto(gas, lucia, 5, "Trabajo limpio y precio justo.", 4);
        voto(gas, marta, 4, "Buen servicio, recomendable.", 2);
        voto(elec, rosa, 5, "Muy ordenado y explicó todo.", 9);
        voto(elec, marta, 4, "Cumplió con el horario acordado.", 3);
        voto(clases, rosa, 5, "Mi hijo mejoró sus notas.", 12);
        voto(carp, rosa, 3, "Buen trabajo, tardó un poco.", 20);

        GrupoApoyo brigada = new GrupoApoyo();
        brigada.setUsuario(carlos); brigada.setNombre("Brigada vecinal de Tiabaya"); brigada.setTipo("Grupo vecinal");
        brigada.setCategoria("Rescate y seguridad"); brigada.setDescripcion("Vecinos voluntarios con botiquín y linternas para apoyar en sismos, lluvias o huaicos.");
        brigada.setZonaCobertura("Tiabaya"); brigada.setDisponibilidad("24 horas (coordinar por llamada)");
        brigada.setLatitud(-16.4575); brigada.setLongitud(-71.5880); brigada.setVerificado(true);
        grupos.save(brigada);
        GrupoApoyo olla = new GrupoApoyo();
        olla.setUsuario(marta); olla.setNombre("Olla común Sector A"); olla.setTipo("Servicio comunitario");
        olla.setCategoria("Alimentos y agua"); olla.setDescripcion("Raciones de comida para familias que lo necesiten. Se atiende con coordinación previa.");
        olla.setZonaCobertura("Sector A"); olla.setDisponibilidad("Lunes a sábado, 12:00 a 14:00");
        olla.setLatitud(-16.4590); olla.setLongitud(-71.5900);
        grupos.save(olla);

        // Cadena de favores: una necesidad, una oferta y una ya en coordinación.
        favor(rosa, Catalogos.TIPO_SOLICITO, "Herramientas", "Necesito una escalera por un día",
            "Para pintar el frente de mi casa. La devuelvo mañana limpia.", "Esta semana", -16.4585, -71.5890);
        favor(jorge, Catalogos.TIPO_OFREZCO, "Herramientas", "Presto escalera y taladro",
            "Tengo una escalera de 3 m y un taladro. Los presto a vecinos con cuidado y devolución al día siguiente.", "Fines de semana", -16.4200, -71.5640);
        Favor movilidad = favor(lucia, Catalogos.TIPO_SOLICITO, "Transporte", "Movilidad al centro de salud",
            "Mi mamá necesita ir al control el viernes en la mañana.", "Viernes en la mañana", -16.4610, -71.5915);
        RespuestaAyuda acept = respuesta("favor", movilidad.getId(), carlos, "Tengo auto, puedo llevarla.", RespuestaAyuda.ACEPTADA);
        movilidad.setEstado(Catalogos.ESTADO_COORDINANDO); movilidad.setResponsable(carlos); favores.save(movilidad);
        eventos.save(new EventoSeguimiento("favor", movilidad.getId(), lucia.getNombreCorto() + " aceptó la ayuda de " + carlos.getNombreCorto()));

        // Emergencias (simulación para la demostración).
        ReporteEmergencia alimentos = new ReporteEmergencia();
        alimentos.setUsuario(marta); alimentos.setCategoria("Alimentos y agua");
        alimentos.setTitulo("[Simulación] Necesidad de alimentos");
        alimentos.setDescripcion("Ejemplo de solicitud y seguimiento; no corresponde a un reporte real.");
        alimentos.setLatitud(-16.459); alimentos.setLongitud(-71.590);
        alimentos = emergencias.save(alimentos);
        eventos.save(new EventoSeguimiento("emergencia", alimentos.getId(), "Necesidad reportada por " + marta.getNombreCorto()));
        respuesta("emergencia", alimentos.getId(), rosa, "Puedo llevar víveres.", RespuestaAyuda.OFRECIDA);
        respuesta("emergencia", alimentos.getId(), lucia, "Tengo agua embotellada.", RespuestaAyuda.OFRECIDA);
        respuesta("emergencia", alimentos.getId(), carlos, "Puedo ayudar a repartir.", RespuestaAyuda.OFRECIDA);
        log.info("Datos de demostración cargados. Vecinos demo: rosa@demo.pe, carlos@demo.pe, lucia@demo.pe, jorge@demo.pe, marta@demo.pe (clave: demo1234).");
    }
}
