package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.repository.InvitadoRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/invitados")
public class InvitadoRestController {

    @Autowired
    private InvitadoRepository invitadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los invitados
    @GetMapping
    public List<Invitado> getAllInvitados() {
        List<Invitado> invitados = invitadoRepository.findAll();
        System.out.println("Invitados devueltos por getAllInvitados: " + invitados); // Depuración
        return invitados;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Invitado> getInvitadoById(@PathVariable String id) {
        return invitadoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{correo}")
    public ResponseEntity<Invitado> getInvitadoByCorreo(@PathVariable String correo) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado != null) {
            return ResponseEntity.ok(invitado);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Invitado> createInvitado(@RequestBody Invitado invitado) {
        if (invitadoRepository.findByCorreo(invitado.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        invitado.setPassword(passwordEncoder.encode(invitado.getPassword()));
        invitado.setRole("INVITADO");
        Invitado saved = invitadoRepository.save(invitado);
        return ResponseEntity.ok(saved);
    }

    // Actualizar un invitado
    @PutMapping("/{correo}")
    public ResponseEntity<Invitado> updateInvitado(@PathVariable String correo, @RequestBody Invitado invitado) {
        Invitado existing = invitadoRepository.findByCorreo(invitado.getCorreo());
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        invitado.setId(existing.getId());
        invitado.setCorreo(correo);
        if (invitado.getPassword() != null && !invitado.getPassword().isEmpty()) {
            invitado.setPassword(passwordEncoder.encode(invitado.getPassword()));
        } else {
            invitado.setPassword(existing.getPassword());
        }
        invitado.setRole("INVITADO");
        Invitado updated = invitadoRepository.save(invitado);
        return ResponseEntity.ok(updated);
    }

    // Eliminar un invitado
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteInvitado(@PathVariable String correo) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado != null) {
            invitadoRepository.delete(invitado);
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

        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre es obligatorio"));
        }
        if (correo == null || correo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo es obligatorio"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La contraseña es obligatoria"));
        }
        if (selectedRole == null || !"INVITADO".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (invitadoRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Invitado invitado = new Invitado();
        invitado.setNombre(nombre);
        invitado.setCorreo(correo);
        invitado.setPassword(passwordEncoder.encode(password));
        invitado.setRole("INVITADO");
        invitadoRepository.save(invitado);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}