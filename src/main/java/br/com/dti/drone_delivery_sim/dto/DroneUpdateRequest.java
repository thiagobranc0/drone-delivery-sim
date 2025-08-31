package br.com.dti.drone_delivery_sim.dto;

import br.com.dti.drone_delivery_sim.enums.DroneState;
import jakarta.validation.constraints.Positive;

public record DroneUpdateRequest(
        @Positive double capacidadeKg,
        @Positive double alcanceKm,
        @Positive double velocidadeKmh,
        double consumoPercentPorKm,
        DroneState estado
) {}

