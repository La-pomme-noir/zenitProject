package com.zenitProject.app.entidades;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "clientes")
public class Cliente {
	
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
    private String role = "CLIENTE";
    
    @Field("eventosRegistrados")
    private List<String> eventosRegistrados = new ArrayList<>();

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

	public List<String> getEventosRegistrados() {
		return eventosRegistrados;
	}

	public void setEventosRegistrados(List<String> eventosRegistrados) {
		this.eventosRegistrados = eventosRegistrados;
	}
	
}
