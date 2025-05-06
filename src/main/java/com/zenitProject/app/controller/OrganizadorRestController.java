package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Organizador;
import com.zenitProject.app.repository.OrganizadorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/organizadores")
public class OrganizadorRestController {

    @Autowired
    private OrganizadorRepository organizadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los organizadores
    @GetMapping
    public List<Organizador> getAllOrganizadores() {
        return organizadorRepository.findAll();
    }

    @GetMapping("/{correo}")
    public ResponseEntity<Organizador> getOrganizadorByCorreo(@PathVariable String correo) {
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador != null) {
            return ResponseEntity.ok(organizador);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Organizador> createOrganizador(@RequestBody Organizador organizador) {
        if (organizadorRepository.findByCorreo(organizador.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        organizador.setPassword(passwordEncoder.encode(organizador.getPassword()));
        organizador.setRole("ORGANIZADOR");
        Organizador saved = organizadorRepository.save(organizador);
        return ResponseEntity.ok(saved);
    }

    // Actualizar un organizador
    @PutMapping("/{correo}")
    public ResponseEntity<Organizador> updateOrganizador(@PathVariable String correo, @RequestBody Organizador organizador) {
        Organizador existing = organizadorRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        organizador.setId(existing.getId());
        organizador.setCorreo(correo);
        if (organizador.getPassword() != null && !organizador.getPassword().isEmpty()) {
            organizador.setPassword(passwordEncoder.encode(organizador.getPassword()));
        } else {
            organizador.setPassword(existing.getPassword());
        }
        organizador.setRole("ORGANIZADOR");
        Organizador updated = organizadorRepository.save(organizador);
        return ResponseEntity.ok(updated);
    }

    // Eliminar un organizador
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteOrganizador(@PathVariable String correo) {
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador != null) {
            organizadorRepository.delete(organizador);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // POST: Registro con verificación de rol
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody Map<String, String> registerRequest) {
        String nombre = registerRequest.get("nombre");
        String correo = registerRequest.get("correo");
        String password = registerRequest.get("password");
        String selectedRole = registerRequest.get("role");

        // Validar campos obligatorios
        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre es obligatorio"));
        }
        if (correo == null || correo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo es obligatorio"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La contraseña es obligatoria"));
        }
        if (selectedRole == null || !"ORGANIZADOR".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (organizadorRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Organizador organizador = new Organizador();
        organizador.setNombre(nombre);
        organizador.setCorreo(correo);
        organizador.setPassword(passwordEncoder.encode(password));
        organizador.setRole("ORGANIZADOR");
        organizadorRepository.save(organizador);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}