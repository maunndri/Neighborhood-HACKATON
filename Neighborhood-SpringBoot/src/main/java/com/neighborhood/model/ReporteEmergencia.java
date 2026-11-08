package com.neighborhood.model;

import com.neighborhood.util.Catalogos;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Necesidad de ayuda en emergencia. Estados: Pendiente -> En coordinación -> Resuelta. */
@Entity
@Table(name = "emergencias")
public class ReporteEmergencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false, length = 100)
    private String categoria;
    @Column(nullable = false, length = 160)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    private Double latitud;
    private Double longitud;
    @Column(nullable = false, length = 30)
    private String estado = Catalogos.ESTADO_PENDIENTE;
    @ManyToOne @JoinColumn(name = "responsable_id")
    private Usuario responsable;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean oculto = false;
    private LocalDateTime creadoEn = LocalDateTime.now();
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @Transient private List<RespuestaAyuda> respuestas = new ArrayList<>();
    @Transient private List<EventoSeguimiento> eventos = new ArrayList<>();

    public ReporteEmergencia() {}
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getCategoria(){return categoria;}
    public void setCategoria(String categoria){this.categoria=categoria;}
    public boolean isSos(){return "SOS".equals(categoria) || (titulo != null && titulo.startsWith("ALERTA SOS"));}
    public String getTitulo(){return titulo;}
    public void setTitulo(String titulo){this.titulo=titulo;}
    public String getDescripcion(){return descripcion;}
    public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public Double getLatitud(){return latitud;}
    public void setLatitud(Double latitud){this.latitud=latitud;}
    public Double getLongitud(){return longitud;}
    public void setLongitud(Double longitud){this.longitud=longitud;}
    /** Los registros antiguos con estado "Activa" se muestran como Pendiente. */
    public String getEstado(){return estado==null||estado.isBlank()||"Activa".equals(estado)?Catalogos.ESTADO_PENDIENTE:estado;}
    public void setEstado(String estado){this.estado=estado;}
    public Usuario getResponsable(){return responsable;}
    public void setResponsable(Usuario responsable){this.responsable=responsable;}
    public boolean isOculto(){return oculto;}
    public void setOculto(boolean oculto){this.oculto=oculto;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
    public LocalDateTime getActualizadoEn(){return actualizadoEn;}
    public void setActualizadoEn(LocalDateTime actualizadoEn){this.actualizadoEn=actualizadoEn;}
    public List<RespuestaAyuda> getRespuestas(){return respuestas;}
    public void setRespuestas(List<RespuestaAyuda> respuestas){this.respuestas=respuestas;}
    /** Vecinos con una respuesta vigente (ofrecida o aceptada). */
    public long getVecinosCoordinando(){return respuestas.stream().filter(r->!RespuestaAyuda.RECHAZADA.equals(r.getEstado())).count();}
    /** Respuesta del vecino indicado a esta publicación (o null). */
    public RespuestaAyuda respuestaDe(Long usuarioId){
        if(usuarioId==null) return null;
        return respuestas.stream().filter(r->r.getUsuario().getId().equals(usuarioId)).findFirst().orElse(null);
    }
    public boolean isOferta(){return false;}
    public List<EventoSeguimiento> getEventos(){return eventos;}
    public void setEventos(List<EventoSeguimiento> eventos){this.eventos=eventos;}
}
