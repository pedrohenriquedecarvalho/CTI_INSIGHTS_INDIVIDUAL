package com.senai.cti_insights.controller;

import com.senai.cti_insights.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/dashboard            -> último lote processado + JSON das contas
 * GET /api/dashboard?loteId=3   -> um lote específico
 * GET /api/dashboard/lotes      -> os últimos lotes (para escolher na tela)
 * As respostas já vêm em JSON (montado pelo PostgreSQL), por isso o retorno é String.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final String JSON = "application/json;charset=UTF-8";

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping(produces = JSON)
    public String obter(@RequestParam(required = false) Long loteId) {
        return service.obter(loteId);
    }

    @GetMapping(value = "/lotes", produces = JSON)
    public String lotes() {
        return service.listarLotes();
    }
}
