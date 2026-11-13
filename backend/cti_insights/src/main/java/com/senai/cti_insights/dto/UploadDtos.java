package com.senai.cti_insights.dto;

import java.util.List;

public final class UploadDtos {
    private UploadDtos() {}

    // Corpo do POST /api/uploads/validadas: só as linhas que o front aprovou.
    // "descartadas" = quantas linhas com erro o front deixou de fora (entra na contagem do lote).
    public record EnvioValidadasRequest(String arquivoNome, Integer descartadas, List<LinhaEnvio> linhas) {}

    // Uma linha já conferida pelo front (todos os campos como texto).
    public record LinhaEnvio(Integer numeroLinha, String codigoCliente, String nomeCliente, String consultor,
                             String segmento, String nivelCliente, String faturamentoAnual,
                             String servicosContratados, String dataContratacao, String cidade, String uf) {}
}
