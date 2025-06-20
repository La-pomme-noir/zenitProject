package com.zenitProject.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/nosotros.html", "/evento.html", "/login.html", "/register.html", "/css/**", "/img/**", "/js/**", "/gif/**", "/uploads/**").permitAll()
                .requestMatchers("/api/clientes/login", "/rest/clientes/**", "/api/users/login", "/rest/organizadores/public/eventos", "/api/clientes/auth-status").permitAll()
                .requestMatchers("/admin.html", "/adminProfile.html", "/adminCreate.html", "/adminList.html", "/api/admins/**", "/rest/admins/**").hasRole("ADMIN")
                .requestMatchers("/rest/organizadores/**", "/rest/invitados/**", "/rest/proveedores/**", "/rest/supervisores/**").authenticated()
                .requestMatchers("/invitado.html", "/acceptedEvents.html", "/checkInvitations.html").hasRole("INVITADO") // Corrección de typos y rutas
                .requestMatchers("/proveedorProfile.html", "/api/proveedores**", "/proveedorServicios.html", "/proveedorEventos.html").hasRole("PROVEEDOR")
                .requestMatchers("/api/clientes/profile", "/api/clientes/register-event", "/api/clientes/cancel-event/**", "/api/clientes/registered-events", "/clienteEventos.html", "/clienteMoreEventos.html", "/paymentForm.html", "/confirmation.html").hasRole("CLIENTE")
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
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            request.getSession().setAttribute("userRole", role);
            if (role.equals("ADMIN")) {
                response.sendRedirect("/admin.html");
            } else if (role.equals("ORGANIZADOR")) {
                response.sendRedirect("/organizador.html");
            } else if (role.equals("PROVEEDOR")) {
                response.sendRedirect("/proveedor.html");
            } else if (role.equals("INVITADO")) {
                response.sendRedirect("/invitado.html");
            } else if (role.equals("CLIENTE")) {
                response.sendRedirect("/cliente.html");
            } else if (role.equals("SUPERVISOR")) {
                response.sendRedirect("/supervisor.html");
            }
        };
    }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.sendRedirect("/index.html");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:8092")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowCredentials(true);
            }
        };
    }
}