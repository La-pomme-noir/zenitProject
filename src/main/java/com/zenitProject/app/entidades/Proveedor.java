package com.zenitProject.app.entidades;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "proveedores")
public class Proveedor {

    @Id
    private String id;
    @Indexed(unique = true)

    @Field("nombre")
    private String nombre;

    @Field("correo")
    private String correo;

    @Field("password")
    private String password;

    @Field("role")
    private String role = "PROVEEDOR";

    @Field("categoria")
    private String categoria; // Nueva campo para la categoría del proveedor

    @Field("servicios")
    private List<String> servicios; // Lista de servicios que ofrece el proveedor

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public List<String> getServicios() {
        return servicios;
    }

    public void setServicios(List<String> servicios) {
        this.servicios = servicios;
    }
}