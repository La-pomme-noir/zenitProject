package com.zenitProject.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Desactivar CSRF para pruebas
            .authorizeHttpRequests(auth -> auth
                // Permitir acceso público a recursos estáticos y páginas de login/registro
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/css/**", "/img/**", "/js/**", "/gif/**").permitAll()
                // Permitir acceso público al endpoint de registro
                .requestMatchers("/api/clientes/login","/rest/clientes/**", "/api/users/login").permitAll()
                // Restringir acceso a endpoints de admin solo a rol ADMIN
                .requestMatchers("/admin.html", "/adminProfile.html", "/adminCreate.html", "/adminList.html", "/api/admins/**", "/rest/admins/**", "/rest/organizadores/**", "/rest/invitados/**", "/rest/proveedores/**", "/rest/supervisores/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/perform_login")
                .successHandler(successHandler())
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/index.html")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            request.getSession().setAttribute("userRole", role); // Guardar rol en sesión
            if (role.equals("ADMIN")) {
                response.sendRedirect("/admin.html");
            }else if (role.equals("ORGANIZADOR")) {
            	response.sendRedirect("/organizador.html");
            }else if (role.equals("PROVEEDOR")) {
            	response.sendRedirect("/proveedor.html");
            }else if (role.equals("INVITADO")) {
            	response.sendRedirect("/invitado.html");
            }else if (role.equals("CLIENTE")) {
            	response.sendRedirect("/cliente.html");
            }else if (role.equals("SUPERVISOR")) {
            	response.sendRedirect("/supervisor.html");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}