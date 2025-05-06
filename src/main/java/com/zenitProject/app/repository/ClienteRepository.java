package com.zenitProject.app.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.zenitProject.app.entidades.Cliente;

public interface ClienteRepository extends MongoRepository<Cliente, String>{
	Cliente findByCorreo(String correo);
}
