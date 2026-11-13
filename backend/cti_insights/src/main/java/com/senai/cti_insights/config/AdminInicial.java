package com.senai.cti_insights.config;

import com.senai.cti_insights.models.Perfil;
import com.senai.cti_insights.models.StatusUsuario;
import com.senai.cti_insights.models.Usuario;
import com.senai.cti_insights.repository.UsuarioRepository;
import com.senai.cti_insights.service.Datas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador (quem libera os demais usuários), se ainda não existir.
 * Sem ele ninguém conseguiria aprovar o primeiro cadastro. Roda uma vez, quando o servidor liga.
 */
@Component
public class AdminInicial implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository repo, PasswordEncoder encoder,
                        @Value("${app.admin.nome:Administrador}") String nome,
                        @Value("${app.admin.email:}") String email,
                        @Value("${app.admin.senha:}") String senha) {
        this.repo = repo;
        this.encoder = encoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) return;           // sem configuração: não cria nada
        String emailLimpo = email.trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(emailLimpo)) return;     // já existe: não cria de novo

        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setEmail(emailLimpo);
        admin.setSenhaHash(encoder.encode(senha));                // só o hash vai para o banco
        admin.setPerfil(Perfil.ADMIN);
        admin.setStatus(StatusUsuario.APROVADO);                  // o admin já nasce APROVADO
        admin.setCriadoEm(Datas.agoraUtc());
        repo.save(admin);
        log.warn("Administrador inicial criado: {} (troque a senha via variável ADMIN_PASSWORD em produção)", emailLimpo);
    }
}
