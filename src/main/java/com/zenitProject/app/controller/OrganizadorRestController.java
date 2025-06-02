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
import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.repository.EventoRepository;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.OrganizadorRepository;
import com.zenitProject.app.repository.ProveedorRepository;
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
import java.util.regex.Pattern;

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
    private ProveedorRepository proveedorRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

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
        organizador.setEventosOrganizados(0);
        Organizador saved = organizadorRepository.save(organizador);
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/eventos")
    public ResponseEntity<List<Map<String, Object>>> getEventosByOrganizador(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        String correo = authentication.getName();
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador == null) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }

        List<Evento> eventos = eventoRepository.findByOrganizadorId(organizador.getId());
        List<Map<String, Object>> eventosResponse = eventos.stream().map(evento -> {
            Map<String, Object> eventoMap = new HashMap<>();
            eventoMap.put("id", evento.getId());
            eventoMap.put("nombreEvento", evento.getNombreEvento());
            eventoMap.put("fecha", evento.getFecha());
            eventoMap.put("lugar", evento.getLugar());
            eventoMap.put("organizadorNombre", evento.getOrganizadorNombre());
            eventoMap.put("invitados", evento.getInvitados());
            eventoMap.put("cantidadSillas", evento.getCantidadSillas());
            eventoMap.put("hora", evento.getHora());
            eventoMap.put("ciudad", evento.getCiudad());
            eventoMap.put("descripcion", evento.getDescripcion());
            eventoMap.put("requisitos", evento.getRequisitos());
            eventoMap.put("estadoAprobacion", evento.getEstadoAprobacion());
            eventoMap.put("imagenUrl", evento.getImagenUrl());
            eventoMap.put("ubicacionesPrecios", evento.getUbicacionesPrecios());

            Map<String, Map<String, String>> proveedoresTransformados = new HashMap<>();
            if (evento.getProveedores() != null && !evento.getProveedores().isEmpty()) {
                for (Map.Entry<String, String> entry : evento.getProveedores().entrySet()) {
                    String proveedorId = entry.getKey();
                    String categoria = entry.getValue();
                    Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                    if (proveedorOpt.isPresent()) {
                        Proveedor proveedor = proveedorOpt.get();
                        Map<String, String> proveedorInfo = new HashMap<>();
                        proveedorInfo.put("nombre", proveedor.getNombre() != null ? proveedor.getNombre() : "Sin nombre");
                        proveedorInfo.put("categoria", categoria);
                        proveedoresTransformados.put(proveedorId, proveedorInfo);
                    } else {
                        Map<String, String> proveedorInfo = new HashMap<>();
                        proveedorInfo.put("nombre", "Proveedor no encontrado");
                        proveedorInfo.put("categoria", categoria);
                        proveedoresTransformados.put(proveedorId, proveedorInfo);
                    }
                }
            }
            eventoMap.put("proveedores", proveedoresTransformados);

            return eventoMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(eventosResponse);
    }
    
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
    
    @GetMapping("/public/eventos/{id}")
    public ResponseEntity<Evento> getEventoByIdPublic(@PathVariable String id) {
        return eventoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
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
            System.out.println("Datos recibidos en eventData: " + eventData);

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
            if (invitados != null && !invitados.isEmpty()) {
                evento.setInvitados(new ArrayList<>(invitados));
            }

            @SuppressWarnings("unchecked")
            Map<String, String> proveedores = (Map<String, String>) eventData.get("proveedores");
            Map<String, String> proveedoresValidos = new HashMap<>();
            List<String> proveedoresNoEncontrados = new ArrayList<>();
            List<String> proveedoresInvalidos = new ArrayList<>();

            // Expresión regular para validar IDs de MongoDB (24 caracteres hexadecimales)
            Pattern mongoIdPattern = Pattern.compile("^[0-9a-fA-F]{24}$");

            if (proveedores != null && !proveedores.isEmpty()) {
                for (Map.Entry<String, String> entry : proveedores.entrySet()) {
                    String proveedorId = entry.getKey();
                    // Validar que el ID tenga el formato correcto
                    if (!mongoIdPattern.matcher(proveedorId).matches()) {
                        proveedoresInvalidos.add(proveedorId);
                        System.out.println("ID de proveedor inválido (no es un ID de MongoDB): " + proveedorId);
                        continue;
                    }

                    Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                    if (proveedorOpt.isPresent()) {
                        Proveedor proveedor = proveedorOpt.get();
                        // Validar que la categoría enviada coincida con la categoría del proveedor
                        String categoriaEnviada = entry.getValue();
                        if (categoriaEnviada != null && categoriaEnviada.equals(proveedor.getCategoria())) {
                            proveedoresValidos.put(proveedorId, categoriaEnviada);
                        } else {
                            proveedoresNoEncontrados.add(proveedorId + " (categoría no coincide: esperada " + proveedor.getCategoria() + ", recibida " + categoriaEnviada + ")");
                            System.out.println("Proveedor " + proveedorId + " encontrado, pero la categoría no coincide: esperada " + proveedor.getCategoria() + ", recibida " + categoriaEnviada);
                        }
                    } else {
                        proveedoresNoEncontrados.add(proveedorId);
                        System.out.println("Proveedor con ID " + proveedorId + " no encontrado al crear evento.");
                    }
                }
                evento.setProveedores(proveedoresValidos);
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
            String eventoId = savedEvento.getId();
            System.out.println("Evento creado con ID: " + eventoId);

            if (invitados != null && !invitados.isEmpty()) {
                for (String invitadoId : invitados) {
                    Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                    if (invitado != null) {
                        List<String> invitaciones = invitado.getInvitaciones();
                        if (invitaciones == null) {
                            invitaciones = new ArrayList<>();
                        }
                        if (!invitaciones.contains(eventoId)) {
                            invitaciones.add(eventoId);
                            invitado.setInvitaciones(invitaciones);
                            invitadoRepository.save(invitado);
                            System.out.println("Invitado " + invitadoId + " actualizado - Evento " + eventoId + " añadido a invitaciones.");
                        }
                    } else {
                        System.out.println("Invitado con ID " + invitadoId + " no encontrado.");
                    }
                }
            }

            if (!proveedoresValidos.isEmpty()) {
                for (String proveedorId : proveedoresValidos.keySet()) {
                    Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                    if (proveedorOpt.isPresent()) {
                        Proveedor proveedor = proveedorOpt.get();
                        List<String> pendientes = proveedor.getEventosPendientes();
                        if (pendientes == null) {
                            pendientes = new ArrayList<>();
                        }
                        if (!pendientes.contains(eventoId)) {
                            pendientes.add(eventoId);
                            proveedor.setEventosPendientes(pendientes);
                            proveedorRepository.save(proveedor);
                            System.out.println("Proveedor " + proveedorId + " actualizado - Evento " + eventoId + " añadido a eventosPendientes: " + pendientes);
                        } else {
                            System.out.println("Evento " + eventoId + " ya está en eventosPendientes de proveedor " + proveedorId);
                        }
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Evento creado exitosamente.");
            response.put("evento", savedEvento);
            if (!proveedoresInvalidos.isEmpty()) {
                response.put("error", "IDs de proveedores inválidos (deben ser IDs de MongoDB): " + proveedoresInvalidos);
            }
            if (!proveedoresNoEncontrados.isEmpty()) {
                response.put("warning", "Algunos proveedores no fueron encontrados o no coinciden con la categoría: " + proveedoresNoEncontrados);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error inesperado: " + e.getMessage()));
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

        List<String> invitados = evento.getInvitados();
        if (invitados == null) {
            invitados = new ArrayList<>();
        }
        if (!invitados.contains(invitado.getId())) {
            invitados.add(invitado.getId());
            evento.setInvitados(invitados);
            eventoRepository.save(evento);
        }

        List<String> invitaciones = invitado.getInvitaciones();
        if (invitaciones == null) {
            invitaciones = new ArrayList<>();
        }
        if (!invitaciones.contains(eventoId)) {
            invitaciones.add(eventoId);
            invitado.setInvitaciones(invitaciones);
            invitadoRepository.save(invitado);
        }

        return ResponseEntity.ok(Map.of("message", "Invitado añadido al evento con éxito"));
    }
    
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

                    String estadoAprobacion = (String) eventData.get("estadoAprobacion");
                    if (estadoAprobacion != null && !"Pendiente".equals(estadoAprobacion)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "No puedes modificar proveedores de un evento que no está en estado Pendiente."));
                    }

                    try {
                        // Actualizar los campos básicos del evento
                        evento.setNombreEvento((String) eventData.get("nombreEvento"));
                        evento.setFecha((String) eventData.get("fecha"));
                        evento.setLugar((String) eventData.get("lugar"));
                        evento.setHora((String) eventData.get("hora"));
                        evento.setCiudad((String) eventData.get("ciudad"));
                        evento.setCantidadSillas((Integer) eventData.get("cantidadSillas"));
                        evento.setDescripcion((String) eventData.get("descripcion"));
                        evento.setRequisitos((String) eventData.get("requisitos"));
                        evento.setEstadoAprobacion("Pendiente");

                        // Manejar invitados
                        @SuppressWarnings("unchecked")
                        List<String> newInvitados = (List<String>) eventData.get("invitados");
                        List<String> invitadosNoEncontrados = new ArrayList<>();
                        if (newInvitados != null) {
                            List<String> oldInvitados = evento.getInvitados();
                            Set<String> newInvitadosSet = new HashSet<>(newInvitados);
                            Set<String> oldInvitadosSet = oldInvitados != null ? new HashSet<>(oldInvitados) : new HashSet<>();

                            // Invitados eliminados
                            Set<String> invitadosEliminados = new HashSet<>(oldInvitadosSet);
                            invitadosEliminados.removeAll(newInvitadosSet);

                            // Invitados añadidos
                            Set<String> invitadosAnadidos = new HashSet<>(newInvitadosSet);
                            invitadosAnadidos.removeAll(oldInvitadosSet);

                            // Actualizar invitaciones de los invitados eliminados
                            for (String invitadoId : invitadosEliminados) {
                                Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                                if (invitado != null) {
                                    List<String> invitaciones = invitado.getInvitaciones();
                                    if (invitaciones != null && invitaciones.contains(id)) {
                                        invitaciones.remove(id);
                                        invitado.setInvitaciones(invitaciones);
                                        invitadoRepository.save(invitado);
                                        System.out.println("Invitado " + invitadoId + " actualizado - Evento " + id + " eliminado de invitaciones.");
                                    }
                                } else {
                                    System.out.println("Invitado " + invitadoId + " no encontrado al eliminar evento " + id);
                                }
                            }

                            // Actualizar invitaciones de los invitados añadidos
                            for (String invitadoId : invitadosAnadidos) {
                                Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                                if (invitado != null) {
                                    List<String> invitaciones = invitado.getInvitaciones();
                                    if (invitaciones == null) {
                                        invitaciones = new ArrayList<>();
                                    }
                                    if (!invitaciones.contains(id)) {
                                        invitaciones.add(id);
                                        invitado.setInvitaciones(invitaciones);
                                        invitadoRepository.save(invitado);
                                        System.out.println("Invitado " + invitadoId + " actualizado - Evento " + id + " añadido a invitaciones.");
                                    }
                                } else {
                                    invitadosNoEncontrados.add(invitadoId);
                                    System.out.println("Invitado " + invitadoId + " no encontrado al añadir evento " + id);
                                }
                            }

                            evento.setInvitados(new ArrayList<>(newInvitados));
                        }

                        // Manejar proveedores
                        @SuppressWarnings("unchecked")
                        Map<String, String> newProveedores = (Map<String, String>) eventData.get("proveedores");
                        Map<String, String> proveedoresValidos = new HashMap<>();
                        List<String> proveedoresNoEncontrados = new ArrayList<>();

                        if (newProveedores != null) {
                            for (Map.Entry<String, String> entry : newProveedores.entrySet()) {
                                String proveedorId = entry.getKey();
                                Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                                if (proveedorOpt.isPresent()) {
                                    proveedoresValidos.put(proveedorId, entry.getValue());
                                } else {
                                    proveedoresNoEncontrados.add(proveedorId);
                                    System.out.println("Proveedor " + proveedorId + " no encontrado al actualizar evento " + id);
                                }
                            }

                            Map<String, String> oldProveedores = evento.getProveedores();
                            Set<String> newProveedoresSet = new HashSet<>(proveedoresValidos.keySet());
                            Set<String> oldProveedoresSet = oldProveedores != null ? new HashSet<>(oldProveedores.keySet()) : new HashSet<>();

                            // Proveedores eliminados
                            Set<String> proveedoresEliminados = new HashSet<>(oldProveedoresSet);
                            proveedoresEliminados.removeAll(newProveedoresSet);

                            // Proveedores añadidos
                            Set<String> proveedoresAnadidos = new HashSet<>(newProveedoresSet);
                            proveedoresAnadidos.removeAll(oldProveedoresSet);

                            System.out.println("Evento ID: " + id);
                            System.out.println("Proveedores eliminados: " + proveedoresEliminados);
                            System.out.println("Proveedores añadidos: " + proveedoresAnadidos);

                            // Actualizar eventosPendientes de los proveedores eliminados
                            for (String proveedorId : proveedoresEliminados) {
                                Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                                if (proveedorOpt.isPresent()) {
                                    Proveedor proveedor = proveedorOpt.get();
                                    List<String> pendientes = proveedor.getEventosPendientes();
                                    if (pendientes != null && pendientes.contains(id)) {
                                        pendientes.remove(id);
                                        proveedor.setEventosPendientes(pendientes);
                                        proveedorRepository.save(proveedor);
                                        System.out.println("Proveedor " + proveedorId + " actualizado - Evento " + id + " eliminado de eventosPendientes: " + pendientes);
                                    }
                                } else {
                                    System.out.println("Proveedor " + proveedorId + " no encontrado al eliminar evento " + id);
                                }
                            }

                            // Actualizar eventosPendientes de los proveedores añadidos
                            for (String proveedorId : proveedoresAnadidos) {
                                Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                                if (proveedorOpt.isPresent()) {
                                    Proveedor proveedor = proveedorOpt.get();
                                    List<String> pendientes = proveedor.getEventosPendientes();
                                    if (pendientes == null) {
                                        pendientes = new ArrayList<>();
                                    }
                                    if (!pendientes.contains(id)) {
                                        pendientes.add(id);
                                        proveedor.setEventosPendientes(pendientes);
                                        proveedorRepository.save(proveedor);
                                        System.out.println("Proveedor " + proveedorId + " actualizado - Evento " + id + " añadido a eventosPendientes: " + pendientes);
                                    } else {
                                        System.out.println("Evento " + id + " ya está en eventosPendientes de proveedor " + proveedorId);
                                    }
                                }
                            }

                            evento.setProveedores(proveedoresValidos);
                        }

                        // Manejar ubicaciones y precios
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> newUbicacionesPrecios = (List<Map<String, Object>>) eventData.get("ubicacionesPrecios");
                        if (newUbicacionesPrecios != null) {
                            evento.setUbicacionesPrecios(newUbicacionesPrecios);
                        }

                        Evento updatedEvento = eventoRepository.save(evento);
                        Map<String, Object> response = new HashMap<>();
                        response.put("evento", updatedEvento);
                        response.put("message", "Evento actualizado exitosamente.");
                        if (!proveedoresNoEncontrados.isEmpty()) {
                            response.put("warning", "Algunos proveedores no fueron encontrados: " + proveedoresNoEncontrados);
                        }
                        if (!invitadosNoEncontrados.isEmpty()) {
                            response.put("warningInvitados", "Algunos invitados no fueron encontrados: " + invitadosNoEncontrados);
                        }
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error al parsear los datos del evento: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Evento no encontrado.")));
    }
    
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

                    // Limpiar referencias en invitados
                    List<String> invitados = evento.getInvitados();
                    if (invitados != null) {
                        for (String invitadoId : invitados) {
                            Invitado invitado = invitadoRepository.findById(invitadoId).orElse(null);
                            if (invitado != null) {
                                List<String> invitaciones = invitado.getInvitaciones();
                                if (invitaciones != null && invitaciones.contains(id)) {
                                    invitaciones.remove(id);
                                    invitado.setInvitaciones(invitaciones);
                                }
                                List<String> asistidos = invitado.getEventosAsistidos();
                                if (asistidos != null && asistidos.contains(id)) {
                                    asistidos.remove(id);
                                    invitado.setEventosAsistidos(asistidos);
                                }
                                invitadoRepository.save(invitado);
                                System.out.println("Invitado " + invitadoId + " actualizado - Evento " + id + " eliminado de invitaciones y eventos asistidos.");
                            }
                        }
                    }

                    // Limpiar referencias en proveedores
                    Map<String, String> proveedores = evento.getProveedores();
                    if (proveedores != null) {
                        for (String proveedorId : proveedores.keySet()) {
                            Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
                            if (proveedorOpt.isPresent()) {
                                Proveedor proveedor = proveedorOpt.get();
                                List<String> pendientes = proveedor.getEventosPendientes();
                                if (pendientes != null && pendientes.contains(id)) {
                                    pendientes.remove(id);
                                    proveedor.setEventosPendientes(pendientes);
                                }
                                List<String> asistidos = proveedor.getEventosAsistidos();
                                if (asistidos != null && asistidos.contains(id)) {
                                    asistidos.remove(id);
                                    proveedor.setEventosAsistidos(asistidos);
                                }
                                proveedorRepository.save(proveedor);
                                System.out.println("Proveedor " + proveedorId + " actualizado - Evento " + id + " eliminado de eventosPendientes y eventosAsistidos.");
                            }
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

        Map<String, Object> response = new HashMap<>();
        response.put("eventos", eventosFiltrados);
        response.put("currentPage", eventosPage.getNumber());
        response.put("totalItems", eventosPage.getTotalElements());
        response.put("totalPages", eventosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

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

    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> deleteOrganizador(@PathVariable String correo) {
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador != null) {
            organizadorRepository.delete(organizador);
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