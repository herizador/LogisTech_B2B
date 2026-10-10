package com.logistech.fms.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente del API público de OSRM (fase 2). Sin clave, con límites de uso;
 * si falla, el servicio vuelve a Haversine automáticamente.
 */
@Component
public class OsrmClient {

  private final RestClient rest;
  private final String baseUrl;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public OsrmClient(@Value("${app.osrm.base-url:https://router.project-osrm.org}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(20));
    this.rest = RestClient.builder()
        .requestFactory(factory)
        .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter()))
        .build();
    this.baseUrl = baseUrl;
  }

  public record Matrices(long[][] durationsSeconds, double[][] distancesMeters) {
  }

  private static String coords(List<RoutingLocation> locations) {
    return locations.stream()
        .map(l -> l.getLongitude() + "," + l.getLatitude())
        .collect(Collectors.joining(";"));
  }

  /** Matriz de tiempos y distancias entre todas las ubicaciones. */
  public Matrices table(List<RoutingLocation> locations) {
    JsonNode json = getJson("/table/v1/driving/" + coords(locations) + "?annotations=duration,distance");
    assertOk(json);
    int n = locations.size();
    long[][] durations = new long[n][n];
    double[][] distances = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        durations[i][j] = Math.round(json.get("durations").get(i).get(j).asDouble());
        distances[i][j] = json.get("distances").get(i).get(j).asDouble();
      }
    }
    return new Matrices(durations, distances);
  }

  /** Geometría GeoJSON [lng,lat] de la ruta ordenada pasada. */
  public List<List<Double>> routeGeometry(List<RoutingLocation> ordered) {
    JsonNode json = getJson("/route/v1/driving/" + coords(ordered) + "?overview=full&geometries=geojson");
    assertOk(json);
    JsonNode coordinates = json.get("routes").get(0).get("geometry").get("coordinates");
    List<List<Double>> points = new java.util.ArrayList<>();
    coordinates.forEach(p -> points.add(List.of(p.get(0).asDouble(), p.get(1).asDouble())));
    return points;
  }

  private JsonNode getJson(String path) {
    String body = rest.get()
        .uri(baseUrl + path)
        .retrieve()
        .body(String.class);
    try {
      return objectMapper.readTree(body);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("OSRM devolvió JSON no válido: "
          + (body == null ? "vacío" : body.substring(0, Math.min(120, body.length()))));
    }
  }

  private static void assertOk(JsonNode json) {
    if (json == null || !json.has("code") || !"Ok".equals(json.get("code").asText())) {
      throw new IllegalStateException("OSRM devolvió respuesta no válida");
    }
  }
}
