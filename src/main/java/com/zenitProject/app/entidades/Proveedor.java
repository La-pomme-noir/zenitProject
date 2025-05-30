package com.zenitProject.app.entidades;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.ArrayList;
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
    private String categoria;

    @Field("servicioCategoria")
    private String servicioCategoria;

    @Field("servicioDescripcion")
    private String servicioDescripcion;

    @Field("imagenUrl")
    private String imagenUrl;

    // Nuevos campos para eventos pendientes y asistidos
    @Field("eventosPendientes")
    private List<String> eventosPendientes = new ArrayList<>();

    @Field("eventosAsistidos")
    private List<String> eventosAsistidos = new ArrayList<>();

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getServicioCategoria() { return servicioCategoria; }
    public void setServicioCategoria(String servicioCategoria) { this.servicioCategoria = servicioCategoria; }

    public String getServicioDescripcion() { return servicioDescripcion; }
    public void setServicioDescripcion(String servicioDescripcion) { this.servicioDescripcion = servicioDescripcion; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public List<String> getEventosPendientes() { return eventosPendientes; }
    public void setEventosPendientes(List<String> eventosPendientes) { this.eventosPendientes = eventosPendientes; }

    public List<String> getEventosAsistidos() { return eventosAsistidos; }
    public void setEventosAsistidos(List<String> eventosAsistidos) { this.eventosAsistidos = eventosAsistidos; }
}