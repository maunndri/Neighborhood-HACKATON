package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(nullable = false, length = 300)
    private String mensaje;
    @Column(length = 200)
    private String enlace;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean leida = false;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Notificacion() {}
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getMensaje(){return mensaje;}
    public void setMensaje(String mensaje){this.mensaje=mensaje;}
    public String getEnlace(){return enlace;}
    public void setEnlace(String enlace){this.enlace=enlace;}
    public boolean isLeida(){return leida;}
    public void setLeida(boolean leida){this.leida=leida;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
    public String getFecha(){return creadoEn==null?"":creadoEn.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));}
}
