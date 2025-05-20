package com.zenitProject.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zenitProject.app.entidades.Admin;
import com.zenitProject.app.entidades.Cliente;
import com.zenitProject.app.entidades.Invitado;
import com.zenitProject.app.entidades.Organizador;
import com.zenitProject.app.entidades.Proveedor;
import com.zenitProject.app.entidades.Supervisor;
import com.zenitProject.app.repository.AdminRepository;
import com.zenitProject.app.repository.ClienteRepository;
import com.zenitProject.app.repository.InvitadoRepository;
import com.zenitProject.app.repository.OrganizadorRepository;
import com.zenitProject.app.repository.ProveedorRepository;
import com.zenitProject.app.repository.SupervisorRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar en la colección de administradores
        Admin admin = adminRepository.findByCorreo(username);
        if (admin != null) {
            return User.withUsername(admin.getCorreo())
                    .password(admin.getPassword())
                    .roles("ADMIN") // Añadir prefijo ROLE_
                    .build();
        }
        
        // Buscar en la colección de organizadores
        Organizador organizador = organizadorRepository.findByCorreo(username);
        if (organizador != null) {
            return User.withUsername(organizador.getCorreo())
                    .password(organizador.getPassword())
                    .roles("ORGANIZADOR")
                    .build();
        }
        
        // Buscar en la colección de proveedores
        Proveedor proveedor = proveedorRepository.findByCorreo(username);
        if (proveedor != null) {
            return User.withUsername(proveedor.getCorreo())
                    .password(proveedor.getPassword())
                    .roles("PROVEEDOR")
                    .build();
        }
        
        // Buscar en la colección de invitados
        Invitado invitado = invitadoRepository.findByCorreo(username);
        if (invitado != null) {
            return User.withUsername(invitado.getCorreo())
                    .password(invitado.getPassword())
                    .roles("INVITADO")
                    .build();
        }

        // Buscar en la colección de clientes
        Cliente cliente = clienteRepository.findByCorreo(username);
        if (cliente != null) {
            return User.withUsername(cliente.getCorreo())
                    .password(cliente.getPassword())
                    .roles("CLIENTE")
                    .build();
        }
        
        // Buscar en la colección de supervisores
        Supervisor supervisor = supervisorRepository.findByCorreo(username);
        if (supervisor != null) {
            return User.withUsername(supervisor.getCorreo())
                    .password(supervisor.getPassword())
                    .roles("SUPERVISOR")
                    .build();
        }

        // Si no se encuentra en ninguna colección, lanzar excepción
        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}