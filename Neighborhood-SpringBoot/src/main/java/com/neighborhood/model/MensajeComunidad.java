package com.neighborhood.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "mensajes_comunidad")
public class MensajeComunidad {
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private ComunidadVecinal comunidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String autorNombre;

    @Column(length = 120)
    private String autorZona;

    /** "general", "alerta", "servicio", "favor" */
    @Column(nullable = false, length = 30)
    private String tipo = "general";

    @Column(nullable = false, length = 1000)
    private String contenido;

    private LocalDateTime creadoEn = LocalDateTime.now();

    public MensajeComunidad() {}

    public Long getId() { return id; }
    public ComunidadVecinal getComunidad() { return comunidad; }
    public void setComunidad(ComunidadVecinal comunidad) { this.comunidad = comunidad; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAutorNombre() { return autorNombre; }
    public void setAutorNombre(String autorNombre) { this.autorNombre = autorNombre; }
    public String getAutorZona() { return autorZona; }
    public void setAutorZona(String autorZona) { this.autorZona = autorZona; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getFechaHora() {
        return creadoEn == null ? "" : creadoEn.format(HORA_FMT);
    }

    public String getInicial() {
        if (autorNombre == null || autorNombre.isBlank()) return "V";
        return autorNombre.trim().substring(0, 1).toUpperCase();
    }
}
