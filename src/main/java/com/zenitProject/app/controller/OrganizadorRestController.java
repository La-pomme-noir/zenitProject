package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;

import com.zenitProject.app.entidades.Evento;
import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.entidades.Organizador;
import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.EventoRepository;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.OrganizadorRepository;
import com.zenitProject.app.repository.ProveedorRepository;
import com.zenitProject.app.repository.SupervisorRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/organizadores")
public class OrganizadorRestController {

    @Autowired
    private OrganizadorRepository organizadorRepository;
    
    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private InvitadoRepository invitadoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private SupervisorRepository supervisorRepository;

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
        organizador.setEventosOrganizados(0); // Inicializar a 0
        Organizador saved = organizadorRepository.save(organizador);
        return ResponseEntity.ok(saved);
    }
    
    // Nuevo endpoint para listar eventos del organizador autenticado
    @GetMapping("/eventos")
    public ResponseEntity<List<Evento>> getEventosByOrganizador(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }

        List<Evento> eventos = eventoRepository.findByOrganizadorId(organizador.getId());
        return ResponseEntity.ok(eventos);
    }
    
    // Obtener un evento por ID
    @GetMapping("/eventos/{id}")
    public ResponseEntity<Evento> getEventoById(@PathVariable String id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).build();
        }

        return eventoRepository.findById(id)
                .filter(evento -> evento.getOrganizadorId().equals(organizador.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
    
    // Nuevo endpoint para crear un evento
    @PostMapping("/eventos")
    public ResponseEntity<?> createEvento(@RequestBody Map<String, Object> eventData, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "No estás autenticado. Por favor, inicia sesión.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Organizador no encontrado.");
            return ResponseEntity.notFound().build();
        }

        // Crear el evento
        Evento evento = new Evento();
        evento.setNombreEvento((String) eventData.get("nombreEvento"));
        evento.setFecha((String) eventData.get("fecha"));
        evento.setCantidadSillas((Integer) eventData.get("cantidadSillas"));
        evento.setProveedores((Map<String, String>) eventData.get("proveedores"));
        evento.setLugar((String) eventData.get("lugar"));
        evento.setHora((String) eventData.get("hora"));
        evento.setCiudad((String) eventData.get("ciudad"));
        evento.setDescripcion((String) eventData.get("descripcion"));
        evento.setRequisitos((String) eventData.get("requisitos"));
        evento.setOrganizadorId(organizador.getId());
        evento.setOrganizadorNombre(organizador.getNombre());

        // Manejar invitados (esperamos IDs, no nombres)
        @SuppressWarnings("unchecked")
        List<String> invitadosIds = (List<String>) eventData.get("invitados");
        if (invitadosIds != null && !invitadosIds.isEmpty()) {
            for (String invitadoId : invitadosIds) {
                Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                if (invitado != null) {
                    // invitaciones ya está inicializado, agregar el ID del evento
                    invitado.getInvitaciones().add(evento.getId());
                    invitadoRepository.save(invitado);
                } else {
                    System.out.println("Invitado no encontrado con ID: " + invitadoId); // Depuración
                }
            }
            evento.setInvitados(invitadosIds); // Asegúrate de que Evento tenga este campo
        }

        // Validar que los proveedores existan (opcional, según tu lógica)
        if (evento.getProveedores() != null) {
            for (Map.Entry<String, String> entry : evento.getProveedores().entrySet()) {
                String proveedorId = entry.getValue();
                if (proveedorId != null && !proveedorId.isEmpty()) {
                    Proveedor proveedor = proveedorRepository.findById(proveedorId).orElse(null);
                    if (proveedor == null) {
                        System.out.println("Proveedor no encontrado con ID: " + proveedorId); // Depuración
                        // Opcional: return ResponseEntity.badRequest().body(Map.of("message", "Proveedor no encontrado: " + proveedorId));
                    }
                }
            }
        }

        // Guardar el evento
        Evento savedEvento = eventoRepository.save(evento);

        // Actualizar eventosOrganizados
        long count = eventoRepository.countByOrganizadorId(organizador.getId());
        organizador.setEventosOrganizados((int) count);
        organizadorRepository.save(organizador);

        // Notificar a los supervisores
        List<Supervisor> supervisores = supervisorRepository.findAll();
        for (Supervisor supervisor : supervisores) {
            if (supervisor.getEventosAprobados() == null) {
                supervisor.setEventosAprobados(new ArrayList<>());
            }
            supervisor.getEventosAprobados().add(savedEvento.getId());
            supervisorRepository.save(supervisor);
        }

        return ResponseEntity.ok(Map.of("message", "Evento creado exitosamente", "id", savedEvento.getId()));
    }
    
    //Actualizar evento
    @PutMapping("/eventos/{id}")
    public ResponseEntity<?> updateEvento(@PathVariable String id, @RequestBody Map<String, Object> eventData, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Organizador no encontrado."));
        }

        return eventoRepository.findById(id)
                .map(evento -> {
                    if (!evento.getOrganizadorId().equals(organizador.getId())) {
                        return ResponseEntity.status(403).body(Map.of("message", "No tienes permiso para editar este evento."));
                    }
                    evento.setNombreEvento((String) eventData.get("nombreEvento"));
                    evento.setFecha((String) eventData.get("fecha"));
                    evento.setLugar((String) eventData.get("lugar"));
                    evento.setHora((String) eventData.get("hora"));
                    evento.setCiudad((String) eventData.get("ciudad"));
                    evento.setCantidadSillas((Integer) eventData.get("cantidadSillas"));
                    evento.setDescripcion((String) eventData.get("descripcion"));
                    evento.setRequisitos((String) eventData.get("requisitos"));

                    // Actualizar invitados
                    @SuppressWarnings("unchecked")
                    List<String> newInvitados = (List<String>) eventData.get("invitados");
                    if (newInvitados != null) {
                        // Limpiar invitados existentes y agregar nuevos
                        evento.setInvitados(new ArrayList<>(newInvitados)); // Reemplaza la lista completa
                    }

                    // Actualizar proveedores
                    @SuppressWarnings("unchecked")
                    Map<String, String> newProveedores = (Map<String, String>) eventData.get("proveedores");
                    if (newProveedores != null) {
                        evento.setProveedores(new HashMap<>(newProveedores)); // Reemplaza el mapa completo
                    }

                    Evento updatedEvento = eventoRepository.save(evento);
                    return ResponseEntity.ok(updatedEvento);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Evento no encontrado.")));
    }
    
    // Eliminar un evento
    @DeleteMapping("/eventos/{id}")
    public ResponseEntity<?> deleteEvento(@PathVariable String id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Organizador no encontrado."));
        }

        return eventoRepository.findById(id)
                .map(evento -> {
                    if (!evento.getOrganizadorId().equals(organizador.getId())) {
                        return ResponseEntity.status(403).body(Map.of("message", "No tienes permiso para eliminar este evento."));
                    }
                    eventoRepository.deleteById(id);
                    // Actualizar eventosOrganizados
                    long count = eventoRepository.countByOrganizadorId(organizador.getId());
                    organizador.setEventosOrganizados((int) count);
                    organizadorRepository.save(organizador);
                    return ResponseEntity.ok().body(Map.of("message", "Evento eliminado exitosamente."));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Evento no encontrado.")));
    }
    
    // Recuperar un evento (Sección de eventos)
    @GetMapping("/public/eventos")
    public ResponseEntity<Map<String, Object>> getAllEventos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Evento> eventosPage = eventoRepository.findByEstadoAprobacion("Pendiente", pageable);

        // Mapear eventos a un formato que excluya campos sensibles
        List<Map<String, Object>> eventosFiltrados = eventosPage.getContent().stream()
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
                    return eventoMap;
                })
                .collect(Collectors.toList());

        // Construir la respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("eventos", eventosFiltrados);
        response.put("currentPage", eventosPage.getNumber());
        response.put("totalItems", eventosPage.getTotalElements());
        response.put("totalPages", eventosPage.getTotalPages());

        return ResponseEntity.ok(response);
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
        organizador.setEventosOrganizados(0);
        organizadorRepository.save(organizador);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }
}