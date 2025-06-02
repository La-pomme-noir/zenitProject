package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.repository.ProveedorRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorApiController {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProveedorProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
        }

        String correo = authentication.getName();
        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Proveedor no encontrado"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", proveedor.getId() != null ? proveedor.getId() : "N/A");
        profile.put("nombre", proveedor.getNombre() != null ? proveedor.getNombre() : "N/A");
        profile.put("correo", proveedor.getCorreo() != null ? proveedor.getCorreo() : "N/A");
        profile.put("imagenUrl", proveedor.getImagenUrl() != null ? proveedor.getImagenUrl() : ""); // Añadir imagenUrl
        profile.put("role", proveedor.getRole() != null ? proveedor.getRole() : "PROVEEDOR");
        System.out.println("Perfil devuelto: " + profile);
        return ResponseEntity.ok(profile);
    }
}