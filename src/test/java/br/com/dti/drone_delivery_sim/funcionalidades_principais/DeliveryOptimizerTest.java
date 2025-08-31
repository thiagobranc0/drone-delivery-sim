package br.com.dti.drone_delivery_sim.funcionalidades_principais;

import br.com.dti.drone_delivery_sim.enums.Priority;
import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.Drone;
import br.com.dti.drone_delivery_sim.model.Order;
import br.com.dti.drone_delivery_sim.service.BatterySimulator;
import br.com.dti.drone_delivery_sim.service.DeliveryOptimizer;
import br.com.dti.drone_delivery_sim.service.RouteCalculator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryOptimizerTest {

    @Test
    void rejeitaPedidoAcimaDaCapacidadeDaFrota() {
        RouteCalculator rc = new RouteCalculator();
        BatterySimulator bat = new BatterySimulator();
        DeliveryOptimizer opt = new DeliveryOptimizer(rc, bat);

        // Frota com capacidade máxima = 10kg
        List<Drone> frota = List.of(
                new Drone("D1", 8, 30, 40, 2.0),
                new Drone("D2", 10, 30, 40, 2.0)
        );

        Order pesado = new Order(2, 2, 12.0, Priority.HIGH); // > 10 kg
        List<Delivery> plano = opt.planejar(List.of(pesado), frota, BatterySimulator.Policy.STRICT);

        boolean contemPesado = plano.stream()
                .flatMap(d -> d.getPedidosIds().stream())
                .anyMatch(id -> id == pesado.getId());

        assertFalse(contemPesado, "Pedido acima da capacidade deve ser rejeitado");
    }

    @Test
    void respeitaPrioridadeHighAntesDeLow() {
        RouteCalculator rc = new RouteCalculator();
        BatterySimulator bat = new BatterySimulator();
        DeliveryOptimizer opt = new DeliveryOptimizer(rc, bat);

        List<Drone> frota = List.of(new Drone("D1", 10, 50, 50, 1.5));

        Order low = new Order(1, 0, 1, Priority.LOW);
        Order high1 = new Order(2, 0, 1, Priority.HIGH);
        Order high2 = new Order(3, 0, 1, Priority.HIGH);

        List<Order> pedidos = new ArrayList<>(List.of(low, high1, high2));
        List<Delivery> plano = opt.planejar(pedidos, frota, BatterySimulator.Policy.STRICT);

        List<Long> idsNaOrdem = plano.stream()
                .flatMap(d -> d.getPedidosIds().stream())
                .collect(Collectors.toList());

        int idxLow = idsNaOrdem.indexOf(low.getId());
        int idxH1 = idsNaOrdem.indexOf(high1.getId());
        int idxH2 = idsNaOrdem.indexOf(high2.getId());

        assertTrue(idxH1 < idxLow && idxH2 < idxLow,
                "Pedidos HIGH devem ser atendidos antes do LOW");
    }
}

