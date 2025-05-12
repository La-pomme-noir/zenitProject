package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Organizador;
import com.zenitProject.app.repository.OrganizadorRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/organizadores")
public class OrganizadorApiController {

    @Autowired
    private OrganizadorRepository organizadorRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getOrganizadorProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Organizador no encontrado"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", organizador.getId() != null ? organizador.getId() : "N/A");
        profile.put("nombre", organizador.getNombre() != null ? organizador.getNombre() : "N/A");
        profile.put("correo", organizador.getCorreo() != null ? organizador.getCorreo() : "N/A");
        profile.put("eventosOrganizados", organizador.getEventosOrganizados() != null ? organizador.getEventosOrganizados() : 0);
        System.out.println("Perfil devuelto: " + profile);
        return ResponseEntity.ok(profile);
    }
}