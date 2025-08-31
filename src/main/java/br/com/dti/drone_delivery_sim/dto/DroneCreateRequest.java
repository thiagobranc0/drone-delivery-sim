package br.com.dti.drone_delivery_sim.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DroneCreateRequest(
        @NotBlank String id,
        @Positive double capacidadeKg,
        @Positive double alcanceKm,
        @Positive double velocidadeKmh,
        double consumoPercentPorKm
) {}

