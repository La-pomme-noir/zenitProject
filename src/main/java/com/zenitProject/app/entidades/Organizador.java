package com.zenitProject.app.entidades;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "organizadores")
public class Organizador {

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
    private String role = "ORGANIZADOR";

    @Field("eventosOrganizados")
    private Integer eventosOrganizados = 0; // Inicialmente 0, se calculará dinámicamente

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

    public Integer getEventosOrganizados() {
        return eventosOrganizados;
    }

    public void setEventosOrganizados(Integer eventosOrganizados) {
        this.eventosOrganizados = eventosOrganizados;
    }
}