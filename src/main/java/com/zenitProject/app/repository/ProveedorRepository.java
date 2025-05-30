package com.zenitProject.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.zenitProject.app.entidades.Proveedor;

public interface ProveedorRepository extends MongoRepository<Proveedor, String> {
    Proveedor findByCorreo(String correo);
    List<Proveedor> findByCategoria(String categoria);
}