package br.com.dti.drone_delivery_sim.web;

import br.com.dti.drone_delivery_sim.controller.OrderController;
import br.com.dti.drone_delivery_sim.enums.Priority;
import br.com.dti.drone_delivery_sim.model.Order;
import br.com.dti.drone_delivery_sim.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerWebTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @SpyBean OrderService orderService;

    @Test
    @DisplayName("POST /orders cria pedido válido")
    void criaPedido() throws Exception {
        int before = orderService.listar().size();

        var body = """
        {"x":3,"y":4,"pesoKg":2.5,"prioridade":"HIGH"}
        """;

        // troque isCreated() por isOk() se seu controller devolver 200
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.x", is(3.0)))
                .andExpect(jsonPath("$.y", is(4.0)))
                .andExpect(jsonPath("$.pesoKg", is(2.5)))
                .andExpect(jsonPath("$.prioridade", is("HIGH")));

        assertEquals(before + 1, orderService.listar().size());
    }

    @Test
    @DisplayName("POST /orders com peso negativo retorna 400 (validação)")
    void criaPedidoInvalidoPesoNegativo() throws Exception {
        var body = """
        {"x":1,"y":1,"pesoKg":-1.0,"prioridade":"HIGH"}
        """;

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders lista pedidos existentes")
    void listaPedidos() throws Exception {
        orderService.limpar();
        orderService.adicionar(new Order(2, 2, 1.0, Priority.HIGH));
        orderService.adicionar(new Order(-3, 1, 3.0, Priority.MEDIUM));

        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].prioridade", anyOf(is("HIGH"), is("MEDIUM"), is("LOW"))));
    }

    @Test
    @DisplayName("DELETE /orders limpa todos (204)")
    void deletaTodosPedidos() throws Exception {
        orderService.adicionar(new Order(1, 1, 1.0, Priority.LOW));

        mvc.perform(delete("/orders"))
                .andExpect(status().isNoContent());

        assertEquals(0, orderService.listar().size());
    }
}
