package br.com.dti.drone_delivery_sim.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> handleBadRequest(IllegalArgumentException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro","VALICAO_NEGOCIO","mensagem", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,Object>> handleConflict(IllegalStateException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("erro","CONFLITO","mensagem", ex.getMessage()));
    }
}

