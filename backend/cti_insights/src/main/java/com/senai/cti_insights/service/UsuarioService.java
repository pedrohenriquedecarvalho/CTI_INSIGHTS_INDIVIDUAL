package com.senai.cti_insights.service;

import com.senai.cti_insights.config.ApiException;
import com.senai.cti_insights.dto.UsuarioResponse;
import com.senai.cti_insights.models.StatusUsuario;
import com.senai.cti_insights.models.Usuario;
import com.senai.cti_insights.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service // Regras do administrador: listar usuários e aprovar/bloquear.
public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    /** Usuário logado a partir do token JWT já validado pelo filtro. */
    public Usuario atual(Authentication authentication) {
        return repository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));
    }

    public List<UsuarioResponse> listar(StatusUsuario status) {
        List<Usuario> lista = status == null
                ? repository.findAllByOrderByCriadoEmDesc()
                : repository.findByStatusOrderByCriadoEmDesc(status);
        return lista.stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse alterarStatus(Long id, StatusUsuario novoStatus, Usuario quemAltera) {
        Usuario alvo = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (alvo.getId().equals(quemAltera.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Você não pode alterar o seu próprio acesso.");
        }
        alvo.setStatus(novoStatus);
        return UsuarioResponse.de(repository.save(alvo));
    }
}
