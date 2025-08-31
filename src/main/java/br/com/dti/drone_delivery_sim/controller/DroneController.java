package br.com.dti.drone_delivery_sim.controller;

import br.com.dti.drone_delivery_sim.dto.DroneCreateRequest;
import br.com.dti.drone_delivery_sim.dto.DroneDTO;
import br.com.dti.drone_delivery_sim.dto.DroneUpdateRequest;
import br.com.dti.drone_delivery_sim.enums.DroneState;
import br.com.dti.drone_delivery_sim.model.Drone;
import br.com.dti.drone_delivery_sim.service.DroneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/drones")
public class DroneController {

    private final DroneService drones;

    public DroneController(DroneService drones) { this.drones = drones; }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DroneDTO create(@Valid @RequestBody DroneCreateRequest req) {
        var d = drones.criar(req.id(), req.capacidadeKg(), req.alcanceKm(),
                req.velocidadeKmh(), req.consumoPercentPorKm());
        return toDTO(d);
    }


    @GetMapping
    public List<DroneDTO> list() {
        return drones.listar().stream().map(DroneController::toDTO).toList();
    }

    @GetMapping("/{id}")
    public DroneDTO get(@PathVariable String id) {
        return drones.buscar(id)
                .map(DroneController::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "drone não encontrado"));
    }

    @PutMapping("/{id}")
    public DroneDTO update(@PathVariable String id, @Valid @RequestBody DroneUpdateRequest req) {
        try {
            var d = drones.atualizar(id, req.capacidadeKg(), req.alcanceKm(),
                    req.velocidadeKmh(), req.consumoPercentPorKm(), req.estado());
            return toDTO(d);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "drone não encontrado");
        }
    }

    @PatchMapping("/{id}/state")
    public DroneDTO patchState(@PathVariable String id, @RequestParam DroneState estado) {
        try {
            return toDTO(drones.atualizarEstado(id, estado));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "drone não encontrado");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!drones.remover(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "drone não encontrado");
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() { drones.limpar(); }


    @GetMapping("/status")
    public List<DroneDTO> status() { return list(); }

    private static DroneDTO toDTO(Drone d) {
        return new DroneDTO(d.getId(), d.getEstado().name(), d.getCapacidadeKg(),
                d.getAlcanceKm(), d.getVelocidadeKmh());
    }
}
