package com.senai.cti_insights.service;

import com.senai.cti_insights.config.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * LÊ DO BANCO o que o Python gravou, para os gráficos. O Java só LÊ (quem grava é o Python).
 * Tabelas lidas: upload_lote (o lote e o status) e estatistica_lote (o JSON com as contas).
 *
 * O PostgreSQL já devolve a resposta pronta em JSON (json_build_object). Assim o Java repassa o texto
 * sem precisar converter o campo "resultado" em objeto.
 */
@Service
public class DashboardService {

    // Um lote como objeto JSON (nomes em camelCase, como o front espera).
    private static final String LOTE_JSON =
            "json_build_object('id', l.id, 'arquivoNome', l.arquivo_nome, 'enviadoEm', l.enviado_em, "
            + "'status', l.status, 'totalLinhas', l.total_linhas, 'linhasValidas', l.linhas_validas, "
            + "'linhasInvalidas', l.linhas_invalidas, 'linhasComAviso', l.linhas_com_aviso, 'mensagem', l.mensagem)";

    // { lote, geradoEm, estatisticas } — estatisticas é o JSON que o Python gravou (ou null se ainda não terminou).
    private static final String SELECT_DASHBOARD =
            "SELECT json_build_object('lote', " + LOTE_JSON + ", 'geradoEm', e.gerado_em, "
            + "'estatisticas', e.resultado::json)::text "
            + "FROM upload_lote l LEFT JOIN estatistica_lote e ON e.lote_id = l.id ";

    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Sem loteId: o último lote PROCESSADO (ou, se não houver, o último enviado). Devolve JSON em texto. */
    public String obter(Long loteId) {
        List<String> achados;
        if (loteId != null) {
            achados = jdbc.queryForList(SELECT_DASHBOARD + "WHERE l.id = ?", String.class, loteId);
        } else {
            achados = jdbc.queryForList(
                    SELECT_DASHBOARD + "WHERE l.status = 'PROCESSADO' ORDER BY l.id DESC LIMIT 1", String.class);
            if (achados.isEmpty()) {
                achados = jdbc.queryForList(SELECT_DASHBOARD + "ORDER BY l.id DESC LIMIT 1", String.class);
            }
        }
        if (achados.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, loteId == null
                    ? "Nenhuma planilha foi processada ainda. Envie uma planilha para ver os gráficos."
                    : "Lote " + loteId + " não encontrado.");
        }
        return achados.get(0);
    }

    /** Os 20 lotes mais recentes (JSON em texto), para a caixa de escolha da tela. */
    public String listarLotes() {
        return jdbc.queryForObject(
                "SELECT COALESCE(json_agg(" + LOTE_JSON + " ORDER BY l.id DESC), '[]'::json)::text "
                + "FROM (SELECT * FROM upload_lote ORDER BY id DESC LIMIT 20) l", String.class);
    }
}
