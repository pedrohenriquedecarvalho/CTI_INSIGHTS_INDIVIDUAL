package com.senai.cti_insights.dto;

import java.util.Map;

// Uma linha da planilha como chegou do front: número da linha no Excel e o texto de cada coluna.
public record ClienteDTO(int numero, Map<String, String> campos) {}
