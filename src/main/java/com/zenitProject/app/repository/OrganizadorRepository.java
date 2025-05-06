package com.zenitProject.app.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.zenitProject.app.entidades.Organizador;

public interface OrganizadorRepository extends MongoRepository<Organizador, String>{
	Organizador findByCorreo(String correo);
}
