package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.entidades.Evento;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.EventoRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/invitados")
public class InvitadoRestController {

    @Autowired
    private InvitadoRepository invitadoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los invitados
    @GetMapping
    public List<Invitado> getAllInvitados() {
        List<Invitado> invitados = invitadoRepository.findAll();
        System.out.println("Invitados devueltos por getAllInvitados: " + invitados);
        return invitados;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Invitado> getInvitadoById(@PathVariable String id) {
        return invitadoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<Invitado> getInvitadoByCorreo(@PathVariable String correo) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado != null) {
            // Limpiar invitaciones inválidas
            List<String> validInvitations = invitado.getInvitaciones().stream()
                .filter(eventoId -> eventoId != null && eventoRepository.existsById(eventoId))
                .collect(Collectors.toList());
            if (validInvitations.size() != invitado.getInvitaciones().size()) {
                invitado.setInvitaciones(validInvitations);
                invitadoRepository.save(invitado);
            }
            // Limpiar eventos asistidos inválidos
            List<String> validEventosAsistidos = invitado.getEventosAsistidos().stream()
                .filter(eventoId -> eventoId != null && eventoRepository.existsById(eventoId))
                .collect(Collectors.toList());
            if (validEventosAsistidos.size() != invitado.getEventosAsistidos().size()) {
                invitado.setEventosAsistidos(validEventosAsistidos);
                invitadoRepository.save(invitado);
            }
            return ResponseEntity.ok(invitado);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping
    public ResponseEntity<Invitado> createInvitado(@RequestBody Invitado invitado) {
        if (invitadoRepository.findByCorreo(invitado.getCorreo()) != null) {
            return ResponseEntity.badRequest().body(null);
        }
        invitado.setPassword(passwordEncoder.encode(invitado.getPassword()));
        invitado.setRole("INVITADO");
        Invitado saved = invitadoRepository.save(invitado);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{correo}")
    public ResponseEntity<Invitado> updateInvitado(@PathVariable String correo, @RequestBody Invitado invitado) {
        Invitado existing = invitadoRepository.findByCorreo(correo);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // Actualizar solo los campos proporcionados, sin sobrescribir el password a menos que sea explícito
        invitado.setId(existing.getId());
        invitado.setCorreo(correo);
        invitado.setRole("INVITADO");

        // Preservar el password existente si no se proporciona uno nuevo
        if (invitado.getPassword() == null || invitado.getPassword().isEmpty()) {
            invitado.setPassword(existing.getPassword());
        } else {
            invitado.setPassword(passwordEncoder.encode(invitado.getPassword()));
        }

        // Preservar otros campos si no se proporcionan
        if (invitado.getNombre() == null) {
            invitado.setNombre(existing.getNombre());
        }
        if (invitado.getInvitaciones() == null) {
            invitado.setInvitaciones(existing.getInvitaciones());
        }
        if (invitado.getEventosAsistidos() == null) {
            invitado.setEventosAsistidos(existing.getEventosAsistidos());
        }

        Invitado updated = invitadoRepository.save(invitado);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteInvitado(@PathVariable String correo) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado != null) {
            invitadoRepository.delete(invitado);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

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

    @PostMapping("/{correo}/accept-invitation/{eventoId}")
    public ResponseEntity<Map<String, String>> acceptInvitation(@PathVariable String correo, @PathVariable String eventoId) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Invitado no encontrado"));
        }

        if (!invitado.getInvitaciones().contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "El evento no está en las invitaciones del invitado"));
        }

        invitado.getInvitaciones().remove(eventoId);
        invitado.getEventosAsistidos().add(eventoId);
        invitadoRepository.save(invitado);

        return ResponseEntity.ok(Map.of("message", "Invitación aceptada con éxito"));
    }

    @PostMapping("/{correo}/decline-invitation/{eventoId}")
    public ResponseEntity<Map<String, String>> declineInvitation(@PathVariable String correo, @PathVariable String eventoId) {
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Invitado no encontrado"));
        }

        if (!invitado.getInvitaciones().contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "El evento no está en las invitaciones del invitado"));
        }

        invitado.getInvitaciones().remove(eventoId);
        invitadoRepository.save(invitado);

        Optional<Evento> eventoOpt = eventoRepository.findById(eventoId);
        if (eventoOpt.isPresent()) {
            Evento evento = eventoOpt.get();
            evento.getInvitados().remove(invitado.getId());
            eventoRepository.save(evento);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Evento no encontrado"));
        }

        return ResponseEntity.ok(Map.of("message", "Invitación declinada con éxito"));
    }
}