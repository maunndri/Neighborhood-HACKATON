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
    private final ComunidadVecinalRepository comunidades;
    private final MensajeComunidadRepository mensajesComunidad;
    private final MiembroComunidadRepository miembrosComunidad;
    private final PasswordEncoder encoder;
    @Value("${neighborhood.demo-data:true}") private boolean demo;
    @Value("${neighborhood.admin.correo:admin@neighborhood.pe}") private String adminCorreo;
    @Value("${neighborhood.admin.password:Admin12345}") private String adminPassword;

    public DemoDataLoader(UsuarioRepository usuarios, ServicioRepository servicios, ValoracionServicioRepository valoraciones,
                          FavorRepository favores, EmergenciaRepository emergencias, GrupoApoyoRepository grupos,
                          RespuestaAyudaRepository respuestas, EventoSeguimientoRepository eventos,
                          ComunidadVecinalRepository comunidades, MensajeComunidadRepository mensajesComunidad,
                          MiembroComunidadRepository miembrosComunidad, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.servicios = servicios; this.valoraciones = valoraciones; this.favores = favores;
        this.emergencias = emergencias; this.grupos = grupos; this.respuestas = respuestas; this.eventos = eventos;
        this.comunidades = comunidades; this.mensajesComunidad = mensajesComunidad;
        this.miembrosComunidad = miembrosComunidad; this.encoder = encoder;
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
        if (demo && comunidades.count() == 0) cargarComunidadesArequipa();
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

    private ComunidadVecinal crearComunidad(String nombre, String distrito, String urbanizacion, String nivel,
                                            String desc, String codigoWa, double lat, double lng, double radioKm, int miembrosBase) {
        ComunidadVecinal c = new ComunidadVecinal();
        c.setNombre(nombre);
        c.setDistrito(distrito);
        c.setUrbanizacion(urbanizacion);
        c.setNivel(nivel);
        c.setDescripcion(desc);
        c.setCodigoWhatsapp(codigoWa);
        c.setLatitud(lat);
        c.setLongitud(lng);
        c.setRadioKm(radioKm);
        c.setMiembrosBase(miembrosBase);
        return comunidades.save(c);
    }

    private void msgComunidad(ComunidadVecinal c, Usuario u, String autorNombre, String autorZona, String tipo, String texto, int minutosAtras) {
        MensajeComunidad m = new MensajeComunidad();
        m.setComunidad(c);
        m.setUsuario(u);
        m.setAutorNombre(autorNombre);
        m.setAutorZona(autorZona);
        m.setTipo(tipo);
        m.setContenido(texto);
        m.setCreadoEn(LocalDateTime.now().minusMinutes(minutosAtras));
        mensajesComunidad.save(m);
    }

    private void cargarComunidadesArequipa() {
        Usuario rosa = usuarios.findByCorreo("rosa@demo.pe").orElse(null);
        Usuario carlos = usuarios.findByCorreo("carlos@demo.pe").orElse(null);
        Usuario lucia = usuarios.findByCorreo("lucia@demo.pe").orElse(null);
        Usuario jorge = usuarios.findByCorreo("jorge@demo.pe").orElse(null);
        Usuario marta = usuarios.findByCorreo("marta@demo.pe").orElse(null);

        // 1. TIABAYA (Distrital + Urbanizaciones)
        ComunidadVecinal tiabayaDist = crearComunidad(
            "Comunidad WhatsApp · Distrito de Tiabaya (Red Central)",
            "Tiabaya", "Tiabaya Cercado, Av. Arequipa y Plaza Principal", "Distrital",
            "Grupo vecinal oficial de todo el distrito de Tiabaya para avisos municipales, cortes de agua/luz, oficios con Visto Bueno y alertas SOS.",
            "chat.whatsapp.com/NbTiabayaCentralAQP", -16.4580, -71.5885, 4.2, 214);

        msgComunidad(tiabayaDist, rosa, "Rosa Q.", "Tiabaya Cercado", "servicio",
            "🔧 Buenos días vecinos de Tiabaya, hoy estaré atendiendo reparaciones de gasfitería y fugas de agua a domicilio en toda la Av. Arequipa y alrededores.", 185);
        msgComunidad(tiabayaDist, carlos, "Carlos M.", "Brigada Tiabaya", "alerta",
            "🚨 Aviso vecinal: SEDAPAR informó baja presión de agua hoy de 2:00 p. m. a 5:00 p. m. en la parte alta de Tiabaya. Recomendamos juntar agua.", 120);
        msgComunidad(tiabayaDist, marta, "Marta F.", "Sector A · Tiabaya", "general",
            "🧀 Vecinos, ya salió el queso fresco y yogurt natural de chacra. Estoy a dos cuadras de la plaza de Tiabaya sin costo de pasaje.", 65);
        msgComunidad(tiabayaDist, lucia, "Lucía H.", "Tiabaya", "favor",
            "🤝 Muchas gracias a don Carlos M. por apoyar con la movilidad para llevar a mi mamá al Centro de Salud de Tiabaya. ¡Qué gran comunidad!", 25);

        if (rosa != null) miembrosComunidad.save(new MiembroComunidad(tiabayaDist, rosa));
        if (carlos != null) miembrosComunidad.save(new MiembroComunidad(tiabayaDist, carlos));
        if (lucia != null) miembrosComunidad.save(new MiembroComunidad(tiabayaDist, lucia));
        if (marta != null) miembrosComunidad.save(new MiembroComunidad(tiabayaDist, marta));

        ComunidadVecinal tiabayaTunales = crearComunidad(
            "Junta Vecinal WhatsApp · Urb. Los Tunales, San José y El Cural",
            "Tiabaya", "Urb. Los Tunales, Pueblo Tradicional San José y El Cural", "Urbanización / Junta Vecinal",
            "Coordinación directa de vecinos de Los Tunales, San José y El Cural: rondas nocturnas, préstamo de herramientas y apoyo a adultos mayores.",
            "chat.whatsapp.com/NbLosTunalesTiabaya", -16.4535, -71.5825, 3.8, 96);

        msgComunidad(tiabayaTunales, carlos, "Carlos M.", "San José · Tiabaya", "general",
            "🔦 Vecinos de Los Tunales y San José: hoy a las 8:00 p. m. probaremos la alarma comunitaria de la cuadra 3. No se alarmen, es prueba preventiva.", 210);
        msgComunidad(tiabayaTunales, rosa, "Rosa Q.", "Tiabaya", "favor",
            "🪜 ¿Alguien en Los Tunales tendrá una escalera de tijera por esta tarde? Es para pintar la fachada, la devuelvo hoy mismo.", 45);

        ComunidadVecinal tiabayaAlata = crearComunidad(
            "Vecinos Unidos WhatsApp · Alata, Patasagua y Pampa de Camarones",
            "Tiabaya", "Alata, Patasagua, Los Perales y Pampa de Camarones", "Urbanización / Junta Vecinal",
            "Grupo de comunicación rápida para las familias de Alata, Patasagua y Pampa de Camarones en Tiabaya.",
            "chat.whatsapp.com/NbAlataPatasaguaAQP", -16.4620, -71.5940, 3.8, 118);

        msgComunidad(tiabayaAlata, lucia, "Lucía H.", "Pampa de Camarones", "servicio",
            "📚 Vecinos de Alata y Pampa de Camarones: abrí cupos para refuerzo escolar de primaria y secundaria por las tardes.", 95);
        msgComunidad(tiabayaAlata, null, "Pedro V.", "Patasagua · Tiabaya", "alerta",
            "🐕 Se encontró un perrito pequeño con collar azul cerca del paradero de Patasagua. Está a buen recaudo en la tienda de la esquina.", 30);

        // 2. SACHACA
        ComunidadVecinal sachaca = crearComunidad(
            "Comunidad WhatsApp · Sachaca, Tahuaycani y Huaranguillo",
            "Sachaca", "Urb. Tahuaycani, Huaranguillo, Arancota y Sachaca Tradicional", "Distrital",
            "Red vecinal de Sachaca para seguridad ciudadana, trueque de herramientas, carpintería y emergencias.",
            "chat.whatsapp.com/NbSachacaTahuaycani", -16.4260, -71.5650, 3.8, 164);
        msgComunidad(sachaca, jorge, "Jorge C.", "Sachaca", "servicio",
            "🪚 Vecinos de Sachaca y Tahuaycani: realizo trabajos de carpintería y arreglo de puertas a domicilio, y presto taladro los fines de semana.", 140);
        msgComunidad(sachaca, null, "Elena R.", "Urb. Tahuaycani", "general",
            "🚛 El camión reciclador de la municipalidad de Sachaca está pasando en este momento por Tahuaycani.", 50);
        if (jorge != null) miembrosComunidad.save(new MiembroComunidad(sachaca, jorge));

        // 3. CERCADO DE AREQUIPA
        ComunidadVecinal cercado = crearComunidad(
            "Junta Vecinal WhatsApp · Cercado, Vallecito y IV Centenario",
            "Cercado", "Urb. Vallecito, IV Centenario, Barrio Obrero y Centro Histórico", "Distrital",
            "Grupo oficial de residentes del Cercado de Arequipa para avisos de tránsito, seguridad en parques y oficios verificados.",
            "chat.whatsapp.com/NbCercadoVallecitoAQP", -16.4090, -71.5375, 3.6, 245);
        msgComunidad(cercado, null, "Roberto D.", "Urb. Vallecito", "alerta",
            "🚧 Atención vecinos de Vallecito: cerraron temporalmente una vía por mantenimiento de adoquines cerca del puente Grau.", 110);
        msgComunidad(cercado, null, "Carmen L.", "IV Centenario", "favor",
            "🤝 Ofrezco clases gratuitas de tejido para adultos mayores del barrio los sábados en la mañana.", 40);

        // 4. YANAHUARA
        ComunidadVecinal yanahuara = crearComunidad(
            "Comunidad WhatsApp · Yanahuara Tradicional, Umacollo y Piedra Santa",
            "Yanahuara", "Yanahuara Tradicional, Urb. Umacollo, Piedra Santa y Magisterial", "Distrital",
            "Comunidad conectada de Yanahuara: vigilancia vecinal, paseo de mascotas, electricistas y apoyo a vecinos mayores.",
            "chat.whatsapp.com/NbYanahuaraUmacollo", -16.3920, -71.5460, 3.6, 198);
        msgComunidad(yanahuara, null, "Gabriela S.", "Urb. Umacollo", "servicio",
            "🐾 Vecinos de Umacollo y Yanahuara: realizo paseos de mascotas por las mañanas en el parque con referencias de vecinos de la cuadra.", 80);

        // 5. CAYMA
        ComunidadVecinal cayma = crearComunidad(
            "Red Vecinal WhatsApp · Cayma, Bello Horizonte y Deán Valdivia",
            "Cayma", "Cayma Tradicional, Urb. Bello Horizonte, Carmen Alto y Deán Valdivia", "Distrital",
            "Grupo distrital de Cayma para emergencias en temporada de lluvias, brigadas solidarias y servicios del barrio.",
            "chat.whatsapp.com/NbCaymaVecinosAQP", -16.3820, -71.5480, 4.0, 276);
        msgComunidad(cayma, null, "Luis P.", "Deán Valdivia · Cayma", "alerta",
            "🌧️ Brigada vecinal de Cayma activa: estamos revisando torrenteras y repartiendo sacos terreros a adultos mayores que lo soliciten.", 75);

        // 6. CERRO COLORADO
        ComunidadVecinal cerroColorado = crearComunidad(
            "Comunidad WhatsApp · Cerro Colorado, Challapampa y La Libertad",
            "Cerro Colorado", "Urb. Challapampa, La Libertad, Cerro Viejo y Zamácola", "Distrital",
            "Comunidad de vecinos de Cerro Colorado para coordinación de juntas vecinales, seguridad y economía local sin transporte.",
            "chat.whatsapp.com/NbCerroColoradoAQP", -16.3740, -71.5640, 4.5, 312);
        msgComunidad(cerroColorado, null, "Fernando T.", "Challapampa", "servicio",
            "⚡ Electricista y técnico en termas solares disponible hoy para los vecinos de Challapampa y La Libertad.", 90);

        // 7. J.L. BUSTAMANTE Y RIVERO
        ComunidadVecinal bustamante = crearComunidad(
            "Junta Vecinal WhatsApp · J.L. Bustamante, Dolores y Lambramani",
            "J.L. Bustamante y Rivero", "Urb. Parque Lambramani, Dolores, Simón Bolívar y 13 de Enero", "Distrital",
            "Grupo vecinal de José Luis Bustamante y Rivero para alertas de serenazgo, comercio de barrio y banco de tiempo.",
            "chat.whatsapp.com/NbBustamanteRivero", -16.4290, -71.5190, 3.6, 229);
        msgComunidad(bustamante, null, "Patricia M.", "Urb. Dolores", "general",
            "🌱 Este domingo haremos jornada de limpieza y poda comunitaria en el parque de nuestra urbanización.", 60);

        // 8. PAUCARPATA
        ComunidadVecinal paucarpata = crearComunidad(
            "Red Solidaria WhatsApp · Paucarpata, Ciudad Blanca y 15 de Agosto",
            "Paucarpata", "Ciudad Blanca, 15 de Agosto, Israel y Paucarpata Tradicional", "Distrital",
            "Red comunitaria de Paucarpata con ollas comunes, brigadas de primeros auxilios y directorio de oficios locales.",
            "chat.whatsapp.com/NbPaucarpataUnida", -16.4250, -71.5030, 4.0, 289);
        msgComunidad(paucarpata, null, "Sonia H.", "Ciudad Blanca · Paucarpata", "favor",
            "🍲 Vecinos de Ciudad Blanca: tenemos botiquín comunitario y tensiómetro disponible para controlar la presión a adultos mayores.", 55);

        // 9. MIRAFLORES
        ComunidadVecinal miraflores = crearComunidad(
            "Vecinos Unidos WhatsApp · Miraflores y Feria del Altiplano",
            "Miraflores", "Miraflores Cercado, Alameda Salaverry y Alto Misti", "Distrital",
            "Grupo vecinal del distrito de Miraflores para avisos comunitarios, cerrajería, gasfitería y alertas rápidas.",
            "chat.whatsapp.com/NbMirafloresAQP", -16.3940, -71.5170, 3.6, 174);
        msgComunidad(miraflores, null, "Héctor Q.", "Miraflores", "servicio",
            "🔑 Cerrajería de emergencia 24h a pie dentro del distrito de Miraflores.", 100);

        // 10. ALTO SELVA ALEGRE
        ComunidadVecinal asa = crearComunidad(
            "Comunidad WhatsApp · Alto Selva Alegre, Chilina e Independencia",
            "Alto Selva Alegre", "Urb. Gráficos, Chilina, Apurímac e Independencia", "Distrital",
            "Comunidad de Alto Selva Alegre enfocada en prevención de emergencias, apoyo vecinal y oficios de la cuadra.",
            "chat.whatsapp.com/NbAltoSelvaAlegre", -16.3810, -71.5220, 3.8, 190);
        msgComunidad(asa, null, "Gladys C.", "Urb. Gráficos · ASA", "general",
            "📦 Vecinos, estamos organizando acopio de frazadas para las zonas altas del distrito.", 130);

        // 11. SOCABAYA
        ComunidadVecinal socabaya = crearComunidad(
            "Junta Vecinal WhatsApp · Socabaya, Lara y San Martín",
            "Socabaya", "Urb. Lara, San Martín de Socabaya, La Campiña y 4 de Octubre", "Distrital",
            "Grupo de vecinos de Socabaya y Urb. Lara para compartir avisos, movilidad solidaria y recomendaciones de servicios.",
            "chat.whatsapp.com/NbSocabayaLaraAQP", -16.4580, -71.5280, 4.0, 182);
        msgComunidad(socabaya, null, "Miguel A.", "Urb. Lara · Socabaya", "favor",
            "🚗 Salgo mañana a las 7:00 a. m. desde Urb. Lara hacia el centro; tengo 3 asientos libres para vecinos.", 70);

        // 12. JACOBO HUNTER
        ComunidadVecinal hunter = crearComunidad(
            "Comunidad WhatsApp · Jacobo Hunter, Tingo y Pampa del Cusco",
            "Jacobo Hunter", "Tingo, Pampa del Cusco, Bellavista y UPIS Paisajista", "Distrital",
            "Red vecinal de Jacobo Hunter y Tingo conectada con brigadas comunitarias y comercios de la manzana.",
            "chat.whatsapp.com/NbJacoboHunterAQP", -16.4450, -71.5550, 3.6, 167);
        msgComunidad(hunter, null, "Raúl Z.", "Tingo · Hunter", "general",
            "📢 Hoy habrá campaña de vacunación antirrábica canina en la losa deportiva del sector.", 85);

        // 13. MARIANO MELGAR
        ComunidadVecinal marianoMelgar = crearComunidad(
            "Red Vecinal WhatsApp · Mariano Melgar, Santa Rosa y Atalaya",
            "Mariano Melgar", "Urb. Santa Rosa, San Lorenzo, Generalísimo San Martín y Atalaya", "Distrital",
            "Grupo de coordinación vecinal de Mariano Melgar para alertas, préstamos sin dinero y servicios cercanos.",
            "chat.whatsapp.com/NbMarianoMelgarAQP", -16.4040, -71.5030, 3.6, 159);
        msgComunidad(marianoMelgar, null, "Verónica P.", "Urb. Santa Rosa", "servicio",
            "🥟 Venta de empanadas y pan artesanal recién horneado para los vecinos de Santa Rosa.", 48);

        // 14. UCHUMAYO
        ComunidadVecinal uchumayo = crearComunidad(
            "Comunidad WhatsApp · Uchumayo, Congata y Cerro Verde",
            "Uchumayo", "Pueblo Tradicional de Uchumayo, Congata y El Huayco", "Distrital",
            "Comunidad de vecinos de Uchumayo y Congata para alertas de carretera, agua potable y ayuda solidaria.",
            "chat.whatsapp.com/NbUchumayoCongata", -16.4220, -71.6300, 4.5, 134);
        msgComunidad(uchumayo, null, "Óscar V.", "Congata · Uchumayo", "general",
            "💧 Cisterna municipal repartiendo agua potable en la plaza de Congata hasta el mediodía.", 62);

        // 15. CHARACATO Y SABANDÍA
        ComunidadVecinal characato = crearComunidad(
            "Vecinos WhatsApp · Characato y Sabandía Tradicional",
            "Characato y Sabandía", "Characato Tradicional, Sabandía, Yumina y Cerrillo", "Distrital",
            "Grupo de vecinos de la cuenca suroriental (Characato y Sabandía) para productos agrícolas locales y seguridad rural.",
            "chat.whatsapp.com/NbCharacatoSabandia", -16.4670, -71.4880, 4.5, 121);
        msgComunidad(characato, null, "Aurelio M.", "Characato", "servicio",
            "🌽 Cosecha fresca de choclo, habas y verduras de chacra directo para vecinos de Characato y Sabandía.", 115);

        log.info("17 Comunidades de WhatsApp Vecinales de Arequipa (por Distrito y Urbanización) cargadas.");
    }
}
