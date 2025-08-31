package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.model.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final List<Order> fila = new ArrayList<>();

    public synchronized void adicionar(Order o){ fila.add(o); }
    public synchronized List<Order> listar(){ return new ArrayList<>(fila); }
    public synchronized void limpar(){ fila.clear(); }
    public synchronized boolean vazia(){ return fila.isEmpty(); }
}

