package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.repository.ProveedorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/proveedores")
public class ProveedorRestController {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los proveedores
    @GetMapping
    public List<Proveedor> getAllProveedores() {
        return proveedorRepository.findAll();
    }

    @GetMapping("/{correo}")
    public ResponseEntity<Proveedor> getProveedorByCorreo(@PathVariable String correo) {
        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor != null) {
            return ResponseEntity.ok(proveedor);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Proveedor> createProveedor(@RequestBody Proveedor proveedor) {
        if (proveedorRepository.findByCorreo(proveedor.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        proveedor.setPassword(passwordEncoder.encode(proveedor.getPassword()));
        proveedor.setRole("PROVEEDOR");
        Proveedor saved = proveedorRepository.save(proveedor);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{correo}")
    public ResponseEntity<Proveedor> updateProveedor(@PathVariable String correo, @RequestBody Proveedor proveedor) {
        Proveedor existing = proveedorRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        proveedor.setId(existing.getId());
        proveedor.setCorreo(correo);
        if (proveedor.getPassword() != null && !proveedor.getPassword().isEmpty()) {
            proveedor.setPassword(passwordEncoder.encode(proveedor.getPassword()));
        } else {
            proveedor.setPassword(existing.getPassword());
        }
        proveedor.setRole("PROVEEDOR");
        Proveedor updated = proveedorRepository.save(proveedor);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteProveedor(@PathVariable String correo) {
        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor != null) {
            proveedorRepository.delete(proveedor);
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
        if (selectedRole == null || !"PROVEEDOR".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (proveedorRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre);
        proveedor.setCorreo(correo);
        proveedor.setPassword(passwordEncoder.encode(password));
        proveedor.setRole("PROVEEDOR");
        proveedorRepository.save(proveedor);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}