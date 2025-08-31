package br.com.dti.drone_delivery_sim.web;

import br.com.dti.drone_delivery_sim.controller.DroneController;
import br.com.dti.drone_delivery_sim.dto.DroneCreateRequest;
import br.com.dti.drone_delivery_sim.model.Drone;
import br.com.dti.drone_delivery_sim.service.DroneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DroneController.class)
class DroneControllerWebTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean DroneService droneService;

    @Test
    @DisplayName("POST /drones cria um drone válido")
    void criaDrone() throws Exception {
        Mockito.when(droneService.criar("D9", 5, 20, 40, 1.2))
                .thenReturn(new Drone("D9", 5, 20, 40, 1.2));

        var body = new DroneCreateRequest("D9", 5, 20, 40, 1.2);

        mvc.perform(post("/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("D9")));
    }

    @Test
    @DisplayName("GET /drones lista drones")
    void listaDrones() throws Exception {
        Mockito.when(droneService.listar())
                .thenReturn(List.of(new Drone("D1", 5, 20, 40, 1.2)));

        mvc.perform(get("/drones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("D1")))
                .andExpect(jsonPath("$[0].estado", notNullValue()));
    }

    @Test
    @DisplayName("DELETE /drones apaga todos (204)")
    void deletaTodos() throws Exception {
        mvc.perform(delete("/drones"))
                .andExpect(status().isNoContent());
        Mockito.verify(droneService).limpar();
    }
}

