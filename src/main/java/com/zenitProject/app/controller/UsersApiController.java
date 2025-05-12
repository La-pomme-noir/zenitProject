package com.zenitProject.app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zenitProject.app.service.UserAuthenticationService;

@RestController
@RequestMapping("/api/users")
public class UsersApiController {

    @Autowired
    private UserAuthenticationService userAuthenticationService;

    // POST: Validar credenciales sin iniciar sesión (opcional)
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> validateCredentials(
            @RequestBody Map<String, String> loginRequest) {
        String correo = loginRequest.get("correo");
        String password = loginRequest.get("password");
        String selectedRole = loginRequest.get("role");

        if (correo == null || password == null || selectedRole == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Todos los campos son obligatorios"));
        }

        Map<String, String> result = userAuthenticationService.authenticate(correo, password, selectedRole);
        return ResponseEntity.ok(result); // Solo valida, no inicia sesión
    }
}