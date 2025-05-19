package com.zenitProject.app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.entidades.Evento;
import com.zenitProject.app.repository.ClienteRepository;
import com.zenitProject.app.repository.EventoRepository;

@RestController
@RequestMapping("/api/clientes")
public class ClienteApiController {
	
	@Autowired
    private ClienteRepository clienteRepository;
	
	@Autowired
    private EventoRepository eventoRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;
    
	// GET: Verificar si el usuario está autenticado y obtener su rol
    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            String correo = authentication.getName();
            Cliente cliente = clienteRepository.findByCorreo(correo);
            if (cliente != null) {
                response.put("isAuthenticated", true);
                response.put("role", cliente.getRole());
                response.put("correo", cliente.getCorreo());
                return ResponseEntity.ok(response);
            }
        }
        response.put("isAuthenticated", false);
        response.put("role", null);
        return ResponseEntity.ok(response);
    }
    
	// GET: Obtener datos del cliente autenticado para la UI
    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getClienteProfile(Authentication authentication) {
        String correo = authentication.getName();
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> profile = new HashMap<>();
        profile.put("correo", cliente.getCorreo());
        profile.put("nombre", cliente.getNombre());
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
    
    // POST: Registrar asistencia a un evento
    @PostMapping("/register-event")
    public ResponseEntity<Map<String, String>> registerEvent(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No estás autenticado."));
        }

        String correo = authentication.getName();
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null || !cliente.getRole().equals("CLIENTE")) {
            return ResponseEntity.status(403).body(Map.of("message", "Acceso denegado. Debes ser un cliente."));
        }

        String eventoId = request.get("eventoId");
        Evento evento = eventoRepository.findById(eventoId).orElse(null);
        if (evento == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Evento no encontrado."));
        }

        List<String> eventosRegistrados = cliente.getEventosRegistrados();
        if (eventosRegistrados.contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ya estás registrado en este evento."));
        }

        // Validar aforo
        if (evento.getCantidadSillas() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lo sentimos, las entradas para este evento están agotadas."));
        }

        // Reducir aforo
        evento.setCantidadSillas(evento.getCantidadSillas() - 1);
        eventoRepository.save(evento);

        // Registrar evento en el cliente
        eventosRegistrados.add(eventoId);
        cliente.setEventosRegistrados(eventosRegistrados);
        clienteRepository.save(cliente);

        return ResponseEntity.ok(Map.of("message", "Asistencia registrada exitosamente."));
    }
    
    // DELETE: Cancelar asistencia a un evento
    @DeleteMapping("/cancel-event/{eventoId}")
    public ResponseEntity<Map<String, String>> cancelEvent(
            @PathVariable String eventoId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No estás autenticado."));
        }

        String correo = authentication.getName();
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null || !cliente.getRole().equals("CLIENTE")) {
            return ResponseEntity.status(403).body(Map.of("message", "Acceso denegado. Debes ser un cliente."));
        }

        Evento evento = eventoRepository.findById(eventoId).orElse(null);
        if (evento == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Evento no encontrado."));
        }

        List<String> eventosRegistrados = cliente.getEventosRegistrados();
        if (!eventosRegistrados.contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "No estás registrado en este evento."));
        }

        // Incrementar aforo
        evento.setCantidadSillas(evento.getCantidadSillas() + 1);
        eventoRepository.save(evento);

        // Eliminar evento de la lista del cliente
        eventosRegistrados.remove(eventoId);
        cliente.setEventosRegistrados(eventosRegistrados);
        clienteRepository.save(cliente);

        return ResponseEntity.ok(Map.of("message", "Asistencia cancelada exitosamente."));
    }
    
    // GET: Obtener eventos registrados del cliente
    @GetMapping("/registered-events")
    public ResponseEntity<List<Evento>> getRegisteredEvents(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(List.of());
        }

        String correo = authentication.getName();
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente == null || !cliente.getRole().equals("CLIENTE")) {
            return ResponseEntity.status(403).body(List.of());
        }

        List<String> eventosRegistrados = cliente.getEventosRegistrados();
        List<Evento> eventos = eventoRepository.findAllById(eventosRegistrados);
        return ResponseEntity.ok(eventos);
    }
}