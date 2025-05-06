package com.zenitProject.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zenitProject.app.entidades.Admin;
import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.entidades.Organizador;
import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.AdminRepository;
import com.zenitProject.app.repository.ClienteRepository;
import com.zenitProject.app.repository.OrganizadorRepository;
import com.zenitProject.app.repository.ProveedorRepository;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.SupervisorRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserAuthenticationService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OrganizadorRepository organizadorRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private InvitadoRepository invitadoRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SupervisorRepository supervisorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, String> authenticate(String correo, String password, String selectedRole) {
        Map<String, String> response = new HashMap<>();

        // Buscar en la colección de administradores
        Admin admin = adminRepository.findByCorreo(correo);
        if (admin != null) {
            if (passwordEncoder.matches(password, admin.getPassword())) {
                if (admin.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", admin.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }

        // Buscar en la colección de organizadores
        Organizador organizador = organizadorRepository.findByCorreo(correo);
        if (organizador != null) {
            if (passwordEncoder.matches(password, organizador.getPassword())) {
                if (organizador.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", organizador.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }

        // Buscar en la colección de proveedores
        Proveedor proveedor = proveedorRepository.findByCorreo(correo);
        if (proveedor != null) {
            if (passwordEncoder.matches(password, proveedor.getPassword())) {
                if (proveedor.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", proveedor.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }

        // Buscar en la colección de invitados
        Invitado invitado = invitadoRepository.findByCorreo(correo);
        if (invitado != null) {
            if (passwordEncoder.matches(password, invitado.getPassword())) {
                if (invitado.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", invitado.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }
        
        // Buscar en la colección de clientes
        Cliente cliente = clienteRepository.findByCorreo(correo);
        if (cliente != null) {
            if (passwordEncoder.matches(password, cliente.getPassword())) {
                if (cliente.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", cliente.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }
        
        // Buscar en la colección de supervisores
        Supervisor supervisor = supervisorRepository.findByCorreo(correo);
        if (supervisor != null) {
            if (passwordEncoder.matches(password, supervisor.getPassword())) {
                if (supervisor.getRole().equals(selectedRole)) {
                    response.put("message", "Login exitoso");
                    response.put("correo", correo);
                    response.put("role", supervisor.getRole());
                    return response;
                } else {
                    response.put("message", "Rol seleccionado no coincide");
                    return response;
                }
            }
        }

        // Si no se encuentra el usuario o las credenciales no coinciden
        response.put("message", "Credenciales inválidas");
        return response;
    }
}