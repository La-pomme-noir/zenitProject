package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.zenitProject.app.repository.EventoRepository;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.OrganizadorRepository;
import com.zenitProject.app.repository.ProveedorRepository;
import com.zenitProject.app.repository.SupervisorRepository;
import com.cloudinary.Cloudinary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/rest/organizadores")
public class OrganizadorRestController {

    @Autowired
    private OrganizadorRepository organizadorRepository;
    
    @Autowired
    private EventoRepository eventoRepository;
    
    @Autowired
    private Cloudinary cloudinary;
    
    @Autowired
    private InvitadoRepository invitadoRepository;
    
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
    
    // Obtener un evento por ID (modificado para permitir acceso a invitados)
    @GetMapping("/eventos/{id}")
    public ResponseEntity<Evento> getEventoById(@PathVariable String id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        Invitado invitado = invitadoRepository.findByCorreo(correo);

        Optional<Evento> eventoOpt = eventoRepository.findById(id);
        if (!eventoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Evento evento = eventoOpt.get();
        if (organizador != null && evento.getOrganizadorId() != null && evento.getOrganizadorId().equals(organizador.getId())) {
            return ResponseEntity.ok(evento);
        }

        if (invitado != null && evento.getInvitados() != null && evento.getInvitados().contains(invitado.getId())) {
            return ResponseEntity.ok(evento);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // Nuevo endpoint público para obtener un evento por ID
    @GetMapping("/public/eventos/{id}")
    public ResponseEntity<Evento> getEventoByIdPublic(@PathVariable String id) {
        return eventoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
 // Crear un evento
    @PostMapping(value = "/eventos", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createEvento(
            @RequestPart(value = "eventData", required = true) String eventDataStr,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Organizador no encontrado."));
        }

        try {
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
            evento.setEstadoAprobacion("Pendiente");

            @SuppressWarnings("unchecked")
            List<String> invitados = (List<String>) eventData.get("invitados");
            if (invitados != null) {
                evento.setInvitados(new ArrayList<>(invitados));
                // Sincronizar con la lista de invitaciones de los invitados
                for (String invitadoId : invitados) {
                    Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                    if (invitado != null) {
                        List<String> invitaciones = invitado.getInvitaciones();
                        if (!invitaciones.contains(evento.getId())) {
                            invitaciones.add(evento.getId());
                            invitado.setInvitaciones(invitaciones);
                            invitadoRepository.save(invitado);
                        }
                    }
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, String> proveedores = (Map<String, String>) eventData.get("proveedores");
            if (proveedores != null) {
                evento.setProveedores(new HashMap<>(proveedores));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ubicacionesPrecios = (List<Map<String, Object>>) eventData.get("ubicacionesPrecios");
            if (ubicacionesPrecios != null) {
                evento.setUbicacionesPrecios(ubicacionesPrecios);
            }

            if (imagen != null && !imagen.isEmpty()) {
                try {
                    Map<String, Object> uploadResult = cloudinary.uploader().upload(imagen.getBytes(), Map.of(
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "image"
                    ));
                    String imageUrl = (String) uploadResult.get("secure_url");
                    evento.setImagenUrl(imageUrl);
                    System.out.println("Imagen subida a Cloudinary con URL: " + imageUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al subir la imagen a Cloudinary: " + e.getMessage()));
                }
            }

            Evento savedEvento = eventoRepository.save(evento);
            // Sincronizar invitaciones después de guardar el evento (porque ahora tenemos el ID)
            if (invitados != null) {
                for (String invitadoId : invitados) {
                    Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                    if (invitado != null) {
                        List<String> invitaciones = invitado.getInvitaciones();
                        if (!invitaciones.contains(savedEvento.getId())) {
                            invitaciones.add(savedEvento.getId());
                            invitado.setInvitaciones(invitaciones);
                            invitadoRepository.save(invitado);
                        }
                    }
                }
            }
            System.out.println("Evento guardado con imagenUrl: " + savedEvento.getImagenUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Evento creado exitosamente.", "evento", savedEvento));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
        }
    }
    
    @PostMapping("/eventos/{eventoId}/add-invitado")
    public ResponseEntity<Map<String, String>> addInvitadoToEvento(@PathVariable String eventoId, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado"));
        }

        String correoOrganizador = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correoOrganizador);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Organizador no encontrado"));
        }

        String correoInvitado = request.get("correo");
        if (correoInvitado == null || correoInvitado.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo del invitado es obligatorio"));
        }

        Optional<Evento> eventoOpt = eventoRepository.findById(eventoId);
        if (!eventoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Evento no encontrado"));
        }

        Evento evento = eventoOpt.get();
        if (!evento.getOrganizadorId().equals(organizador.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No tienes permiso para modificar este evento"));
        }

        Invitado invitado = invitadoRepository.findByCorreo(correoInvitado);
        if (invitado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Invitado no encontrado"));
        }

        // Añadir el invitado al evento si no está ya
        List<String> invitados = evento.getInvitados();
        if (invitados == null) {
            invitados = new ArrayList<>();
            evento.setInvitados(invitados);
        }
        if (!invitados.contains(invitado.getId())) {
            invitados.add(invitado.getId());
            eventoRepository.save(evento);
        }

        // Sincronizar la lista de invitaciones del invitado
        List<String> invitaciones = invitado.getInvitaciones();
        if (invitaciones == null) {
            invitaciones = new ArrayList<>();
            invitado.setInvitaciones(invitaciones);
        }
        if (!invitaciones.contains(eventoId)) {
            invitaciones.add(eventoId);
            invitado.setInvitaciones(invitaciones); // Solo actualizamos las invitaciones
            invitadoRepository.save(invitado);
        }

        return ResponseEntity.ok(Map.of("message", "Invitado añadido al evento con éxito"));
    }
    
 // Actualizar evento
    @PutMapping(value = "/eventos/{id}", consumes = "application/json")
    public ResponseEntity<?> updateEvento(
            @PathVariable String id,
            @RequestBody Map<String, Object> eventData,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Organizador no encontrado."));
        }

        return eventoRepository.findById(id)
                .map(evento -> {
                    if (!evento.getOrganizadorId().equals(organizador.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No tienes permiso para editar este evento."));
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

                        // Sincronizar la lista de invitados
                        @SuppressWarnings("unchecked")
                        List<String> newInvitados = (List<String>) eventData.get("invitados");
                        if (newInvitados != null) {
                            // Determinar invitados eliminados
                            List<String> oldInvitados = evento.getInvitados();
                            Set<String> newInvitadosSet = new HashSet<>(newInvitados);
                            Set<String> oldInvitadosSet = new HashSet<>(oldInvitados);

                            // Invitados eliminados (en old pero no en new)
                            Set<String> invitadosEliminados = new HashSet<>(oldInvitadosSet);
                            invitadosEliminados.removeAll(newInvitadosSet);

                            // Invitados añadidos (en new pero no en old)
                            Set<String> invitadosAnadidos = new HashSet<>(newInvitadosSet);
                            invitadosAnadidos.removeAll(oldInvitadosSet);

                            // Actualizar invitaciones de los invitados eliminados
                            for (String invitadoId : invitadosEliminados) {
                                Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                                if (invitado != null) {
                                    invitado.getInvitaciones().remove(id);
                                    invitadoRepository.save(invitado);
                                }
                            }

                            // Actualizar invitaciones de los invitados añadidos
                            for (String invitadoId : invitadosAnadidos) {
                                Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                                if (invitado != null) {
                                    List<String> invitaciones = invitado.getInvitaciones();
                                    if (!invitaciones.contains(id)) {
                                        invitaciones.add(id);
                                        invitado.setInvitaciones(invitaciones);
                                        invitadoRepository.save(invitado);
                                    }
                                }
                            }

                            evento.setInvitados(new ArrayList<>(newInvitados));
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, String> newProveedores = (Map<String, String>) eventData.get("proveedores");
                        if (newProveedores != null) {
                            evento.setProveedores(new HashMap<>(newProveedores));
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> newUbicacionesPrecios = (List<Map<String, Object>>) eventData.get("ubicacionesPrecios");
                        if (newUbicacionesPrecios != null) {
                            evento.setUbicacionesPrecios(newUbicacionesPrecios);
                        }

                        Evento updatedEvento = eventoRepository.save(evento);
                        return ResponseEntity.ok(updatedEvento);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Evento no encontrado.")));
    }
    
 // Eliminar un evento
    @DeleteMapping("/eventos/{id}")
    public ResponseEntity<?> deleteEvento(@PathVariable String id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No estás autenticado. Por favor, inicia sesión."));
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Organizador no encontrado."));
        }

        return eventoRepository.findById(id)
                .map(evento -> {
                    if (!evento.getOrganizadorId().equals(organizador.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No tienes permiso para eliminar este evento."));
                    }
                    // Eliminar el evento de las invitaciones de los invitados
                    for (String invitadoId : evento.getInvitados()) {
                        Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                        if (invitado != null) {
                            invitado.getInvitaciones().remove(id);
                            invitado.getEventosAsistidos().remove(id);
                            invitadoRepository.save(invitado);
                        }
                    }
                    eventoRepository.deleteById(id);
                    long count = eventoRepository.countByOrganizadorId(organizador.getId());
                    organizador.setEventosOrganizados((int) count);
                    organizadorRepository.save(organizador);
                    return ResponseEntity.ok().body(Map.of("message", "Evento eliminado exitosamente."));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Evento no encontrado.")));
    }
    // Recuperar un evento (Sección de eventos)
    @GetMapping("/public/eventos")
    public ResponseEntity<Map<String, Object>> getAllEventos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Evento> eventosPage;

        if (search != null && !search.trim().isEmpty()) {
            eventosPage = eventoRepository.findByEstadoAprobacionAndNombreEventoContaining("Pendiente", search, pageable);
        } else {
            eventosPage = eventoRepository.findByEstadoAprobacion("Pendiente", pageable);
        }

        // Mapear eventos a un formato que incluya los campos necesarios
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
                    eventoMap.put("imagenUrl", evento.getImagenUrl());
                    eventoMap.put("ubicacionesPrecios", evento.getUbicacionesPrecios());
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