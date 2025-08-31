package br.com.dti.drone_delivery_sim.model;

import br.com.dti.drone_delivery_sim.enums.Priority;

import java.util.concurrent.atomic.AtomicLong;

public final class Order {
    private static final AtomicLong SEQ = new AtomicLong(1);

    private final long id = SEQ.getAndIncrement();
    private final double x;
    private final double y;
    private final double pesoKg;
    private final Priority prioridade;
    private final long criadoEm = System.currentTimeMillis();

    public Order(double x, double y, double pesoKg, Priority prioridade) {
        if (pesoKg <= 0) throw new IllegalArgumentException("pesoKg > 0");
        this.x = x; this.y = y; this.pesoKg = pesoKg; this.prioridade = prioridade;
    }

    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getPesoKg() { return pesoKg; }
    public Priority getPrioridade() { return prioridade; }
    public long getCriadoEm() { return criadoEm; }
}

