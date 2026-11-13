package com.senai.cti_insights.controller;

import com.senai.cti_insights.config.ApiException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Padroniza todas as respostas de erro: {"erro": "...", "campos": {...}}. */
@RestControllerAdvice
public class ErroHandler {
    private static final Logger log = LoggerFactory.getLogger(ErroHandler.class);

    public record ErroResposta(String erro, Map<String, String> campos) {}

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(new ErroResposta(mensagem, campos));
    }

    @ExceptionHandler(ApiException.class) // erros que nós mesmos lançamos
    public ResponseEntity<ErroResposta> api(ApiException e) {
        return resposta(e.getStatus(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // campos inválidos (@Valid)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> campos.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Verifique os campos informados.", campos);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> parametroAusente(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Falta o parâmetro '" + e.getParameterName() + "'.", null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResposta> requisicaoInvalida(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodo(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.", null);
    }

    // Sem estes dois, o handler genérico abaixo transformaria 401/403 em erro 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> semPermissao(AccessDeniedException e) {
        return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para isso.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> naoAutenticado(AuthenticationException e) {
        return resposta(HttpStatus.UNAUTHORIZED, "Faça login para continuar.", null);
    }

    @ExceptionHandler(Exception.class) // qualquer outro erro
    public ResponseEntity<ErroResposta> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.", null);
    }
}
