package com.barrioconecta.controller;

/** Elemento del mapa. clase: servicio, favor, emergencia o recurso. */
public record MapaPublicacionDto(String clase, Long id, String titulo, String descripcion,
        String categoria, String tipo, String autor, Double latitud, Double longitud, String contacto,
        String estado, boolean verificado, Double calificacion, String enlace) {}
