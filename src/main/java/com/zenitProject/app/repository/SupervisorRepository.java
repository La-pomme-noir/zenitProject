package com.zenitProject.app.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.zenitProject.app.entidades.Supervisor;

public interface SupervisorRepository extends MongoRepository<Supervisor, String>{
	Supervisor findByCorreo(String correo);
}
