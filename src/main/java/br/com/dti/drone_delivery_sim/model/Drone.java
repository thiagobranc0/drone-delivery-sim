package br.com.dti.drone_delivery_sim.model;


import br.com.dti.drone_delivery_sim.enums.DroneState;

public final class Drone {
    private final String id;
    private final double capacidadeKg;
    private final double alcanceKm;
    private final double velocidadeKmh;
    private final double consumoPercentPorKm;
    private DroneState estado = DroneState.IDLE;

    public Drone(String id, double capacidadeKg, double alcanceKm, double velocidadeKmh, double consumoPercentPorKm) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id inválido");
        if (capacidadeKg <= 0) throw new IllegalArgumentException("capacidadeKg > 0");
        if (alcanceKm <= 0) throw new IllegalArgumentException("alcanceKm > 0");
        if (velocidadeKmh <= 0) throw new IllegalArgumentException("velocidadeKmh > 0");
        if (consumoPercentPorKm < 0) throw new IllegalArgumentException("consumoPercentPorKm >= 0");
        this.id = id; this.capacidadeKg = capacidadeKg; this.alcanceKm = alcanceKm;
        this.velocidadeKmh = velocidadeKmh; this.consumoPercentPorKm = consumoPercentPorKm;
    }

    public String getId() { return id; }
    public double getCapacidadeKg() { return capacidadeKg; }
    public double getAlcanceKm() { return alcanceKm; }
    public double getVelocidadeKmh() { return velocidadeKmh; }
    public double getConsumoPercentPorKm() { return consumoPercentPorKm; }

    public DroneState getEstado() { return estado; }
    public void setEstado(DroneState estado) { this.estado = estado; }

    public boolean podeCarregar(double pesoKg) { return pesoKg > 0 && pesoKg <= capacidadeKg; }
    public boolean podeAlcancar(double distanciaKm) { return distanciaKm >= 0 && distanciaKm <= alcanceKm; }
    public double estimarEtaMin(double distanciaKm) { return (distanciaKm / velocidadeKmh) * 60.0; }
    public double estimarConsumoPercent(double distanciaKm) { return distanciaKm * consumoPercentPorKm; }
}

