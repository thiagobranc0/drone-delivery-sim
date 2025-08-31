package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.Drone;
import br.com.dti.drone_delivery_sim.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Heurística de alocação:
 * - Ordena pedidos por prioridade (HIGH > MEDIUM > LOW), depois peso (desc), depois FIFO (id).
 * - Para cada drone (round-robin), tenta encher a viagem respeitando capacidade e bateria.
 * - REJEITA pedidos cujo peso exceda a MAIOR capacidade da frota.
 * - Fallback só cria viagem unitária se o pedido couber no drone atual.
 */
@Service
public class DeliveryOptimizer {

    private final RouteCalculator rotas;
    private final BatterySimulator bateria;

    public DeliveryOptimizer(RouteCalculator rotas, BatterySimulator bateria) {
        this.rotas = rotas;
        this.bateria = bateria;
    }

    public List<Delivery> planejar(List<Order> pedidos, List<Drone> frota, BatterySimulator.Policy politicaBateria){
        if (pedidos.isEmpty() || frota == null || frota.isEmpty()) return List.of();

        // 1) Remove logo os pedidos impossíveis para TODA a frota (peso > maior capacidade)
        double capMax = frota.stream().mapToDouble(Drone::getCapacidadeKg).max().orElse(0);
        List<Order> restantes = new ArrayList<>(pedidos);
        restantes.removeIf(o -> o.getPesoKg() > capMax);

        // 2) Ordenação: prioridade > peso(desc) > id (FIFO)
        restantes.sort(Comparator
                .comparing(Order::getPrioridade, Comparator.comparingInt(p -> switch (p){
                    case HIGH -> 3; case MEDIUM -> 2; case LOW -> 1; }))
                .reversed()
                .thenComparing((Order o) -> o.getPesoKg(), Comparator.reverseOrder())
                .thenComparingLong(Order::getId)
        );

        Map<String,List<Delivery>> porDrone = new LinkedHashMap<>();
        for (Drone d : frota) porDrone.put(d.getId(), new ArrayList<>());

        int idx = 0;
        while (!restantes.isEmpty()) {
            Drone d = frota.get(idx);
            List<Delivery> viagens = porDrone.get(d.getId());

            Delivery atual = new Delivery(d.getId());
            List<Order> viagemAtual = new ArrayList<>(); // mantém objetos dos pedidos já aceitos na viagem

            // Tenta adicionar pedidos enquanto couber (first-fit dentro da ordenação)
            List<Order> snapshot = new ArrayList<>(restantes);
            for (Order o : snapshot) {
                if (atual.getPesoTotalKg() + o.getPesoKg() > d.getCapacidadeKg()) continue;

                // candidato = pedidos já na viagem + o novo
                List<Order> candidato = new ArrayList<>(viagemAtual);
                candidato.add(o);

                var rr = rotas.vizinhoMaisProximo(candidato);
                var br = bateria.aplicar(politicaBateria, d, rr.rota());
                if (!br.viavel()) continue;

                // fixa na viagem
                viagemAtual.add(o);
                atual.adicionarPedido(o);
                atual.getRota().clear();
                atual.getRota().addAll(br.rota());
                atual.setDistanciaKm(round2(br.distanciaKm()));
                atual.setEtaMin(round2(d.estimarEtaMin(br.distanciaKm())));
                atual.setViavel(true);

                restantes.remove(o);
            }

            // 3) Fallback: se viagem ficou vazia, criar unitária apenas se couber no drone
            if (atual.getPedidosIds().isEmpty()) {
                Optional<Order> firstFit = restantes.stream()
                        .filter(o -> o.getPesoKg() <= d.getCapacidadeKg())
                        .findFirst();

                if (firstFit.isEmpty()) {
                    // nenhum pedido cabe neste drone → tenta o próximo drone
                    idx = (idx + 1) % frota.size();
                    continue;
                }

                Order o = firstFit.get();
                restantes.remove(o);

                var rr = rotas.vizinhoMaisProximo(List.of(o));
                var br = bateria.aplicar(politicaBateria, d, rr.rota());

                atual.adicionarPedido(o);
                atual.getRota().clear();
                atual.getRota().addAll(br.rota());
                atual.setDistanciaKm(round2(br.distanciaKm()));
                atual.setEtaMin(round2(d.estimarEtaMin(br.distanciaKm())));
                atual.setViavel(br.viavel());
            }

            viagens.add(atual);
            idx = (idx + 1) % frota.size();
        }

        return porDrone.values().stream().flatMap(List::stream).toList();
    }

    private static double round2(double v){ return Math.round(v * 100.0) / 100.0; }
}


