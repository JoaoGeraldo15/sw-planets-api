package com.example.swplanetapi.web;

import com.example.swplanetapi.domain.PlanetService;
import com.example.swplanetapi.domain.model.Planet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.swplanetapi.commom.PlanetConstants.PLANET;
import static com.example.swplanetapi.commom.PlanetConstants.PLANETS;
import static com.example.swplanetapi.commom.PlanetConstants.TATOOINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class PlanetControllerTest {
    private static final String URL = "/api/planets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private PlanetService planetService;

    @Test
    public void createPlanet_WithValidData_ReturnsCreated() throws Exception {
        when(planetService.create(PLANET)).thenReturn(PLANET);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(PLANET)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(PLANET));

    }

    @Test
    public void createPlanet_WithInvalidData_ShouldReturnsBadRequest() throws Exception {
        Planet invalidPlanet = new Planet(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
        Planet emptyPlanet = new Planet();

        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(invalidPlanet)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emptyPlanet)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createPlanet_WithExistingName_ShouldReturnsConflict() throws Exception {
        when(planetService.create(any())).thenThrow(DataIntegrityViolationException.class);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(PLANET)))
                .andExpect(status().isConflict());
    }

    @Test
    public void getPlanet_ByExistingId_ReturnsPlanet() throws Exception {
        when(planetService.get(anyLong())).thenReturn(Optional.of(PLANET));

        String contentAsString = mockMvc.perform(get(URL + "/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();

        Planet planet = mapper.readValue(contentAsString, Planet.class);

        assertThat(planet).isNotNull();
        assertThat(planet.getName()).isEqualTo(PLANET.getName());
        assertThat(planet.getClimate()).isEqualTo(PLANET.getClimate());
        assertThat(planet.getTerrain()).isEqualTo(PLANET.getTerrain());
    }

    @Test
    public void getPlanet_ByNonExistingId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPlanet_ByExistingName_ReturnsPlanet() throws Exception {
        when(planetService.getByName(anyString())).thenReturn(Optional.of(PLANET));

        String contentAsString = mockMvc.perform(get(URL + "/name/" + PLANET.getName())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Planet sut = mapper.readValue(contentAsString, Planet.class);

        assertThat(sut).isNotNull();
        assertThat(sut.getName()).isEqualTo(PLANET.getName());
        assertThat(sut.getClimate()).isEqualTo(PLANET.getClimate());
        assertThat(sut.getTerrain()).isEqualTo(PLANET.getTerrain());
    }

    @Test
    public void getPlanet_ByNonExistingName_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/name/" + PLANET.getName())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void listPlanets_ReturnsFilteredPlanets() throws Exception {
        when(planetService.list(null, null)).thenReturn(PLANETS);
        when(planetService.list(TATOOINE.getTerrain(), TATOOINE.getClimate())).thenReturn(List.of(TATOOINE));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        String URL_LIST = String.format(URL + "?terrain=%s&climate=%s", TATOOINE.getTerrain(), TATOOINE.getClimate());
        mockMvc.perform(get(URL_LIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(TATOOINE));
    }

    @Test
    public void listPlanets_ReturnsNoPlanets() throws Exception {
        when(planetService.list(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void removePlanet_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void removePlanet_WithNonExistingId_ReturnsNotFound() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(planetService).remove(anyLong());

        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isNotFound());
    }


}
