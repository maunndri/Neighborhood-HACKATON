package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grupos_apoyo")
public class GrupoApoyo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 140)
    private String nombre;
    @Column(nullable = false, length = 60)
    private String tipo; // Grupo vecinal, servicio comunitario, profesional voluntario
    @Column(nullable = false, length = 80)
    private String categoria; // Salud, alimentos, refugio, transporte, rescate...
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    @Column(length = 100)
    private String zonaCobertura;
    @Column(length = 40)
    private String telefono;
    @Column(length = 160)
    private String disponibilidad;
    private Double latitud;
    private Double longitud;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(nullable = false)
    private boolean verificado = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean oculto = false;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public GrupoApoyo() {}
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getNombre(){return nombre;}
    public void setNombre(String nombre){this.nombre=nombre;}
    public String getTipo(){return tipo;}
    public void setTipo(String tipo){this.tipo=tipo;}
    public String getCategoria(){return categoria;}
    public void setCategoria(String categoria){this.categoria=categoria;}
    public String getDescripcion(){return descripcion;}
    public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public String getZonaCobertura(){return zonaCobertura;}
    public void setZonaCobertura(String zonaCobertura){this.zonaCobertura=zonaCobertura;}
    public String getTelefono(){return telefono;}
    public String getTelefonoWhatsapp(){if(telefono==null)return null;String n=telefono.replaceAll("\\D","");return n.length()==9?"51"+n:n;}
    public void setTelefono(String telefono){this.telefono=telefono;}
    public String getDisponibilidad(){return disponibilidad;}
    public void setDisponibilidad(String disponibilidad){this.disponibilidad=disponibilidad;}
    public Double getLatitud(){return latitud;}
    public void setLatitud(Double latitud){this.latitud=latitud;}
    public Double getLongitud(){return longitud;}
    public void setLongitud(Double longitud){this.longitud=longitud;}
    public boolean isActivo(){return activo;}
    public void setActivo(boolean activo){this.activo=activo;}
    public boolean isVerificado(){return verificado;}
    public void setVerificado(boolean verificado){this.verificado=verificado;}
    public boolean isOculto(){return oculto;}
    public void setOculto(boolean oculto){this.oculto=oculto;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
}
