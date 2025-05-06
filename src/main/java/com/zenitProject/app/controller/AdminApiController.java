package com.zenitProject.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Admin;
import com.zenitProject.app.repository.AdminRepository;

@RestController
@RequestMapping("/api/admins")
public class AdminApiController {
	
	@Autowired
    private AdminRepository adminRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;

	// GET: Obtener datos del administrador autenticado para la UI
    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getAdminProfile(Authentication authentication) {
        String correo = authentication.getName(); // Correo del usuario autenticado
        Admin admin = adminRepository.findByCorreo(correo);
        if (admin == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> profile = new HashMap<>();
        profile.put("correo", admin.getCorreo());
        profile.put("nombre", admin.getNombre()); // Añadir el nombre
        profile.put("role", admin.getRole());
        return ResponseEntity.ok(profile);
    }
    
    // POST: Login con verificación de rol
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> loginRequest) {
        String correo = loginRequest.get("correo");
        String password = loginRequest.get("password");
        String selectedRole = loginRequest.get("role");
        
        Admin admin = adminRepository.findByCorreo(correo);
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Credenciales inválidas"));
        }
        if (!admin.getRole().equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol seleccionado no coincide"));
        }
        return ResponseEntity.ok(Map.of("message", "Login exitoso", "correo", correo, "role", admin.getRole()));
    }
}