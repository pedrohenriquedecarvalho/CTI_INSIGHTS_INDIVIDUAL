package com.senai.cti_insights.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity // Relaciona esta classe a uma tabela do banco.
@Table(name = "usuario") // Mesma tabela criada pelo schema.sql
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Identificador gerado pelo banco.
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash; // Nunca armazenar a senha original.

    @Enumerated(EnumType.STRING) // Guarda USER/ADMIN como texto no PostgreSQL.
    @Column(nullable = false, length = 20)
    private Perfil perfil = Perfil.USER;

    @Enumerated(EnumType.STRING) // PENDENTE, APROVADO ou BLOQUEADO
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Usuario() {} // Construtor exigido pelo JPA.

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public StatusUsuario getStatus() { return status; }
    public void setStatus(StatusUsuario status) { this.status = status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
