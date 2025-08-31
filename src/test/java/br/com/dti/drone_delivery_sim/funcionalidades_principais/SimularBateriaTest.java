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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimularBateriaTest {

    @Autowired TestRestTemplate http;

    @BeforeEach
    void reset() {
        http.delete("/orders");
        http.postForEntity("/realtime/stop", null, Void.class);
    }

    @Test
    void bateriaCaiAposTicks() {
        // 1) cria um drone e um pedido curto
        postJson("/drones", new DroneCreateRequest("BT1", 10.0, 30.0, 60.0, 2.0));
        postJson("/orders", new OrderDTO(2.0, 1.0, 2.0, Priority.HIGH));

        // 2) planeja
        ResponseEntity<JsonNode> plan = http.postForEntity("/plan?batteryPolicy=STRICT", null, JsonNode.class);
        assertEquals(HttpStatus.OK, plan.getStatusCode());
        JsonNode body = plan.getBody();
        assertNotNull(body);

        // pega o drone realmente usado na 1ª viagem
        var viagens = body.withArray("viagens");
        assertTrue(viagens.size() > 0, "Plano precisa ter pelo menos 1 viagem");
        String droneId = viagens.get(0).get("droneId").asText();
        assertNotNull(droneId);
        assertFalse(droneId.isBlank(), "droneId não pode estar vazio");

        // 3) coloca o plano na fila do realtime
        var q = http.postForEntity("/realtime/queue-last-plan", null, Void.class);
        assertTrue(q.getStatusCode().is2xxSuccessful() || q.getStatusCode().value() == 204);

        ResponseEntity<Void> start =
                http.postForEntity("/realtime/start?mode=MANUAL&tickMillis=50", null, Void.class);
        assertTrue(start.getStatusCode().is2xxSuccessful(),
                "Esperado 2xx em /realtime/start, veio: " + start.getStatusCode());

        // 5) bateria antes
        double before = readBatteryPercentOf(droneId);

        // 6) avança alguns ticks
        for (int i = 0; i < 5; i++) {
            http.postForEntity("/realtime/tick", null, Void.class);
        }

        // 7) bateria depois
        double after = readBatteryPercentOf(droneId);

        assertTrue(before > after,
                "Bateria deve reduzir após ticks (drone=" + droneId + ", before=" + before + ", after=" + after + ")");
    }


    private double readBatteryPercentOf(String droneId) {
        ResponseEntity<JsonNode> resp = http.getForEntity("/realtime/telemetry/{id}", JsonNode.class, droneId);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode t = resp.getBody();
        assertNotNull(t);

        // tenta vários nomes comuns
        for (String key : new String[]{"batteryPercent", "battery", "bateriaPercent", "bateria"}) {
            if (t.has(key)) return t.get(key).asDouble();
        }
        // fallback: tenta campo 'percent' ou algo similar
        if (t.has("percent")) return t.get("percent").asDouble();

        fail("Não encontrei campo de bateria no telemetry: " + t);
        return 0.0;
    }

    private void postJson(String url, Object dto) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        http.exchange(url, HttpMethod.POST, new HttpEntity<>(dto, h), String.class);
    }
}
