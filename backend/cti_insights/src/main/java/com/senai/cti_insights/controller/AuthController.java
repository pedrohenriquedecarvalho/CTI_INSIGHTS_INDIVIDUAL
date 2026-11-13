package com.senai.cti_insights.controller;

import com.senai.cti_insights.dto.*;
import com.senai.cti_insights.service.AuthService;
import com.senai.cti_insights.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    private final UsuarioService usuarios;

    public AuthController(AuthService service, UsuarioService usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/register") // rota do CONTRATO (antes era /auth/cadastro, mas o front chamava /auth/register)
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse cadastrar(@Valid @RequestBody CadastroRequest dados) {
        return service.cadastrar(dados); // Controller delega a regra ao Service.
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest dados) {
        return service.entrar(dados);
    }

    @GetMapping("/auth/me")
    public UsuarioResponse me(Authentication authentication) {
        return UsuarioResponse.de(usuarios.atual(authentication));
    }

    /** Para testar se o servidor está no ar. */
    @GetMapping("/health")
    public Map<String, String> saude() {
        return Map.of("status", "ok");
    }

    @GetMapping("/publico")
    public Map<String, String> publico() {
        return Map.of("mensagem", "Rota pública");
    }

    @GetMapping("/privado")
    public Map<String, String> privado() {
        return Map.of("mensagem", "JWT válido!");
    }
}
