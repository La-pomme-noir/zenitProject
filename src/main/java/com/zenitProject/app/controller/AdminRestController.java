package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Admin;
import com.zenitProject.app.repository.AdminRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/admins")
public class AdminRestController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los administradores
    @GetMapping
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    // Obtener un administrador por correo
    @GetMapping("/{correo}")
    public ResponseEntity<Admin> getAdminByCorreo(@PathVariable String correo) {
        Admin admin = adminRepository.findByCorreo(correo);
        if (admin != null) {
            return ResponseEntity.ok(admin);
        }
        return ResponseEntity.notFound().build();
    }

    // Crear un nuevo administrador
    @PostMapping
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        if (adminRepository.findByCorreo(admin.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole("ADMIN");
        Admin saved = adminRepository.save(admin);
        return ResponseEntity.ok(saved);
    }

    // Actualizar un administrador
    @PutMapping("/{correo}")
    public ResponseEntity<Admin> updateAdmin(@PathVariable String correo, @RequestBody Admin admin) {
        Admin existing = adminRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        admin.setId(existing.getId());
        admin.setCorreo(correo);
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(existing.getPassword());
        }
        admin.setRole("ADMIN");
        Admin updated = adminRepository.save(admin);
        return ResponseEntity.ok(updated);
    }

    // Eliminar un administrador
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable String correo) {
        Admin admin = adminRepository.findByCorreo(correo);
        if (admin != null) {
            adminRepository.delete(admin);
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
        if (selectedRole == null || !"ADMIN".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (adminRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Admin admin = new Admin();
        admin.setNombre(nombre);
        admin.setCorreo(correo);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        adminRepository.save(admin);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}