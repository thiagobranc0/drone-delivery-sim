package br.com.dti.drone_delivery_sim.controller;

import br.com.dti.drone_delivery_sim.dto.OrderDTO;
import br.com.dti.drone_delivery_sim.model.Order;
import br.com.dti.drone_delivery_sim.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService pedidos;

    public OrderController(OrderService pedidos) {
        this.pedidos = pedidos;
    }

    @PostMapping
    public ResponseEntity<Order> criar(@Valid @RequestBody OrderDTO req) {
        Order p = new Order(req.x(), req.y(), req.pesoKg(), req.prioridade());
        pedidos.adicionar(p);

        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @GetMapping
    public List<Order> listar() {
        return pedidos.listar();
    }

    @DeleteMapping
    public ResponseEntity<Void> limpar() {
        pedidos.limpar();
        return ResponseEntity.noContent().build();
    }
}
