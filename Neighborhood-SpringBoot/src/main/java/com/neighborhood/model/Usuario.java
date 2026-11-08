package com.neighborhood.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, unique = true, length = 160)
    private String correo;
    @Column(nullable = false)
    private String password;
    @Column(length = 160)
    private String zona;
    @Column(length = 40)
    private String telefono;
    private Double latitud;
    private Double longitud;
    /** VECINO o ADMIN. */
    @Column(length = 20)
    private String rol = "VECINO";
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean suspendido = false;
    /** Reputación solidaria: ayudas que el vecino completó y el solicitante confirmó. */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int ayudasCompletadas = 0;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Usuario() {}
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    /** Nombre y primera inicial del apellido, para respetar la privacidad en recomendaciones públicas. */
    public String getNombreCorto() {
        if (nombre == null || nombre.isBlank()) return "Vecino";
        String[] p = nombre.trim().split("\\s+");
        return p.length > 1 ? p[0] + " " + p[1].substring(0, 1).toUpperCase() + "." : p[0];
    }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelefono() { return telefono; }
    public String getTelefonoWhatsapp() { if (telefono == null) return null; String n=telefono.replaceAll("\\D", ""); return n.length()==9 ? "51"+n : n; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public String getRol() { return rol == null ? "VECINO" : rol; }
    public void setRol(String rol) { this.rol = rol; }
    public boolean isAdmin() { return "ADMIN".equals(getRol()); }
    public boolean isSuspendido() { return suspendido; }
    public void setSuspendido(boolean suspendido) { this.suspendido = suspendido; }
    public int getAyudasCompletadas() { return ayudasCompletadas; }
    public void setAyudasCompletadas(int ayudasCompletadas) { this.ayudasCompletadas = ayudasCompletadas; }
    /** Insignia del Banco de Tiempo y Solidaridad Vecinal (sin dinero). */
    public String getInsigniaSolidaria() {
        if (ayudasCompletadas >= 5) return "Guardián del Barrio";
        if (ayudasCompletadas >= 2) return "Vecino Solidario";
        if (ayudasCompletadas >= 1) return "Mano Amiga";
        return "Vecino Activo";
    }
    /** Créditos de gratitud acumulados en el Banco de Tiempo Vecinal. */
    public int getCreditosSolidarios() {
        return 10 + (ayudasCompletadas * 15);
    }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
