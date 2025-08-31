package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.enums.DroneState;
import br.com.dti.drone_delivery_sim.model.Drone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DroneService {

    private final Map<String, Drone> frota = new LinkedHashMap<>();

    @Value("${drone.limits.capacidade-max-kg:25}")
    private double capacidadeMaxKg;

    private void validarLimites(double capacidadeKg, double alcanceKm, double velocidadeKmh, double consumo) {
        if (capacidadeKg > capacidadeMaxKg) {
            throw new IllegalArgumentException(
                    "capacidadeKg acima do permitido: " + capacidadeKg + " kg (máximo " + capacidadeMaxKg + " kg)"
            );
        }
        if (consumo < 0) throw new IllegalArgumentException("consumoPercentPorKm não pode ser negativo");
    }

    public synchronized Drone criar(String id, double capacidadeKg, double alcanceKm,
                                    double velocidadeKmh, double consumoPercentPorKm) {
        if (frota.containsKey(id)) throw new IllegalStateException("já existe drone com id: " + id);
        validarLimites(capacidadeKg, alcanceKm, velocidadeKmh, consumoPercentPorKm);
        var novo = new Drone(id, capacidadeKg, alcanceKm, velocidadeKmh, consumoPercentPorKm);
        frota.put(id, novo);
        return novo;
    }

    public synchronized List<Drone> listar() { return new ArrayList<>(frota.values()); }

    public synchronized Optional<Drone> buscar(String id) { return Optional.ofNullable(frota.get(id)); }

    public synchronized Drone atualizar(String id, double capacidadeKg, double alcanceKm,
                                        double velocidadeKmh, double consumoPercentPorKm,
                                        DroneState estadoNovoOuNull) {
        var atual = frota.get(id);
        if (atual == null) throw new NoSuchElementException("drone não encontrado: " + id);
        validarLimites(capacidadeKg, alcanceKm, velocidadeKmh, consumoPercentPorKm);
        var novo = new Drone(id, capacidadeKg, alcanceKm, velocidadeKmh, consumoPercentPorKm);
        novo.setEstado(estadoNovoOuNull != null ? estadoNovoOuNull : atual.getEstado());
        frota.put(id, novo);
        return novo;
    }

    public synchronized Drone atualizarEstado(String id, DroneState novoEstado) {
        var d = frota.get(id);
        if (d == null) throw new NoSuchElementException("drone não encontrado: " + id);
        d.setEstado(novoEstado);
        return d;
    }

    public synchronized boolean remover(String id) { return frota.remove(id) != null; }

    public synchronized void limpar() { frota.clear(); }

    public void setEstadoTodos(DroneState estado){ frota.values().forEach(d -> d.setEstado(estado)); }
}


