package br.com.dti.drone_delivery_sim.funcionalidades_principais;

import br.com.dti.drone_delivery_sim.dto.DroneCreateRequest;
import br.com.dti.drone_delivery_sim.dto.OrderDTO;
import br.com.dti.drone_delivery_sim.enums.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalcularTempoEntregaTest {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;

    @BeforeEach
    void reset() {
        http.delete("/orders");
        http.delete("/obstacles");
    }

    @Test
    void tempoTotalBateComDistanciaESpeedDoDrone() {
        ensureAtLeastOneDrone();

        //cria 1 pedido em (3,4) => 5 km até o ponto; ida e volta ~10 km
        var order = new OrderDTO(3.0, 4.0, 2.0, Priority.HIGH);
        var rOrder = http.postForEntity("/orders", order, String.class);
        assertEquals(HttpStatus.CREATED, rOrder.getStatusCode(),
                "POST /orders deveria retornar 201, veio " + rOrder.getStatusCode());

        // gera plano
        var plan = http.postForEntity("/plan", null, JsonNode.class);
        assertEquals(HttpStatus.OK, plan.getStatusCode(), "/plan deveria ser 200");
        JsonNode body = plan.getBody();
        assertNotNull(body);

        assertTrue(body.has("viagens") && body.get("viagens").isArray() && body.get("viagens").size() >= 1,
                "Plano sem viagens");

        double distanciaTotal = body.get("distanciaTotalKm").asDouble();
        double tempoTotal = body.get("tempoTotalMin").asDouble();

        JsonNode viagem = body.get("viagens").get(0);
        String droneIdUsado = viagem.get("droneId").asText();
        double distanciaDaViagem = viagem.get("distanciaKm").asDouble();
        double etaMin = viagem.get("etaMin").asDouble();

        // distância esperada ~10 km (com pequena tolerância)
        assertEquals(10.0, distanciaDaViagem, 0.05, "distância da viagem devia ~10 km");
        assertEquals(10.0, distanciaTotal, 0.05, "distância total devia ~10 km");

        // obtém velocidade do drone realmente usado no plano
        var status = http.getForEntity("/drones/status", JsonNode.class);
        assertEquals(HttpStatus.OK, status.getStatusCode());
        double velocidadeKmh = findVelocidade(status.getBody(), droneIdUsado);
        assertTrue(velocidadeKmh > 0, "Velocidade do drone não encontrada para id=" + droneIdUsado);

        // tempos esperados
        double tempoEsperadoViagem = (distanciaDaViagem / velocidadeKmh) * 60.0;
        double tempoEsperadoTotal = (distanciaTotal / velocidadeKmh) * 60.0;

        // tolerância de 0.1 minuto (6s) para arredondamentos internos
        assertEquals(tempoEsperadoViagem, etaMin, 0.1, "ETA da viagem não bate com (dist/vel)*60");
        assertEquals(tempoEsperadoTotal, tempoTotal, 0.1, "tempoTotalMin não bate com (distTotal/vel)*60");
    }


    private double findVelocidade(JsonNode dronesStatus, String id) {
        if (dronesStatus != null && dronesStatus.isArray()) {
            for (JsonNode d : dronesStatus) {
                if (d.has("id") && id.equals(d.get("id").asText())) {
                    if (d.has("velocidadeKmh")) return d.get("velocidadeKmh").asDouble();
                }
            }
        }
        return -1;
    }


    private void ensureAtLeastOneDrone() {
        ResponseEntity<JsonNode> list = http.getForEntity("/drones/status", JsonNode.class);
        if (list.getStatusCode().is2xxSuccessful()
                && list.getBody() != null && list.getBody().isArray()
                && list.getBody().size() > 0) {
            return;
        }


        var req = new DroneCreateRequest("TST1", 10.0, 100.0, 60.0, 1.5);
        ResponseEntity<String> created = http.postForEntity("/drones", req, String.class);

        if (!(created.getStatusCode().is2xxSuccessful()
                || created.getStatusCode().value() == 201
                || created.getStatusCode().value() == 200
                || created.getStatusCode().value() == 409)) {
            fail("POST /drones falhou: " + created.getStatusCode() + " body=" + created.getBody());
        }

        list = http.getForEntity("/drones/status", JsonNode.class);
        assertTrue(list.getStatusCode().is2xxSuccessful()
                        && list.getBody() != null && list.getBody().isArray()
                        && list.getBody().size() > 0,
                "Ainda sem drones após tentar criar um válido");
    }
}
