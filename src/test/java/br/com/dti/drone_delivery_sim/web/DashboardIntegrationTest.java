package br.com.dti.drone_delivery_sim.web;

import br.com.dti.drone_delivery_sim.dto.OrderDTO;
import br.com.dti.drone_delivery_sim.dto.DroneCreateRequest;
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
class DashboardIntegrationTest {

    @Autowired TestRestTemplate http;

    @BeforeEach
    void reset() {
        http.delete("/orders");
    }

    @Test
    void dashboardRefletePlanoReal() {
        postJson("/drones", new DroneCreateRequest("T1", 10.0, 30.0, 60.0, 2.0));
        postJson("/drones", new DroneCreateRequest("T2", 10.0, 30.0, 60.0, 2.0));


        postJson("/orders", new OrderDTO(2.0, 2.0, 2.0, Priority.HIGH));
        postJson("/orders", new OrderDTO(-3.0, 1.0, 3.0, Priority.MEDIUM));

        var plan = http.postForEntity("/plan?batteryPolicy=STRICT", null, String.class);
        assertEquals(HttpStatus.OK, plan.getStatusCode());

        var resp = http.getForEntity("/dashboard", JsonNode.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        var body = resp.getBody();
        assertNotNull(body);

        assertEquals(2, body.get("quantidadeEntregas").asInt(), "Deve refletir os 2 pedidos planejados");
        assertTrue(body.get("tempoMedioMin").asDouble() > 0.0);
        assertFalse(body.get("droneMaisEficiente").asText().isBlank());
        assertTrue(body.get("mapaAscii").asText().contains("B")); // base plotada
    }

    private void postJson(String url, Object dto) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        http.exchange(url, HttpMethod.POST, new HttpEntity<>(dto, h), String.class);
    }
}
