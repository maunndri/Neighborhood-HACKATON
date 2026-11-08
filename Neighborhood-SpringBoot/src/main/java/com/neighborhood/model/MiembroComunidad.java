package com.neighborhood.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "miembros_comunidad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"comunidad_id", "usuario_id"})
})
public class MiembroComunidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private ComunidadVecinal comunidad;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private LocalDateTime unidoEn = LocalDateTime.now();

    public MiembroComunidad() {}
    public MiembroComunidad(ComunidadVecinal comunidad, Usuario usuario) {
        this.comunidad = comunidad;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public ComunidadVecinal getComunidad() { return comunidad; }
    public void setComunidad(ComunidadVecinal comunidad) { this.comunidad = comunidad; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getUnidoEn() { return unidoEn; }
}
