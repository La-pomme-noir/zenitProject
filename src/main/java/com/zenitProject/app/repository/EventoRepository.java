package com.zenitProject.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.zenitProject.app.entidades.Evento;

public interface EventoRepository extends MongoRepository<Evento, String> {
    // Puedes añadir consultas personalizadas si las necesitas
	List<Evento> findByOrganizadorId(String organizadorId);
    long countByOrganizadorId(String organizadorId);
    Page<Evento> findByEstadoAprobacion(String estadoAprobacion, Pageable pageable);
}