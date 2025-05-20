package com.zenitProject.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.repository.InvitadoRepository;

import java.util.Map;

@RestController
@RequestMapping("/rest/auth")
public class InvitadoApiController {

    @Autowired
    private InvitadoRepository invitadoRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No estás autenticado"));
        }

        String correo = authentication.getName(); // Asumiendo que el nombre del usuario es el correo
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Invitado no encontrado"));
        }

        return ResponseEntity.ok(invitado);
    }
}