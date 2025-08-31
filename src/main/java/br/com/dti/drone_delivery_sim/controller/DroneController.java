package br.com.dti.drone_delivery_sim.controller;

import br.com.dti.drone_delivery_sim.dto.DroneDTO;
import br.com.dti.drone_delivery_sim.service.DroneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drones")
public class DroneController {

    private final DroneService drones;

    public DroneController(DroneService drones) { this.drones = drones; }

    @GetMapping("/status")
    public List<DroneDTO> status(){
        return drones.listar().stream()
                .map(d -> new DroneDTO(d.getId(), d.getEstado().name(), d.getCapacidadeKg(), d.getAlcanceKm(), d.getVelocidadeKmh()))
                .toList();
    }
}

