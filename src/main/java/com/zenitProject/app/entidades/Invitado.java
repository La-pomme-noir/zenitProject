package com.zenitProject.app.entidades;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "invitados")
public class Invitado {

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
    private String role = "INVITADO";

    @Field("invitaciones")
    private List<String> invitaciones = new ArrayList<>(); // Inicializar como lista vacía

    @Field("eventosAsistidos")
    private List<String> eventosAsistidos = new ArrayList<>(); // Inicializar como lista vacía

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

    public List<String> getInvitaciones() {
        return invitaciones;
    }

    public void setInvitaciones(List<String> invitaciones) {
        this.invitaciones = invitaciones;
    }

    public List<String> getEventosAsistidos() {
        return eventosAsistidos;
    }

    public void setEventosAsistidos(List<String> eventosAsistidos) {
        this.eventosAsistidos = eventosAsistidos;
    }
}