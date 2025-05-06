package com.zenitProject.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

	@GetMapping({"/", "/index"})
	public ResponseEntity<String> redirectToHomePage() {
		return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/index.html").build();
	}
	
	@GetMapping("/api/user")
    @ResponseBody
    public Map<String, String> getUserRole(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            response.put("role", role);
        } else {
            response.put("role", "ANONYMOUS");
        }
        return response;
    }
}
