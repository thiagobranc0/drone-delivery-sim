package br.com.dti.drone_delivery_sim.controller;

import br.com.dti.drone_delivery_sim.service.RealTimeSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/realtime")
public class SimulationController {

    private final RealTimeSimulator sim;

    public SimulationController(RealTimeSimulator sim) {
        this.sim = sim;
    }

    /** Usa o último plano registrado (depois do /plan) e cria filas de missões por drone. */
    @PostMapping("/queue-last-plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void queueLastPlan() { sim.carregarPlanoComoMissoesDoUltimoPlano(); }

    /** Inicia o simulador em AUTO (scheduler) ou configura para MANUAL. */
    @PostMapping("/start")
    public Map<String,Object> start(@RequestParam(defaultValue = "MANUAL") String mode,
                                    @RequestParam(required = false) Long tickMillis) {
        sim.iniciar(mode, tickMillis);
        return Map.of("mode", mode.toUpperCase(), "tickMillis", tickMillis == null ? 1000 : tickMillis);
    }

    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop() { sim.parar(); }

    /** Avança manualmente a simulação 'secs' segundos. Útil para testes determinísticos. */
    @PostMapping("/tick")
    public Map<String,Object> tick(@RequestParam(defaultValue = "60") long secs) {
        sim.tick(secs);
        return Map.of("advancedSeconds", secs);
    }

    @GetMapping("/status")
    public RealTimeSimulator.Status status(){ return sim.status(); }

    @GetMapping("/telemetry")
    public Object allTelemetry(){ return sim.listarTelemetria(); }

    @GetMapping("/telemetry/{droneId}")
    public Object telemetry(@PathVariable String droneId){ return sim.telemetriaDoDrone(droneId); }
}

