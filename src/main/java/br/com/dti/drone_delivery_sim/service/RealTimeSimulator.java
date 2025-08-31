package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RealTimeSimulator {

    private List<Delivery> ultimoPlano = new ArrayList<>();
    private List<Order> pedidosUsados = new ArrayList<>();

    public void registrarPlano(List<Delivery> entregas, List<Order> pedidos) {
        this.ultimoPlano = new ArrayList<>(entregas);
        this.pedidosUsados = new ArrayList<>(pedidos);
    }

    public record Relatorio(int quantidadeEntregas, double tempoMedioMin, String droneMaisEficiente, String mapaAscii){}

    public Relatorio gerarRelatorio() {
        if (ultimoPlano.isEmpty()) return new Relatorio(0,0.0,"-", "(sem dados)");

        int qtd = ultimoPlano.stream().mapToInt(d -> d.getPedidosIds().size()).sum();
        double media = ultimoPlano.stream().mapToDouble(Delivery::getEtaMin).average().orElse(0.0);
        String melhorDrone = ultimoPlano.stream()
                .collect(Collectors.groupingBy(Delivery::getDroneId, Collectors.summingDouble(Delivery::getDistanciaKm)))
                .entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");

        return new Relatorio(qtd, round2(media), melhorDrone, mapaAscii());
    }

    private String mapaAscii(){
        if (pedidosUsados.isEmpty()) return "(sem pedidos)";

        double maxX = pedidosUsados.stream().mapToDouble(Order::getX).map(Math::abs).max().orElse(5);
        double maxY = pedidosUsados.stream().mapToDouble(Order::getY).map(Math::abs).max().orElse(5);
        int w = (int)Math.max(20, Math.ceil(maxX)*2 + 3);
        int h = (int)Math.max(10, Math.ceil(maxY)*2 + 3);

        char[][] g = new char[h][w];
        for (char[] row : g) Arrays.fill(row, '.');

        int cx = w/2, cy = h/2;
        g[cy][cx] = 'B';

        for (Order o : pedidosUsados){
            int px = cx + (int)Math.round(o.getX());
            int py = cy - (int)Math.round(o.getY());
            if (px>=0 && px<w && py>=0 && py<h) g[py][px] = '*';
        }

        StringBuilder sb = new StringBuilder();
        for (char[] row : g) sb.append(row).append('\n');
        return sb.toString();
    }

    private static double round2(double v){ return Math.round(v*100.0)/100.0; }
}
