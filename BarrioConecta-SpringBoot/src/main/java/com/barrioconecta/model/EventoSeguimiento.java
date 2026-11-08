package com.barrioconecta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Línea de tiempo de una solicitud: quién hizo qué y cuándo. */
@Entity
@Table(name = "eventos_seguimiento")
public class EventoSeguimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String objetivoTipo;
    @Column(nullable = false)
    private Long objetivoId;
    @Column(nullable = false, length = 300)
    private String texto;
    private LocalDateTime creadoEn = LocalDateTime.now();

    public EventoSeguimiento() {}
    public EventoSeguimiento(String objetivoTipo, Long objetivoId, String texto) {
        this.objetivoTipo = objetivoTipo; this.objetivoId = objetivoId; this.texto = texto;
    }
    public Long getId(){return id;}
    public String getObjetivoTipo(){return objetivoTipo;}
    public Long getObjetivoId(){return objetivoId;}
    public String getTexto(){return texto;}
    public LocalDateTime getCreadoEn(){return creadoEn;}
    public String getFecha(){return creadoEn==null?"":creadoEn.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));}
}
