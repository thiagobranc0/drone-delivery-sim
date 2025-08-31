package br.com.dti.drone_delivery_sim.dto;

import br.com.dti.drone_delivery_sim.enums.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderDTO(
        @NotNull Double x,
        @NotNull Double y,
        @Positive(message = "Peso em Kg deve ser maior que 0") double pesoKg,
        @NotNull Priority prioridade
) {}

