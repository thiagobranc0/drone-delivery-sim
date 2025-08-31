package br.com.dti.drone_delivery_sim.funcionalidades_principais;

import br.com.dti.drone_delivery_sim.enums.Priority;
import br.com.dti.drone_delivery_sim.model.NoFlyZone;
import br.com.dti.drone_delivery_sim.model.Order;
import br.com.dti.drone_delivery_sim.service.RouteCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObstaculoTest {

    @Test
    void rotaSimplesSemObstaculos() {
        RouteCalculator calc = new RouteCalculator();

        Order o = new Order(3, 4, 2.0, Priority.HIGH); // 5 até a base
        RouteCalculator.ResultadoRota rr = calc.vizinhoMaisProximo(List.of(o));

        assertNotNull(rr);
        assertEquals(3, rr.rota().size(), "Deve ir e voltar para a base");
        assertEquals(10.0, rr.distanciaKm(), 1e-6, "Distância total 5 + 5 = 10");
    }

    @Test
    void rotaConsideraZonaDeExclusao() {
        RouteCalculator calc = new RouteCalculator();
        calc.adicionarZona(new NoFlyZone(1, 1, 2, 3)); // retângulo cruzando a reta (0,0)->(3,4)

        Order o = new Order(3, 4, 2.0, Priority.HIGH);
        RouteCalculator.ResultadoRota rr = calc.vizinhoMaisProximo(List.of(o));

        assertNotNull(rr);
        assertTrue(rr.rota().size() > 3, "Rota deve ter waypoints extras");
        assertTrue(rr.distanciaKm() > 10.0, "Distância deve aumentar por causa do obstáculo");
    }

    @Test
    void naoAlteraRotaSeZonaNaoIntersecta() {
        RouteCalculator calc = new RouteCalculator();
        // obstáculo longe da reta (0,0)->(3,4)
        calc.adicionarZona(new NoFlyZone(10, 10, 12, 12));

        Order o = new Order(3, 4, 2.0, Priority.HIGH);
        var rr = calc.vizinhoMaisProximo(List.of(o));

        assertEquals(3, rr.rota().size(), "Vai direto: base->ponto->base");
        assertEquals(10.0, rr.distanciaKm(), 1e-6);
    }

    @Test
    void limparZonasFunciona() {
        RouteCalculator calc = new RouteCalculator();
        calc.adicionarZona(new NoFlyZone(1, 1, 2, 3)); // cruza a reta

        Order o = new Order(3, 4, 2.0, Priority.HIGH);
        var rrCom = calc.vizinhoMaisProximo(List.of(o));
        assertTrue(rrCom.distanciaKm() > 10.0);

        calc.limparZonas();
        var rrSem = calc.vizinhoMaisProximo(List.of(o));
        assertEquals(10.0, rrSem.distanciaKm(), 1e-6);
    }

}
