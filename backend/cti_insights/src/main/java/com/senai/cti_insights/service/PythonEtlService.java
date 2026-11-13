package com.senai.cti_insights.service;

import com.senai.cti_insights.config.ApiException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * A PONTE entre o Java e o Python. O Java só valida e ENCAMINHA: o Python cria o lote,
 * limpa, calcula e GRAVA no banco. Aqui não há model nem repository.
 */
@Component
public class PythonEtlService {
    private static final Logger log = LoggerFactory.getLogger(PythonEtlService.class);

    private final String url;
    private final String chave;
    private final RestClient http;

    public PythonEtlService(@Value("${app.etl.url:http://localhost:8000/etl}") String url,
                            @Value("${app.etl.key:}") String chave) {
        this.url = url.trim();
        this.chave = chave.trim();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(Duration.ofMinutes(5)); // o Python pode demorar em planilhas grandes
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    /** Envia as linhas válidas. O Python grava tudo no banco e responde 200. */
    public void enviar(String arquivoNome, int recebidas, int invalidas, int comAviso,
                       List<Map<String, Object>> linhas) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("arquivoNome", arquivoNome);
        pedido.put("recebidas", recebidas);
        pedido.put("invalidas", invalidas);
        pedido.put("comAviso", comAviso);
        pedido.put("linhas", linhas);
        log.info("Enviando {} linhas ao Python: POST {}", linhas.size(), url);
        try {
            http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (!chave.isEmpty()) h.set("X-ETL-Key", chave); })
                    .body(pedido)
                    .retrieve()
                    .toBodilessEntity(); // status diferente de 2xx lança RestClientResponseException
        } catch (RestClientResponseException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "O serviço Python respondeu " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível falar com o serviço Python (ele está ligado? uvicorn api:app --port 8000).");
        }
    }
}
