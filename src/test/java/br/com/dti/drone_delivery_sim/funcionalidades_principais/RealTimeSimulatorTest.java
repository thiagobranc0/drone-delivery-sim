package br.com.dti.drone_delivery_sim.funcionalidades_principais;

import br.com.dti.drone_delivery_sim.enums.Priority;
import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.Order;
import br.com.dti.drone_delivery_sim.service.DroneService;
import br.com.dti.drone_delivery_sim.service.RealTimeSimulator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RealTimeSimulatorTest {

    @Test
    void geraRelatorioComMediaEMapa() {
        RealTimeSimulator sim = new RealTimeSimulator(new DroneService());

        // duas viagens com tempos/distâncias diferentes
        Delivery d1 = new Delivery("D1");
        d1.setDistanciaKm(10.0);
        d1.setEtaMin(15.0);

        Delivery d2 = new Delivery("D2");
        d2.setDistanciaKm(8.0);
        d2.setEtaMin(10.0);

        // pedidos só para alimentar o mapa ASCII e os ids das entregas
        Order o1 = new Order(2, 1, 1.0, Priority.HIGH);
        Order o2 = new Order(-3, 2, 1.0, Priority.MEDIUM);

        // >>> Popula os pedidos entregues em cada Delivery <<<
        d1.getPedidosIds().add(o1.getId());
        d2.getPedidosIds().add(o2.getId());

        sim.registrarPlano(List.of(d1, d2), List.of(o1, o2));
        var rel = sim.gerarRelatorio();

        assertEquals(2, rel.quantidadeEntregas());       // 1 pedido em cada delivery
        assertEquals(12.5, rel.tempoMedioMin(), 1e-6);   // média de 15 e 10
        assertNotNull(rel.droneMaisEficiente());         // (opcional, poderia ser "D2")
        assertTrue(rel.mapaAscii().contains("B"));       // base
        assertTrue(rel.mapaAscii().contains("*"));       // pelo menos um pedido plotado
    }
}
