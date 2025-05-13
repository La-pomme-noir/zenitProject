package com.zenitProject.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Admin;
import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.repository.ClienteRepository;

@RestController
@RequestMapping("/api/clientes")
public class ClienteApiController {
	
	@Autowired
    private ClienteRepository clienteRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;
    
	// GET: Obtener datos del administrador autenticado para la UI
    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getClienteProfile(Authentication authentication) {
        String correo = authentication.getName(); // Correo del usuario autenticado
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> profile = new HashMap<>();
        profile.put("correo", cliente.getCorreo());
        profile.put("nombre", cliente.getNombre()); // Añadir el nombre
        profile.put("role", cliente.getRole());
        return ResponseEntity.ok(profile);
    }
	
    // POST: Login con verificación de rol
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> loginRequest) {
        String correo = loginRequest.get("correo");
        String password = loginRequest.get("password");
        String selectedRole = loginRequest.get("role");
        
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null || !passwordEncoder.matches(password, cliente.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Credenciales inválidas"));
        }
        if (!cliente.getRole().equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol seleccionado no coincide"));
        }
        return ResponseEntity.ok(Map.of("message", "Login exitoso", "correo", correo, "role", cliente.getRole()));
    }
}