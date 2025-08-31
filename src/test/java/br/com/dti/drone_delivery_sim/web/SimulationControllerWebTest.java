package br.com.dti.drone_delivery_sim.web;

import br.com.dti.drone_delivery_sim.controller.SimulationController;
import br.com.dti.drone_delivery_sim.service.RealTimeSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SimulationController.class)
class SimulationControllerWebTest {

    @Autowired MockMvc mvc;
    @MockBean RealTimeSimulator sim;

    @Test
    void queueLastPlan204() throws Exception {
        mvc.perform(post("/realtime/queue-last-plan"))
                .andExpect(status().isNoContent());
        Mockito.verify(sim).carregarPlanoComoMissoesDoUltimoPlano();
    }

    @Test
    void startManual200() throws Exception {
        mvc.perform(post("/realtime/start")
                        .param("mode","MANUAL")
                        .param("tickMillis","1000"))
                .andExpect(status().isOk());
        Mockito.verify(sim).iniciar("MANUAL", 1000L);
    }

    @Test
    void stop200() throws Exception {
        mvc.perform(post("/realtime/stop"))
                .andExpect(status().isNoContent()); // ← em vez de isOk()
        Mockito.verify(sim).parar();
    }

    @Test
    void tickAvancaTempo() throws Exception {
        mvc.perform(post("/realtime/tick").param("secs","60"))
                .andExpect(status().isOk());
        Mockito.verify(sim).tick(60L);
    }

    @Test
    void status200() throws Exception {
        mvc.perform(get("/realtime/status"))
                .andExpect(status().isOk());
    }

    @Test
    void telemetry200() throws Exception {
        mvc.perform(get("/realtime/telemetry"))
                .andExpect(status().isOk());
        mvc.perform(get("/realtime/telemetry/D1"))
                .andExpect(status().isOk());
    }
}
