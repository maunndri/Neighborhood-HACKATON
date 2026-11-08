package com.barrioconecta.util;

import java.util.List;

/** Listas de opciones compartidas por formularios y filtros. */
public final class Catalogos {
    private Catalogos() {}

    public static final String ESTADO_PENDIENTE = "Pendiente";
    public static final String ESTADO_COORDINANDO = "En coordinación";
    public static final String ESTADO_RESUELTA = "Resuelta";

    public static final String TIPO_SOLICITO = "solicito";
    public static final String TIPO_OFREZCO = "ofrezco";

    public static final List<String> CATEGORIAS_SERVICIO = List.of(
        "Gasfitería", "Electricidad", "Carpintería", "Cerrajería", "Paseo de mascotas",
        "Clases y tutorías", "Alimentos", "Reparaciones", "Otro");

    public static final List<String> CATEGORIAS_FAVOR = List.of(
        "Herramientas", "Transporte", "Alimentos", "Apoyo temporal", "Donación de materiales",
        "Intercambio de habilidades", "Cuidado de personas o mascotas", "Otro");

    public static final List<String> CATEGORIAS_EMERGENCIA = List.of(
        "Adulto mayor o persona vulnerable", "Atención médica y primeros auxilios", 
        "Alimentos y agua potable", "Refugio y albergue temporal", 
        "Sin electricidad / Paciente dependiente", "Transporte solidario y rescate", 
        "Otro apoyo urgente");

    public static final List<String> DISPONIBILIDAD = List.of(
        "Hoy", "Esta semana", "Mañanas", "Tardes", "Noches", "Fines de semana", "Flexible");

    /** Redondea a 3 decimales (~110 m) para no exponer la puerta exacta de una vivienda. */
    public static Double aproximar(Double coordenada) {
        if (coordenada == null) return null;
        return Math.round(coordenada * 1000.0) / 1000.0;
    }

    public static String limpiar(String texto, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }
}
