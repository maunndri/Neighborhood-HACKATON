package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Reporte de un vecino sobre una publicación o recurso sospechoso. Lo revisa un administrador. */
@Entity
@Table(name = "denuncias")
public class DenunciaContenido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** servicio, favor, emergencia o recurso. */
    @Column(nullable = false, length = 20)
    private String tipo;
    @Column(nullable = false)
    private Long objetivoId;
    @Column(length = 160)
    private String titulo;
    @Column(nullable = false, length = 300)
    private String motivo;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false, length = 20)
    private String estado = "Pendiente";
    private LocalDateTime creadoEn = LocalDateTime.now();

    public DenunciaContenido() {}
    public Long getId(){return id;}
    public String getTipo(){return tipo;}
    public void setTipo(String tipo){this.tipo=tipo;}
    public Long getObjetivoId(){return objetivoId;}
    public void setObjetivoId(Long objetivoId){this.objetivoId=objetivoId;}
    public String getTitulo(){return titulo;}
    public void setTitulo(String titulo){this.titulo=titulo;}
    public String getMotivo(){return motivo;}
    public void setMotivo(String motivo){this.motivo=motivo;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getEstado(){return estado;}
    public void setEstado(String estado){this.estado=estado;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
}
