package br.com.dti.drone_delivery_sim.funcionalidades_principais;

import br.com.dti.drone_delivery_sim.dto.DroneCreateRequest;
import br.com.dti.drone_delivery_sim.dto.OrderDTO;
import br.com.dti.drone_delivery_sim.enums.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FilaPrioridadeTest {

    @Autowired TestRestTemplate http;

    @BeforeEach
    void reset() {
        http.delete("/orders");
        http.delete("/obstacles");
        http.postForEntity("/realtime/stop", null, Void.class);
        ensureAtLeastOneDrone(); // cria um drone válido se não houver frota
    }

    @Test
    void prioridadeHighVemAntesDeMediumQueVemAntesDeLow() throws InterruptedException {
        // Chegada: MEDIUM -> HIGH -> LOW (com pequenos sleeps para timestamps distintos)
        postOrder(new OrderDTO( 1.0, 1.0, 1.0, Priority.MEDIUM));
        Thread.sleep(5);
        postOrder(new OrderDTO(-1.0, 1.0, 1.0, Priority.HIGH));
        Thread.sleep(5);
        postOrder(new OrderDTO( 2.0,-1.0, 1.0, Priority.LOW));

        // Plano
        ResponseEntity<JsonNode> plan = http.postForEntity("/plan", null, JsonNode.class);
        assertEquals(HttpStatus.OK, plan.getStatusCode());
        JsonNode body = plan.getBody();
        assertNotNull(body);
        assertTrue(body.has("viagens") && body.get("viagens").isArray() && body.get("viagens").size() >= 1,
                "Plano sem viagens");

        // Mapa id -> prioridade atual do sistema
        Map<Integer, Integer> rank = priorityRankingByOrderId();

        // Achata pedidosIds na ordem das viagens
        int lastRank = Integer.MAX_VALUE; // começa no maior possível
        for (JsonNode viagem : body.get("viagens")) {
            for (JsonNode pid : viagem.withArray("pedidosIds")) {
                int id = pid.asInt();
                Integer r = rank.get(id);
                assertNotNull(r, "Pedido " + id + " não encontrado em GET /orders");
                // assert de ordem não-crescente (3 >= 2 >= 1)
                assertTrue(r <= lastRank,
                        "Prioridade subiu na fila (id " + id + "): " + r + " > " + lastRank);
                lastRank = r;
            }
        }
    }


    /** Cria um pedido e exige 201/2xx. Corpo pode ser json ou número; não precisamos do id aqui. */
    private void postOrder(OrderDTO dto) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = http.exchange("/orders", HttpMethod.POST, new HttpEntity<>(dto, h), String.class);
        assertTrue(res.getStatusCode().is2xxSuccessful(),
                "POST /orders deveria retornar 201/2xx, veio " + res.getStatusCode() + "; body=" + res.getBody());
    }

    /** Lê /orders e mapeia id -> rank (HIGH=3, MEDIUM=2, LOW=1). */
    private Map<Integer, Integer> priorityRankingByOrderId() {
        ResponseEntity<JsonNode> list = http.getForEntity("/orders", JsonNode.class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        JsonNode arr = list.getBody();
        assertNotNull(arr);
        assertTrue(arr.isArray());

        Map<Integer, Integer> map = new HashMap<>();
        for (JsonNode o : arr) {
            int id = o.get("id").asInt();
            String p = o.get("prioridade").asText();
            map.put(id, rankOf(p));
        }
        return map;
    }

    private int rankOf(String prio) {
        // normaliza
        String p = prio.toUpperCase();
        return switch (p) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /** Garante frota: tenta criar um drone válido; se já houver, só segue. */
    private void ensureAtLeastOneDrone() {
        ResponseEntity<JsonNode> list = http.getForEntity("/drones/status", JsonNode.class);
        if (list.getStatusCode().is2xxSuccessful()
                && list.getBody() != null && list.getBody().isArray()
                && list.getBody().size() > 0) {
            return;
        }
        // valores dentro das validações do seu controller (capacidade <= 25, positivos, id alfanumérico)
        DroneCreateRequest req = new DroneCreateRequest("QPR1", 10.0, 100.0, 60.0, 1.5);
        ResponseEntity<String> created = http.postForEntity("/drones", req, String.class);
        assertTrue(created.getStatusCode().is2xxSuccessful()
                        || created.getStatusCode().value() == 201
                        || created.getStatusCode().value() == 200
                        || created.getStatusCode().value() == 409,
                "POST /drones falhou: " + created.getStatusCode() + " body=" + created.getBody());
    }
}
