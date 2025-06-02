package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.entidades.Evento;
import com.zenitProject.app.repository.ProveedorRepository;
import com.zenitProject.app.repository.EventoRepository;
import com.cloudinary.Cloudinary;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/proveedores")
public class ProveedorRestController {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Cloudinary cloudinary;

    @GetMapping
    public List<Proveedor> getAllProveedores() {
        return proveedorRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Proveedor> getProveedorById(@PathVariable String id) {
        return proveedorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Proveedor>> getProveedoresByCategoria(@PathVariable String categoria) {
        List<Proveedor> proveedores = proveedorRepository.findByCategoria(categoria);
        System.out.println("Proveedores encontrados para la categoría '" + categoria + "': " + proveedores);
        return ResponseEntity.ok(proveedores);
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<Proveedor> getProveedorByCorreo(@PathVariable String correo) {
        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor != null) {
            List<String> validPendientes = proveedor.getEventosPendientes().stream()
                .filter(eventoId -> eventoId != null && eventoRepository.existsById(eventoId))
                .collect(Collectors.toList());
            if (validPendientes.size() != proveedor.getEventosPendientes().size()) {
                proveedor.setEventosPendientes(validPendientes);
                proveedorRepository.save(proveedor);
            }
            List<String> validAsistidos = proveedor.getEventosAsistidos().stream()
                .filter(eventoId -> eventoId != null && eventoRepository.existsById(eventoId))
                .collect(Collectors.toList());
            if (validAsistidos.size() != proveedor.getEventosAsistidos().size()) {
                proveedor.setEventosAsistidos(validAsistidos);
                proveedorRepository.save(proveedor);
            }
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
        if (proveedor.getCategoria() == null) proveedor.setCategoria(existing.getCategoria());
        if (proveedor.getServicioCategoria() == null) proveedor.setServicioCategoria(existing.getServicioCategoria());
        if (proveedor.getServicioDescripcion() == null) proveedor.setServicioDescripcion(existing.getServicioDescripcion());
        if (proveedor.getImagenUrl() == null) proveedor.setImagenUrl(existing.getImagenUrl());
        if (proveedor.getEventosPendientes() == null) proveedor.setEventosPendientes(existing.getEventosPendientes());
        if (proveedor.getEventosAsistidos() == null) proveedor.setEventosAsistidos(existing.getEventosAsistidos());
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
        String categoria = registerRequest.get("categoria");

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
        proveedor.setCategoria(categoria);
        proveedorRepository.save(proveedor);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso", "correo", correo));
    }

    @PutMapping(value = "/profile/image", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateProveedorImage(
            @RequestPart("imagen") MultipartFile imagen,
            Authentication authentication) {
        System.out.println("Iniciando updateProveedorImage...");

        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Error: No autenticado");
            return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
        }

        String correo = authentication.getName();
        System.out.println("Correo del usuario autenticado: " + correo);

        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            System.out.println("Error: Proveedor no encontrado para el correo: " + correo);
            return ResponseEntity.status(404).body(Map.of("message", "Proveedor no encontrado"));
        }

        if (imagen == null || imagen.isEmpty()) {
            System.out.println("Error: No se proporcionó una imagen");
            return ResponseEntity.badRequest().body(Map.of("message", "No se proporcionó una imagen"));
        }

        try {
            System.out.println("Subiendo imagen a Cloudinary...");
            Map<String, Object> uploadResult = cloudinary.uploader().upload(imagen.getBytes(), Map.of(
                    "public_id", "proveedor_" + proveedor.getId() + "_" + System.currentTimeMillis(),
                    "resource_type", "image"
            ));
            String imageUrl = (String) uploadResult.get("secure_url");
            System.out.println("Imagen subida a Cloudinary con URL: " + imageUrl);

            proveedor.setImagenUrl(imageUrl);
            System.out.println("Guardando proveedor en la base de datos...");
            proveedorRepository.save(proveedor);
            System.out.println("Proveedor guardado con éxito");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Imagen actualizada exitosamente");
            response.put("imagenUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al subir la imagen a Cloudinary: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error al subir la imagen a Cloudinary: " + e.getMessage()));
        }
    }

    @PostMapping("/correo/{correo}/servicio")
    public ResponseEntity<Map<String, Object>> addOrUpdateServicio(
            @PathVariable String correo,
            @RequestBody Map<String, String> servicioRequest,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
        }

        String usuarioCorreo = authentication.getName();
        if (!usuarioCorreo.equals(correo)) {
            return ResponseEntity.status(403).body(Map.of("message", "No autorizado"));
        }

        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Proveedor no encontrado"));
        }

