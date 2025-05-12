package com.zenitProject.app.entidades;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "eventos")
public class Evento {

    @Id
    private String id;

    @Field("nombreEvento")
    private String nombreEvento;

    @Field("fecha")
    private String fecha;

    @Field("invitados")
    private List<String> invitados; // Lista de nombres o IDs de invitados

    @Field("cantidadSillas")
    private Integer cantidadSillas;

    @Field("proveedores")
    private Map<String, String> proveedores; // Mapa: categoría -> nombre o ID del proveedor

    @Field("lugar")
    private String lugar;

    @Field("hora")
    private String hora;

    @Field("ciudad")
    private String ciudad;

    @Field("descripcion")
    private String descripcion;

    @Field("requisitos")
    private String requisitos;

    @Field("organizadorId")
    private String organizadorId;

    @Field("organizadorNombre")
    private String organizadorNombre;

    @Field("estadoAprobacion")
    private String estadoAprobacion = "Pendiente"; // Valores: Pendiente, Aprobado, Desaprobado

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public List<String> getInvitados() {
        return invitados;
    }

    public void setInvitados(List<String> invitados) {
        this.invitados = invitados;
    }

    public Integer getCantidadSillas() {
        return cantidadSillas;
    }

    public void setCantidadSillas(Integer cantidadSillas) {
        this.cantidadSillas = cantidadSillas;
    }

    public Map<String, String> getProveedores() {
        return proveedores;
    }

    public void setProveedores(Map<String, String> proveedores) {
        this.proveedores = proveedores;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public void setRequisitos(String requisitos) {
        this.requisitos = requisitos;
    }

    public String getOrganizadorId() {
        return organizadorId;
    }

    public void setOrganizadorId(String organizadorId) {
        this.organizadorId = organizadorId;
    }

    public String getOrganizadorNombre() {
        return organizadorNombre;
    }

    public void setOrganizadorNombre(String organizadorNombre) {
        this.organizadorNombre = organizadorNombre;
    }

    public String getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public void setEstadoAprobacion(String estadoAprobacion) {
        this.estadoAprobacion = estadoAprobacion;
    }
}