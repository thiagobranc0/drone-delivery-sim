package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.enums.DroneState;
import br.com.dti.drone_delivery_sim.model.Drone;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DroneService {
    private final List<Drone> frota = new ArrayList<>();

    public DroneService() {
        frota.add(new Drone("D1", 5.0, 20.0, 40.0, 1.0));
        frota.add(new Drone("D2", 8.0, 30.0, 35.0, 1.2));
        frota.add(new Drone("D3", 3.0, 15.0, 45.0, 0.8));
    }

    public List<Drone> listar(){ return frota; }
    public void setEstadoTodos(DroneState estado){ frota.forEach(d -> d.setEstado(estado)); }
}