        String servicioCategoria = servicioRequest.get("categoria");
        String servicioDescripcion = servicioRequest.get("descripcion");

        if (servicioCategoria == null || servicioCategoria.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La categoría es obligatoria"));
        }
        if (servicioDescripcion == null || servicioDescripcion.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La descripción es obligatoria"));
        }

        proveedor.setServicioCategoria(servicioCategoria);
        proveedor.setServicioDescripcion(servicioDescripcion);
        proveedor.setCategoria(servicioCategoria);

        proveedorRepository.save(proveedor);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Servicio agregado/actualizado exitosamente");
        response.put("servicioCategoria", servicioCategoria);
        response.put("servicioDescripcion", servicioDescripcion);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/correo/{correo}/accept-event/{eventoId}")
    public ResponseEntity<Map<String, String>> acceptEvent(@PathVariable String correo, @PathVariable String eventoId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }

        String usuarioCorreo = authentication.getName();
        if (!usuarioCorreo.equals(correo)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No autorizado"));
        }

        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Proveedor no encontrado"));
        }

        if (!proveedor.getEventosPendientes().contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "El evento no está en los pendientes del proveedor"));
        }

        System.out.println("Aceptando evento " + eventoId + " para proveedor " + correo);
        proveedor.getEventosPendientes().remove(eventoId);
        proveedor.getEventosAsistidos().add(eventoId);
        proveedorRepository.save(proveedor);
        System.out.println("Evento " + eventoId + " movido a eventosAsistidos para proveedor " + correo);

        return ResponseEntity.ok(Map.of("message", "Evento aceptado con éxito"));
    }

    @PostMapping("/correo/{correo}/decline-event/{eventoId}")
    public ResponseEntity<Map<String, String>> declineEvent(@PathVariable String correo, @PathVariable String eventoId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }

        String usuarioCorreo = authentication.getName();
        if (!usuarioCorreo.equals(correo)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No autorizado"));
        }

        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Proveedor no encontrado"));
        }

        if (!proveedor.getEventosPendientes().contains(eventoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "El evento no está en los pendientes del proveedor"));
        }

        System.out.println("Declinando evento " + eventoId + " para proveedor " + correo);
        proveedor.getEventosPendientes().remove(eventoId);
        proveedorRepository.save(proveedor);

        Optional<Evento> eventoOpt = eventoRepository.findById(eventoId);
        if (eventoOpt.isPresent()) {
            Evento evento = eventoOpt.get();
            String proveedorId = proveedor.getId();
            evento.getProveedores().remove(proveedorId);
            eventoRepository.save(evento);
            System.out.println("Proveedor " + proveedorId + " eliminado del evento " + eventoId);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Evento no encontrado"));
        }

        return ResponseEntity.ok(Map.of("message", "Evento declinado con éxito"));
    }

    @GetMapping("/{correo}/eventos")
    public ResponseEntity<Map<String, Object>> getProveedorEvents(@PathVariable String correo, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }

        String usuarioCorreo = authentication.getName();
        if (!usuarioCorreo.equals(correo)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No autorizado"));
        }

        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Proveedor no encontrado"));
        }

        List<Evento> allEvents = eventoRepository.findAll();
        List<Map<String, Object>> eventosPendientes = allEvents.stream()
            .filter(evento -> proveedor.getEventosPendientes().contains(evento.getId()))
            .map(evento -> {
                Map<String, Object> eventoMap = new HashMap<>();
                eventoMap.put("id", evento.getId());
                eventoMap.put("nombreEvento", evento.getNombreEvento());
                eventoMap.put("fecha", evento.getFecha());
                eventoMap.put("hora", evento.getHora());
                return eventoMap;
            })
            .collect(Collectors.toList());

        List<Map<String, Object>> eventosAsistidos = allEvents.stream()
            .filter(evento -> proveedor.getEventosAsistidos().contains(evento.getId()))
            .map(evento -> {
                Map<String, Object> eventoMap = new HashMap<>();
                eventoMap.put("id", evento.getId());
                eventoMap.put("nombreEvento", evento.getNombreEvento());
                eventoMap.put("fecha", evento.getFecha());
                eventoMap.put("hora", evento.getHora());
                return eventoMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("eventosPendientes", eventosPendientes);
        response.put("eventosAsistidos", eventosAsistidos);
        return ResponseEntity.ok(response);
    }
}