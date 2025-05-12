package com.zenitProject.app.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.zenitProject.app.entidades.Invitado;

public interface InvitadoRepository extends MongoRepository<Invitado, String>{
	Invitado findByCorreo(String correo);
}
