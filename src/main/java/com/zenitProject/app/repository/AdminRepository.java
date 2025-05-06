package com.zenitProject.app.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.zenitProject.app.entidades.Admin;

public interface AdminRepository extends MongoRepository<Admin, String>{
	Admin findByCorreo(String correo);
}
