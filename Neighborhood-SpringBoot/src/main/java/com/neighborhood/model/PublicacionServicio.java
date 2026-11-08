package com.neighborhood.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "servicios")
public class PublicacionServicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false, length = 160)
    private String titulo;
    @Column(nullable = false, length = 30)
    private String tipo = "servicio";
    @Column(nullable = false, length = 100)
    private String categoria;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    private Double precio;
    @Column(length = 40)
    private String contacto;
    private Double latitud;
    private Double longitud;
    @Column(length = 500)
    private String imagenUrl;
    /** Verificado por un administrador (identidad o recurso). No equivale a una certificación profesional. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verificado = false;
    private LocalDateTime verificadoEn;
    /** Oculto por moderación: no aparece en listados ni en el mapa. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean oculto = false;
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Transient private Double promedioCalificacion = 0.0;
    @Transient private Integer totalCalificaciones = 0;
    @Transient private Integer miCalificacion = 0;
    @Transient private Integer totalRecomendaciones = 0;
    @Transient private Integer recomendacionesZona = 0;
    /** NINGUNO, RECOMENDADO o MUY. */
    @Transient private String nivelVecinal = "NINGUNO";
    @Transient private String ultimaRecomendacion;

    public PublicacionServicio() {}
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getTitulo(){return titulo;}
    public void setTitulo(String titulo){this.titulo=titulo;}
    public String getTipo(){return tipo;}
    public void setTipo(String tipo){this.tipo=tipo;}
    public String getCategoria(){return categoria;}
    public void setCategoria(String categoria){this.categoria=categoria;}
    public String getDescripcion(){return descripcion;}
    public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public Double getPrecio(){return precio;}
    public void setPrecio(Double precio){this.precio=precio;}
    public String getContacto(){return contacto;}
    public void setContacto(String contacto){this.contacto=contacto;}
    /** Número listo para wa.me (los de 9 dígitos se interpretan como Perú, +51). */
    public String getContactoWhatsapp(){ if(contacto==null) return null; String n=contacto.replaceAll("\\D",""); if(n.isEmpty()) return null; return n.length()==9?"51"+n:n; }
    public Double getLatitud(){return latitud;}
    public void setLatitud(Double latitud){this.latitud=latitud;}
    public Double getLongitud(){return longitud;}
    public void setLongitud(Double longitud){this.longitud=longitud;}
    public String getImagenUrl(){return imagenUrl;}
    public void setImagenUrl(String imagenUrl){this.imagenUrl=imagenUrl;}
    /** Devuelve la foto subida por el vecino o una fotografía realista acorde a su oficio/categoría. */
    public String getImagenPortada() {
        if (imagenUrl != null && !imagenUrl.isBlank()) return imagenUrl;
        if (categoria == null) return "/images/categorias/otro.jpg";
        String c = categoria.toLowerCase();
        if (c.contains("gasfiter")) return "/images/categorias/gasfiteria.jpg";
        if (c.contains("electric")) return "/images/categorias/electricidad.jpg";
        if (c.contains("carpinter")) return "/images/categorias/carpinteria.jpg";
        if (c.contains("cerrajer")) return "/images/categorias/cerrajeria.jpg";
        if (c.contains("mascota")) return "/images/categorias/mascotas.jpg";
        if (c.contains("clase") || c.contains("tutor")) return "/images/categorias/clases.jpg";
        if (c.contains("alimento") || c.contains("comida") || "producto".equalsIgnoreCase(tipo)) return "/images/categorias/alimentos.jpg";
        if (c.contains("reparac")) return "/images/categorias/reparaciones.jpg";
        return "/images/categorias/otro.jpg";
    }
    public boolean isVerificado(){return verificado;}
    public void setVerificado(boolean verificado){this.verificado=verificado;}
    public LocalDateTime getVerificadoEn(){return verificadoEn;}
    public void setVerificadoEn(LocalDateTime verificadoEn){this.verificadoEn=verificadoEn;}
    public boolean isOculto(){return oculto;}
    public void setOculto(boolean oculto){this.oculto=oculto;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
    public Double getPromedioCalificacion(){return promedioCalificacion;}
    public void setPromedioCalificacion(Double v){this.promedioCalificacion=v;}
    public Integer getTotalCalificaciones(){return totalCalificaciones;}
    public void setTotalCalificaciones(Integer v){this.totalCalificaciones=v;}
    public Integer getMiCalificacion(){return miCalificacion;}
    public void setMiCalificacion(Integer v){this.miCalificacion=v;}
    public Integer getTotalRecomendaciones(){return totalRecomendaciones;}
    public void setTotalRecomendaciones(Integer v){this.totalRecomendaciones=v;}
    public Integer getRecomendacionesZona(){return recomendacionesZona;}
    public void setRecomendacionesZona(Integer v){this.recomendacionesZona=v;}
    public String getNivelVecinal(){return nivelVecinal;}
    public void setNivelVecinal(String v){this.nivelVecinal=v;}
    public String getUltimaRecomendacion(){return ultimaRecomendacion;}
    public void setUltimaRecomendacion(String v){this.ultimaRecomendacion=v;}
}
