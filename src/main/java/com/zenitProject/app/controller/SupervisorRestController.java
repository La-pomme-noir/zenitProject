package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.SupervisorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/supervisores")
public class SupervisorRestController {

    @Autowired
    private SupervisorRepository supervisorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los supervisores
    @GetMapping
    public List<Supervisor> getAllSupervisores() {
        return supervisorRepository.findAll();
    }

    @GetMapping("/{correo}")
    public ResponseEntity<Supervisor> getSupervisorByCorreo(@PathVariable String correo) {
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor != null) {
            return ResponseEntity.ok(supervisor);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Supervisor> createSupervisor(@RequestBody Supervisor supervisor) {
        if (supervisorRepository.findByCorreo(supervisor.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        supervisor.setPassword(passwordEncoder.encode(supervisor.getPassword()));
        supervisor.setRole("SUPERVISOR");
        Supervisor saved = supervisorRepository.save(supervisor);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{correo}")
    public ResponseEntity<Supervisor> updateSupervisor(@PathVariable String correo, @RequestBody Supervisor supervisor) {
        Supervisor existing = supervisorRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        supervisor.setId(existing.getId());
        supervisor.setCorreo(correo);
        if (supervisor.getPassword() != null && !supervisor.getPassword().isEmpty()) {
            supervisor.setPassword(passwordEncoder.encode(supervisor.getPassword()));
        } else {
            supervisor.setPassword(existing.getPassword());
        }
        supervisor.setRole("SUPERVISOR");
        Supervisor updated = supervisorRepository.save(supervisor);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteSupervisor(@PathVariable String correo) {
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor != null) {
            supervisorRepository.delete(supervisor);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

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
        if (selectedRole == null || !"SUPERVISOR".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (supervisorRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Supervisor supervisor = new Supervisor();
        supervisor.setNombre(nombre);
        supervisor.setCorreo(correo);
        supervisor.setPassword(passwordEncoder.encode(password));
        supervisor.setRole("SUPERVISOR");
        supervisorRepository.save(supervisor);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}