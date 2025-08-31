package br.com.dti.drone_delivery_sim.model;

import java.util.ArrayList;
import java.util.List;

public final class Delivery {
    private final String droneId;
    private final List<Long> pedidosIds = new ArrayList<>();
    private final List<double[]> rota = new ArrayList<>(); // [x,y] começa/termina em [0,0]
    private double distanciaKm;
    private double etaMin;
    private double pesoTotalKg;
    private boolean viavel = true;
    private int paradasRecarga = 0;

    public Delivery(String droneId) { this.droneId = droneId; }

    public String getDroneId() { return droneId; }
    public List<Long> getPedidosIds() { return pedidosIds; }
    public List<double[]> getRota() { return rota; }
    public double getDistanciaKm() { return distanciaKm; }
    public double getEtaMin() { return etaMin; }
    public double getPesoTotalKg() { return pesoTotalKg; }
    public boolean isViavel() { return viavel; }
    public int getParadasRecarga() { return paradasRecarga; }

    public void adicionarPedido(Order p){ pedidosIds.add(p.getId()); pesoTotalKg += p.getPesoKg(); }
    public void setDistanciaKm(double v){ distanciaKm = v; }
    public void setEtaMin(double v){ etaMin = v; }
    public void setViavel(boolean v){ viavel = v; }
    public void incParadasRecarga(){ paradasRecarga++; }
}

