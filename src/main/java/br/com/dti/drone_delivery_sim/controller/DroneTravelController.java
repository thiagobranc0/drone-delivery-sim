package br.com.dti.drone_delivery_sim.controller;

import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.NoFlyZone;
import br.com.dti.drone_delivery_sim.service.*;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class DroneTravelController {

    private final OrderService pedidos;
    private final DroneService drones;
    private final DeliveryOptimizer otimizador;
    private final RealTimeSimulator tempoReal;
    private final RouteCalculator rotas;

    public DroneTravelController(OrderService pedidos, DroneService drones, DeliveryOptimizer otimizador,
                                 RealTimeSimulator tempoReal, RouteCalculator rotas) {
        this.pedidos = pedidos; this.drones = drones; this.otimizador = otimizador;
        this.tempoReal = tempoReal; this.rotas = rotas;
    }

    public record PlanResponse(int totalViagens, double distanciaTotalKm, double tempoTotalMin, List<Delivery> viagens){}

    @PostMapping("/plan")
    public ResponseEntity<PlanResponse> planejar(
            @RequestParam(name="batteryPolicy", defaultValue="STRICT")
            @Pattern(regexp = "STRICT|SMART") String batteryPolicy
    ){
        var politica = BatterySimulator.Policy.valueOf(batteryPolicy);
        List<Delivery> viagens = otimizador.planejar(pedidos.listar(), drones.listar(), politica);

        double dist = viagens.stream().mapToDouble(Delivery::getDistanciaKm).sum();
        double tempo = viagens.stream().mapToDouble(Delivery::getEtaMin).sum();

        tempoReal.registrarPlano(viagens, pedidos.listar());
        return ResponseEntity.ok(new PlanResponse(viagens.size(), round2(dist), round2(tempo), viagens));
    }

    @GetMapping("/dashboard")
    public RealTimeSimulator.Relatorio dashboard(){ return tempoReal.gerarRelatorio(); }

    @PostMapping("/obstacles")
    public void adicionarZona(@RequestParam double x1, @RequestParam double y1,
                              @RequestParam double x2, @RequestParam double y2){
        rotas.adicionarZona(new NoFlyZone(x1,y1,x2,y2));
    }

    @GetMapping("/obstacles")
    public List<NoFlyZone> listarZonas(){ return rotas.listarZonas(); }

    @DeleteMapping("/obstacles")
    public void limparZonas(){ rotas.limparZonas(); }

    private static double round2(double v){ return Math.round(v*100.0)/100.0; }
}

