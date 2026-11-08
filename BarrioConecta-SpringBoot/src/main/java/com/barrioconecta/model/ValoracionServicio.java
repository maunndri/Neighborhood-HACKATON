package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "valoraciones_servicio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"servicio_id", "usuario_id"}))
public class ValoracionServicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id")
    private PublicacionServicio servicio;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false)
    private Integer puntuacion;
    @Column(length = 500)
    private String comentario;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public ValoracionServicio() {}
    public Long getId(){ return id; }
    public PublicacionServicio getServicio(){ return servicio; }
    public void setServicio(PublicacionServicio servicio){ this.servicio=servicio; }
    public Usuario getUsuario(){ return usuario; }
    public void setUsuario(Usuario usuario){ this.usuario=usuario; }
    public Integer getPuntuacion(){ return puntuacion; }
    public void setPuntuacion(Integer puntuacion){ this.puntuacion=puntuacion; }
    public String getComentario(){ return comentario; }
    public void setComentario(String comentario){ this.comentario=comentario; }
    public LocalDateTime getCreadoEn(){ return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn){ this.creadoEn=creadoEn; }
}
