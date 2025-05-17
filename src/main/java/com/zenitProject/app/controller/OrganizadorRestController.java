package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

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
    @PostMapping(value = "/eventos", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createEvento(
            @RequestPart(value = "eventData", required = true) String eventDataStr,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Organizador no encontrado."));
        }

        try {
            // Parsear el eventData desde el String JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> eventData = objectMapper.readValue(eventDataStr, new TypeReference<Map<String, Object>>() {});

            Evento evento = new Evento();
            evento.setOrganizadorId(organizador.getId());
            evento.setOrganizadorNombre(organizador.getNombre());
            evento.setNombreEvento((String) eventData.get("nombreEvento"));
            evento.setFecha((String) eventData.get("fecha"));
            evento.setLugar((String) eventData.get("lugar"));
            evento.setHora((String) eventData.get("hora"));
            evento.setCiudad((String) eventData.get("ciudad"));
            evento.setCantidadSillas((Integer) eventData.get("cantidadSillas"));
            evento.setDescripcion((String) eventData.get("descripcion"));
            evento.setRequisitos((String) eventData.get("requisitos"));
            evento.setEstadoAprobacion("Pendiente"); // Ajusta según tu lógica

            // Manejar invitados
            @SuppressWarnings("unchecked")
            List<String> invitados = (List<String>) eventData.get("invitados");
            if (invitados != null) {
                evento.setInvitados(new ArrayList<>(invitados));
            }

            // Manejar proveedores
            @SuppressWarnings("unchecked")
            Map<String, String> proveedores = (Map<String, String>) eventData.get("proveedores");
            if (proveedores != null) {
                evento.setProveedores(new HashMap<>(proveedores));
            }

            // Manejar ubicacionesPrecios
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ubicacionesPrecios = (List<Map<String, Object>>) eventData.get("ubicacionesPrecios");
            if (ubicacionesPrecios != null) {
                evento.setUbicacionesPrecios(ubicacionesPrecios);
            }

            // Manejar la imagen
            if (imagen != null && !imagen.isEmpty()) {
                try {
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                        System.out.println("Directorio creado: " + uploadPath.toAbsolutePath());
                    }

                    // Imprimir el nombre original del archivo subido
                    System.out.println("Nombre original del archivo subido: " + imagen.getOriginalFilename());

                    // Generar un UUID y construir el nombre del archivo
                    String uuid = UUID.randomUUID().toString();
                    String originalFileName = imagen.getOriginalFilename();
                    String fileName = uuid + "_" + (originalFileName != null ? originalFileName : "default.jpg");
                    Path filePath = uploadPath.resolve(fileName);

                    // Guardar la imagen
                    System.out.println("Guardando imagen en: " + filePath.toAbsolutePath());
                    Files.write(filePath, imagen.getBytes());
                    System.out.println("Imagen guardada exitosamente en: " + filePath.toAbsolutePath());

                    // Generar la URL con el mismo nombre
                    String imageUrl = "/uploads/" + fileName;
                    System.out.println("URL generada para la imagen: " + imageUrl);
                    evento.setImagenUrl(imageUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(500).body(Map.of("message", "Error al guardar la imagen: " + e.getMessage()));
                }
            } else {
                System.out.println("No se proporcionó ninguna imagen para el evento.");
                evento.setImagenUrl(null); // O un valor por defecto si lo deseas
            }

            Evento savedEvento = eventoRepository.save(evento);
            System.out.println("Evento guardado con imagenUrl: " + savedEvento.getImagenUrl());
            return ResponseEntity.status(201).body(Map.of("message", "Evento creado exitosamente.", "evento", savedEvento));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
        }
    }
    
    //Actualizar evento
    @PutMapping(value = "/eventos/{id}", consumes = "application/json")
    public ResponseEntity<?> updateEvento(
            @PathVariable String id,
            @RequestBody Map<String, Object> eventData,
            Authentication authentication) {
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

                    try {
                        evento.setNombreEvento((String) eventData.get("nombreEvento"));
                        evento.setFecha((String) eventData.get("fecha"));
                        evento.setLugar((String) eventData.get("lugar"));
                        evento.setHora((String) eventData.get("hora"));
                        evento.setCiudad((String) eventData.get("ciudad"));
                        evento.setCantidadSillas((Integer) eventData.get("cantidadSillas"));
                        evento.setDescripcion((String) eventData.get("descripcion"));
                        evento.setRequisitos((String) eventData.get("requisitos"));
                        evento.setEstadoAprobacion((String) eventData.get("estadoAprobacion"));

                        // Actualizar invitados
                        @SuppressWarnings("unchecked")
                        List<String> newInvitados = (List<String>) eventData.get("invitados");
                        if (newInvitados != null) {
                            evento.setInvitados(new ArrayList<>(newInvitados));
                        }

                        // Actualizar proveedores
                        @SuppressWarnings("unchecked")
                        Map<String, String> newProveedores = (Map<String, String>) eventData.get("proveedores");
                        if (newProveedores != null) {
                            evento.setProveedores(new HashMap<>(newProveedores));
                        }

                        // Actualizar ubicacionesPrecios
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> newUbicacionesPrecios = (List<Map<String, Object>>) eventData.get("ubicacionesPrecios");
                        if (newUbicacionesPrecios != null) {
                            evento.setUbicacionesPrecios(newUbicacionesPrecios);
                        }

                        // No se procesa la imagen (se mantiene la existente)
                        Evento updatedEvento = eventoRepository.save(evento);
                        return ResponseEntity.ok(updatedEvento);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(400).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
                    }
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