package br.com.dti.drone_delivery_sim.web;

import br.com.dti.drone_delivery_sim.controller.DroneTravelController;
import br.com.dti.drone_delivery_sim.model.NoFlyZone;
import br.com.dti.drone_delivery_sim.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DroneTravelController.class)
class DroneTravelControllerWebTest {

    @Autowired MockMvc mvc;

    @MockBean OrderService orderService;
    @MockBean DroneService droneService;
    @MockBean DeliveryOptimizer optimizer;
    @MockBean RealTimeSimulator simulator;
    @MockBean RouteCalculator routes;

    @Test
    void planRetorna200ComEstrutura() throws Exception {
        Mockito.when(orderService.listar()).thenReturn(List.of());
        Mockito.when(droneService.listar()).thenReturn(List.of());
        Mockito.when(optimizer.planejar(Mockito.anyList(), Mockito.anyList(), Mockito.any()))
                .thenReturn(List.of()); // plano vazio

        mvc.perform(post("/plan").param("batteryPolicy", "STRICT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViagens", isA(Number.class)))
                .andExpect(jsonPath("$.viagens", isA(List.class)));
    }

    @Test
    void obstaclesCrudBasico() throws Exception {
        Mockito.when(routes.listarZonas()).thenReturn(List.of(new NoFlyZone(1,1,2,2)));

        mvc.perform(post("/obstacles")
                        .param("x1","1").param("y1","1")
                        .param("x2","2").param("y2","2"))
                .andExpect(status().isOk());

        mvc.perform(get("/obstacles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minX", is(1.0)));

        mvc.perform(delete("/obstacles"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardRetorna200() throws Exception {
        Mockito.when(simulator.gerarRelatorio())
                .thenReturn(new RealTimeSimulator.Relatorio(0,0.0,"-","(sem dados)"));

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEntregas", is(0)));
    }
}
