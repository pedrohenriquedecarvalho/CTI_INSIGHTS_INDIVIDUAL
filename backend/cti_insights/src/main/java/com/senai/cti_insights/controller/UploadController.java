package com.senai.cti_insights.controller;

import com.senai.cti_insights.dto.UploadDtos.EnvioValidadasRequest;
import com.senai.cti_insights.service.UploadService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final UploadService service;

    public UploadController(UploadService service) {
        this.service = service;
    }

    /** Recebe SOMENTE as linhas que o front já validou. O Spring valida de novo e encaminha ao Python. */
    @PostMapping("/validadas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> enviarValidadas(@RequestBody EnvioValidadasRequest pedido) {
        return service.receberValidadas(pedido);
    }
}
