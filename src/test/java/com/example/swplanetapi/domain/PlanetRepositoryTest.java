package com.example.swplanetapi.domain;

import com.example.swplanetapi.domain.model.Planet;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static com.example.swplanetapi.commom.PlanetConstants.PLANET;
import static com.example.swplanetapi.commom.PlanetConstants.TATOOINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@DataJpaTest
public class PlanetRepositoryTest {

    @Autowired
    private PlanetRepository planetRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @AfterEach
    public void afterEach() {
        PLANET.setId(null);
    }

    @Test
    public void createPlanet_WithValidData_ShouldReturnPlanetSaved() {
        Planet planet = planetRepository.save(PLANET);

        Planet sut = testEntityManager.find(Planet.class, planet.getId());

        assertThat(sut).isNotNull();
        assertThat(sut.getName()).isEqualTo(PLANET.getName());
        assertThat(sut.getClimate()).isEqualTo(PLANET.getClimate());
        assertThat(sut.getTerrain()).isEqualTo(PLANET.getTerrain());

    }

    @Test
    public void createPlanet_WithInvalidData_ShouldThrowRunTimeException() {
        Planet emptyPlanet = new Planet();
        Planet invalidPlanet = new Planet(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);

        assertThatThrownBy(() -> planetRepository.save(emptyPlanet)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> planetRepository.save(invalidPlanet)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void createPlanet_WithExistingName_ShouldThrowsException() {
        Planet planet = testEntityManager.persistFlushFind(PLANET);
        testEntityManager.detach(planet);
        planet.setId(null);

        assertThatThrownBy(() -> planetRepository.save(planet)).isInstanceOf(RuntimeException.class);

    }

    @Test
    public void getPlanet_ByExistingId_ReturnsPlanet() {

        Planet planet = testEntityManager.persistFlushFind(PLANET);

        Optional<Planet> planetOptional = planetRepository.findById(planet.getId());

        assertThat(planetOptional).isNotEmpty();
        assertThat(planetOptional.get()).isEqualTo(planet);
    }

    @Test
    public void getPlanet_ByNonExistingId_ReturnsEmpty() {
        Optional<Planet> sut = planetRepository.findById(1L);

        assertThat(sut).isEmpty();
    }

    @Test
    public void getPlanet_ByExistingName_ReturnsPlanet() {
        Planet planet = testEntityManager.persistFlushFind(PLANET);

        Optional<Planet> sut = planetRepository.findByName(PLANET.getName());
        assertThat(sut).isNotEmpty();
        assertThat(sut.get()).isEqualTo(planet);
    }

    @Test
    public void getPlanet_ByNonExistingName_ReturnsNotFound() {
        Optional<Planet> sut = planetRepository.findByName(PLANET.getName());
        assertThat(sut).isEmpty();
    }

    @Sql(scripts = "/import_planets.sql")
    @Test
    public void listPlanets_ReturnsFilteredPlanets() {
        Example<Planet> queryWithoutFilters = QueryBuilder.makeQuery(new Planet());
        Example<Planet> queryWithFilters = QueryBuilder.makeQuery(new Planet(TATOOINE.getClimate(), TATOOINE.getTerrain()));

        List<Planet> responseWithoutFilters = planetRepository.findAll(queryWithoutFilters);
        List<Planet> responseWithFilters = planetRepository.findAll(queryWithFilters);

        assertThat(responseWithoutFilters).isNotEmpty();
        assertThat(responseWithoutFilters).hasSize(3);
        assertThat(responseWithFilters).isNotEmpty();
        assertThat(responseWithFilters).hasSize(1);
        assertThat(responseWithFilters.get(0)).isEqualTo(TATOOINE);
    }

    @Test
    public void listPlanets_ReturnsNoPlanets() {
        Example<Planet> query = QueryBuilder.makeQuery(new Planet());

        List<Planet> response = planetRepository.findAll(query);
        assertThat(response).isEmpty();

    }

    @Test
    public void removePlanet_WithExistingId_RemovesPlanetFromDatabase() {
        Planet sut = testEntityManager.persistFlushFind(PLANET);
        assertThatCode(() -> planetRepository.deleteById(sut.getId())).doesNotThrowAnyException();

        Planet sutDeleted = testEntityManager.find(Planet.class, sut.getId());
        assertThat(sutDeleted).isNull();
    }

    @Test
    public void removePlanet_WithUnexistingId_ThrowsException() {
        assertThatCode(() -> planetRepository.deleteById(1L)).isInstanceOf(EmptyResultDataAccessException.class);
    }

}
