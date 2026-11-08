package com.barrioconecta.model;

import com.barrioconecta.util.Catalogos;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Publicación de la cadena de favores. tipo = "solicito" (Necesito ayuda) u "ofrezco" (Quiero ayudar).
 * Estados: Pendiente -> En coordinación -> Resuelta.
 */
@Entity
@Table(name = "favores")
public class Favor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false, length = 20)
    private String tipo;
    @Column(length = 100)
    private String categoria;
    @Column(nullable = false, length = 160)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    @Column(length = 100)
    private String disponibilidad;
    private Double latitud;
    private Double longitud;
    private boolean activo = true;
    @Column(length = 30)
    private String estado = Catalogos.ESTADO_PENDIENTE;
    @ManyToOne @JoinColumn(name = "responsable_id")
    private Usuario responsable;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean oculto = false;
    private LocalDateTime creadoEn = LocalDateTime.now();
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @Transient private List<RespuestaAyuda> respuestas = new ArrayList<>();
    @Transient private List<EventoSeguimiento> eventos = new ArrayList<>();

    public Favor() {}
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getTipo(){return tipo;}
    public void setTipo(String tipo){this.tipo=tipo;}
    public boolean isOferta(){return Catalogos.TIPO_OFREZCO.equals(tipo);}
    public String getCategoria(){return categoria==null||categoria.isBlank()?"Apoyo vecinal":categoria;}
    public void setCategoria(String categoria){this.categoria=categoria;}
    public String getTitulo(){return titulo;}
    public void setTitulo(String titulo){this.titulo=titulo;}
    public String getDescripcion(){return descripcion;}
    public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public String getDisponibilidad(){return disponibilidad;}
    public void setDisponibilidad(String disponibilidad){this.disponibilidad=disponibilidad;}
    public Double getLatitud(){return latitud;}
    public void setLatitud(Double latitud){this.latitud=latitud;}
    public Double getLongitud(){return longitud;}
    public void setLongitud(Double longitud){this.longitud=longitud;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
    public LocalDateTime getActualizadoEn(){return actualizadoEn;}
    public void setActualizadoEn(LocalDateTime actualizadoEn){this.actualizadoEn=actualizadoEn;}
    public boolean isActivo(){return activo;}
    public void setActivo(boolean activo){this.activo=activo;}
    public String getEstado(){return estado==null||estado.isBlank()?Catalogos.ESTADO_PENDIENTE:estado;}
    public void setEstado(String estado){this.estado=estado;}
    public Usuario getResponsable(){return responsable;}
    public void setResponsable(Usuario responsable){this.responsable=responsable;}
    public boolean isOculto(){return oculto;}
    public void setOculto(boolean oculto){this.oculto=oculto;}
    public List<RespuestaAyuda> getRespuestas(){return respuestas;}
    public void setRespuestas(List<RespuestaAyuda> respuestas){this.respuestas=respuestas;}
    /** Vecinos con una respuesta vigente (ofrecida o aceptada). */
    public long getVecinosCoordinando(){return respuestas.stream().filter(r->!RespuestaAyuda.RECHAZADA.equals(r.getEstado())).count();}
    /** Respuesta del vecino indicado a esta publicación (o null). */
    public RespuestaAyuda respuestaDe(Long usuarioId){
        if(usuarioId==null) return null;
        return respuestas.stream().filter(r->r.getUsuario().getId().equals(usuarioId)).findFirst().orElse(null);
    }
    public List<EventoSeguimiento> getEventos(){return eventos;}
    public void setEventos(List<EventoSeguimiento> eventos){this.eventos=eventos;}
}
