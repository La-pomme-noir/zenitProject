package com.zenitProject.app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.repository.ClienteRepository;

@RestController
@RequestMapping("/api/clientes")
public class ClienteApiController {
	
	@Autowired
    private ClienteRepository clienteRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;
    
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