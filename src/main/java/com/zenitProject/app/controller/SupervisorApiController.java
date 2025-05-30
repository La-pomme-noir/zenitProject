package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.SupervisorRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/supervisores")
public class SupervisorApiController {

    @Autowired
    private SupervisorRepository supervisorRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getSupervisorProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
        }

        String correo = authentication.getName();
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Supervisor no encontrado"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", supervisor.getId() != null ? supervisor.getId() : "N/A");
        profile.put("nombre", supervisor.getNombre() != null ? supervisor.getNombre() : "N/A");
        profile.put("correo", supervisor.getCorreo() != null ? supervisor.getCorreo() : "N/A");
        profile.put("role", supervisor.getRole() != null ? supervisor.getRole() : "SUPERVISOR"); // Añadir el campo role
        System.out.println("Perfil devuelto: " + profile);
        return ResponseEntity.ok(profile);
    }
}