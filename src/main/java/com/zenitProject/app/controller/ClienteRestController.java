package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.repository.ClienteRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los clientes
    @GetMapping
    public List<Cliente> getAllClientes() {
        return clienteRepository.findAll();
    }

    // Obtener un cliente por correo
    @GetMapping("/{correo}")
    public ResponseEntity<Cliente> getClienteByCorreo(@PathVariable String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente != null) {
            return ResponseEntity.ok(cliente);
        }
        return ResponseEntity.notFound().build();
    }

    // Crear un nuevo cliente
    @PostMapping
    public ResponseEntity<Cliente> createCliente(@RequestBody Cliente cliente) {
        if (clienteRepository.findByCorreo(cliente.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        cliente.setPassword(passwordEncoder.encode(cliente.getPassword()));
        cliente.setRole("CLIENTE");
        Cliente saved = clienteRepository.save(cliente);
        return ResponseEntity.ok(saved);
    }

    // Actualizar un cliente
    @PutMapping("/{correo}")
    public ResponseEntity<Cliente> updateCliente(@PathVariable String correo, @RequestBody Cliente cliente) {
        Cliente existing = clienteRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        cliente.setId(existing.getId());
        cliente.setCorreo(correo);
        if (cliente.getPassword() != null && !cliente.getPassword().isEmpty()) {
            cliente.setPassword(passwordEncoder.encode(cliente.getPassword()));
        } else {
            cliente.setPassword(existing.getPassword());
        }
        cliente.setRole("CLIENTE");
        Cliente updated = clienteRepository.save(cliente);
        return ResponseEntity.ok(updated);
    }

    // Eliminar un cliente
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteCliente(@PathVariable String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente != null) {
            clienteRepository.delete(cliente);
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
        if (selectedRole == null || !"CLIENTE".equals(selectedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido para registro"));
        }
        if (clienteRepository.findByCorreo(correo) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo ya está registrado"));
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setCorreo(correo);
        cliente.setPassword(passwordEncoder.encode(password));
        cliente.setRole("CLIENTE");
        clienteRepository.save(cliente);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}