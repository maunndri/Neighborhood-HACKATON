package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un vecino responde a una publicación: "Puedo ayudar" (si la publicación es una necesidad)
 * o "Lo necesito" (si es una oferta). El dueño puede aceptar o rechazar.
 * objetivoTipo = "favor" o "emergencia".
 */
@Entity
@Table(name = "respuestas_ayuda")
public class RespuestaAyuda {
    public static final String OFRECIDA = "Ofrecida";
    public static final String ACEPTADA = "Aceptada";
    public static final String RECHAZADA = "Rechazada";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String objetivoTipo;
    @Column(nullable = false)
    private Long objetivoId;
    @ManyToOne(optional = false) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Column(length = 300)
    private String mensaje;
    @Column(nullable = false, length = 20)
    private String estado = OFRECIDA;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public RespuestaAyuda() {}
    public Long getId(){return id;}
    public String getObjetivoTipo(){return objetivoTipo;}
    public void setObjetivoTipo(String objetivoTipo){this.objetivoTipo=objetivoTipo;}
    public Long getObjetivoId(){return objetivoId;}
    public void setObjetivoId(Long objetivoId){this.objetivoId=objetivoId;}
    public Usuario getUsuario(){return usuario;}
    public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getMensaje(){return mensaje;}
    public void setMensaje(String mensaje){this.mensaje=mensaje;}
    public String getEstado(){return estado;}
    public void setEstado(String estado){this.estado=estado;}
    public boolean isAceptada(){return ACEPTADA.equals(estado);}
    public boolean isOfrecida(){return OFRECIDA.equals(estado);}
    public LocalDateTime getCreadoEn(){return creadoEn;}
}
