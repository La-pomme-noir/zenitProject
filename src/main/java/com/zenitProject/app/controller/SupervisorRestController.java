package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Evento;
import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.EventoRepository;
import com.zenitProject.app.repository.SupervisorRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest")
public class SupervisorRestController {

    @Autowired
    private SupervisorRepository supervisorRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los supervisores
    @GetMapping("/supervisores")
    public List<Supervisor> getAllSupervisores() {
        return supervisorRepository.findAll();
    }

    @GetMapping("/supervisores/{correo}")
    public ResponseEntity<Supervisor> getSupervisorByCorreo(@PathVariable String correo) {
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor != null) {
            return ResponseEntity.ok(supervisor);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/supervisores")
    public ResponseEntity<Supervisor> createSupervisor(@RequestBody Supervisor supervisor) {
        if (supervisorRepository.findByCorreo(supervisor.getCorreo()) != null) {
            return ResponseEntity.badRequest().build();
        }
        supervisor.setPassword(passwordEncoder.encode(supervisor.getPassword()));
        supervisor.setRole("SUPERVISOR");
        Supervisor saved = supervisorRepository.save(supervisor);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/supervisores/{correo}")
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

    @DeleteMapping("/supervisores/{correo}")
    public ResponseEntity<Void> deleteSupervisor(@PathVariable String correo) {
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor != null) {
            supervisorRepository.delete(supervisor);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/supervisores/register")
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

    // Actualizar el estado de un evento (solo para supervisores)
    @PutMapping("/eventos/{id}/estado")
    public ResponseEntity<Map<String, String>> updateEstadoEvento(
            @PathVariable String id,
            @RequestBody Map<String, String> estadoRequest,
            Authentication authentication) {
        // Verificar autenticación
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        // Verificar que el usuario sea un supervisor
        String correo = authentication.getName();
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para realizar esta acción."));
        }

        // Buscar el evento
        return eventoRepository.findById(id)
                .map(evento -> {
                    String nuevoEstado = estadoRequest.get("estadoAprobacion");
                    if (nuevoEstado == null || (!nuevoEstado.equals("Aprobado") && !nuevoEstado.equals("Desaprobado"))) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Estado no válido. Debe ser 'Aprobado' o 'Desaprobado'."));
                    }

                    evento.setEstadoAprobacion(nuevoEstado);
                    eventoRepository.save(evento);
                    return ResponseEntity.ok(Map.of("message", "Estado del evento actualizado exitosamente."));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Evento no encontrado.")));
    }

    // Nuevo endpoint para listar todos los eventos (solo para supervisores)
    @GetMapping("/supervisores/eventos")
    public ResponseEntity<List<Map<String, Object>>> getAllEventos(Authentication authentication) {
        // Verificar autenticación
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Verificar que el usuario sea un supervisor
        String correo = authentication.getName();
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // Obtener todos los eventos
        List<Evento> eventos = eventoRepository.findAll();

        // Mapear los eventos al formato esperado por el frontend
        List<Map<String, Object>> eventosFiltrados = eventos.stream()
                .map(evento -> {
                    Map<String, Object> eventoMap = new HashMap<>();
                    eventoMap.put("id", evento.getId());
                    eventoMap.put("nombreEvento", evento.getNombreEvento());
                    eventoMap.put("lugar", evento.getLugar());
                    eventoMap.put("ciudad", evento.getCiudad());
                    eventoMap.put("aforo", evento.getCantidadSillas());
                    eventoMap.put("organizadorNombre", evento.getOrganizadorNombre());
                    eventoMap.put("fecha", evento.getFecha());
                    eventoMap.put("hora", evento.getHora());
                    eventoMap.put("descripcion", evento.getDescripcion());
                    eventoMap.put("requisitos", evento.getRequisitos());
                    eventoMap.put("imagenUrl", evento.getImagenUrl());
                    eventoMap.put("ubicacionesPrecios", evento.getUbicacionesPrecios());
                    eventoMap.put("estadoAprobacion", evento.getEstadoAprobacion());
                    return eventoMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(eventosFiltrados);
    }
}