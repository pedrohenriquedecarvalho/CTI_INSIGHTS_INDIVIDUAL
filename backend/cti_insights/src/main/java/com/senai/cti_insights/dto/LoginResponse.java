package com.senai.cti_insights.dto;

// A resposta devolve o token, quanto tempo ele vale e os dados públicos do usuário.
public record LoginResponse(String token, String tipo, long expiraEmSegundos, UsuarioResponse usuario) {}
