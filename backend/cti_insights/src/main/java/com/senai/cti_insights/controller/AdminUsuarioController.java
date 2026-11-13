package com.senai.cti_insights.controller;

import com.senai.cti_insights.dto.UsuarioResponse;
import com.senai.cti_insights.models.StatusUsuario;
import com.senai.cti_insights.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Tela do administrador: liberar ou bloquear quem se cadastrou. Só ADMIN (veja SecurityConfig). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {
    public record AlterarStatusRequest(@NotNull(message = "Informe o status") StatusUsuario status) {}

    private final UsuarioService service;

    public AdminUsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping // GET /api/admin/usuarios?status=PENDENTE (status é opcional)
    public List<UsuarioResponse> listar(@RequestParam(required = false) StatusUsuario status) {
        return service.listar(status);
    }

    @PatchMapping("/{id}/status") // body: { "status": "APROVADO" } ou "BLOQUEADO"
    public UsuarioResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest req,
                                         Authentication authentication) {
        return service.alterarStatus(id, req.status(), service.atual(authentication));
    }
}
