package com.zenitProject.app.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.zenitProject.app.entidades.Evento;

public interface EventoRepository extends MongoRepository<Evento, String> {
    List<Evento> findByOrganizadorId(String organizadorId);
    long countByOrganizadorId(String organizadorId);
    Page<Evento> findByEstadoAprobacion(String estadoAprobacion, Pageable pageable);
    
    @Query("{ 'estadoAprobacion': ?0, 'nombreEvento': { $regex: ?1, $options: 'i' } }")
    Page<Evento> findByEstadoAprobacionAndNombreEventoContaining(String estadoAprobacion, String nombreEvento, Pageable pageable);
}