package com.neighborhood.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comunidades_vecinales")
public class ComunidadVecinal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String distrito;

    @Column(nullable = false, length = 180)
    private String urbanizacion;

    /** "Distrital" o "Urbanización / Junta Vecinal" */
    @Column(nullable = false, length = 50)
    private String nivel = "Distrital";

    @Column(nullable = false, length = 600)
    private String descripcion;

    @Column(nullable = false, length = 120)
    private String codigoWhatsapp;

    private Double latitud;
    private Double longitud;

    @Column(nullable = false)
    private double radioKm = 3.8;

    @Column(nullable = false)
    private int miembrosBase = 85;

    private LocalDateTime creadoEn = LocalDateTime.now();

    // Campos calculados para la vista según la localización del vecino actual
    @Transient private boolean accesoPermitido;
    @Transient private boolean unido;
    @Transient private Double distanciaKm;
    @Transient private String razonAcceso;
    @Transient private long totalMensajes;
    @Transient private int totalMiembros;
    @Transient private String ultimoMensajeTexto;
    @Transient private String ultimoMensajeAutor;

    public ComunidadVecinal() {}

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }
    public String getUrbanizacion() { return urbanizacion; }
    public void setUrbanizacion(String urbanizacion) { this.urbanizacion = urbanizacion; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCodigoWhatsapp() { return codigoWhatsapp; }
    public void setCodigoWhatsapp(String codigoWhatsapp) { this.codigoWhatsapp = codigoWhatsapp; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public double getRadioKm() { return radioKm; }
    public void setRadioKm(double radioKm) { this.radioKm = radioKm; }
    public int getMiembrosBase() { return miembrosBase; }
    public void setMiembrosBase(int miembrosBase) { this.miembrosBase = miembrosBase; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public boolean isAccesoPermitido() { return accesoPermitido; }
    public void setAccesoPermitido(boolean accesoPermitido) { this.accesoPermitido = accesoPermitido; }
    public boolean isUnido() { return unido; }
    public void setUnido(boolean unido) { this.unido = unido; }
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }
    public String getDistanciaFormato() {
        if (distanciaKm == null) return "";
        if (distanciaKm < 1.0) return "a " + Math.max(50, Math.round(distanciaKm * 20) * 50) + " m de ti";
        return String.format(java.util.Locale.US, "a %.1f km de ti", distanciaKm);
    }
    public String getRazonAcceso() { return razonAcceso; }
    public void setRazonAcceso(String razonAcceso) { this.razonAcceso = razonAcceso; }
    public long getTotalMensajes() { return totalMensajes; }
    public void setTotalMensajes(long totalMensajes) { this.totalMensajes = totalMensajes; }
    public int getTotalMiembros() { return totalMiembros > 0 ? totalMiembros : miembrosBase; }
    public void setTotalMiembros(int totalMiembros) { this.totalMiembros = totalMiembros; }
    public String getUltimoMensajeTexto() { return ultimoMensajeTexto; }
    public void setUltimoMensajeTexto(String ultimoMensajeTexto) { this.ultimoMensajeTexto = ultimoMensajeTexto; }
    public String getUltimoMensajeAutor() { return ultimoMensajeAutor; }
    public void setUltimoMensajeAutor(String ultimoMensajeAutor) { this.ultimoMensajeAutor = ultimoMensajeAutor; }
}
