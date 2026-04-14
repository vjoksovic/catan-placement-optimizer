package com.example.catan.utils;

import com.example.catan.models.map.Vertex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {

  private static final String DEFAULT_CLASSPATH =
      "/com/example/catan/config/game.json";
  private static final String FIELDS_CLASSPATH =
      "/com/example/catan/config/fields.json";
  private static final String VERTICES_CLASSPATH =
      "/com/example/catan/config/vertices.json";
  private static final String HEURISTICS_CLASSPATH =
      "/com/example/catan/config/heuristic.json";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ConfigLoader() {}

  public static HashMap<String, Integer> loadResourceMap() {
    try (InputStream in = openClasspath(DEFAULT_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      return new HashMap<>(parseStringIntMap(root.path("resource")));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static HashMap<Integer, Integer> loadNumberMap() {
    try (InputStream in = openClasspath(DEFAULT_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      return new HashMap<>(parseIntIntMap(root.path("number") ));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static HashMap<Integer, Integer> loadProductionMap() {
    try (InputStream in = openClasspath(DEFAULT_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      return new HashMap<>(parseIntIntMap(root.path("production")));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Map<String, Double> loadResourceWeights() {
    try (InputStream in = openClasspath(HEURISTICS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      return parseStringDoubleMap(root.path("resourceWeights"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final Map<String, Double> DEFAULT_SCARCITY_RESOURCE_MULTIPLIERS;

  static {
    Map<String, Double> d = new HashMap<>();
    d.put("WOOD", 1.15);
    d.put("BRICK", 1.15);
    d.put("SHEEP", 0.85);
    d.put("WHEAT", 1.25);
    d.put("ORE", 1.25);
    d.put("DESERT", 0.0);
    DEFAULT_SCARCITY_RESOURCE_MULTIPLIERS = Collections.unmodifiableMap(d);
  }

  /**
   * Per-resource multipliers applied to scarcity points per adjacent field (see {@code HeuristicService}).
   * JSON entries override {@link #DEFAULT_SCARCITY_RESOURCE_MULTIPLIERS}; missing keys keep defaults.
   */
  public static Map<String, Double> loadScarcityResourceMultipliers() {
    try (InputStream in = openClasspath(HEURISTICS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      Map<String, Double> parsed = parseStringDoubleMap(root.path("scarcityResourceMultipliers"));
      Map<String, Double> out = new HashMap<>(DEFAULT_SCARCITY_RESOURCE_MULTIPLIERS);
      out.putAll(parsed);
      return Collections.unmodifiableMap(out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Map<String, Map<String, Double>> loadPlaystyleWeights() {
    try (InputStream in = openClasspath(HEURISTICS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      JsonNode playstylesNode = root.path("playstyleWeights");
      Map<String, Map<String, Double>> weightsByPlaystyle = new HashMap<>();
      if (!playstylesNode.isObject()) {
        return weightsByPlaystyle;
      }

      Iterator<String> playstyleNames = playstylesNode.fieldNames();
      while (playstyleNames.hasNext()) {
        String playstyleName = playstyleNames.next();
        JsonNode playstyleNode = playstylesNode.path(playstyleName);
        weightsByPlaystyle.put(playstyleName, parseStringDoubleMap(playstyleNode));
      }
      return weightsByPlaystyle;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final double[] DEFAULT_HEATMAP_RATING_CAPS = {0.15, 0.4, 0.6, 0.85};

  /**
   * Normalized min–max caps for {@link com.example.catan.models.enums.HeatmapRating}: four thresholds
   * {@code t} in {@code [0,1]} — {@code VERY_LOW} if {@code t < caps[0]}, {@code LOW} if {@code < caps[1]}, etc.;
   * {@code VERY_HIGH} if {@code t >= caps[3]}.
   * <p>Configured in {@code catan-heuristic.json} as {@code "heatmapRatingCaps": [c0,c1,c2,c3]}.</p>
   *
   * @return a new array (callers may cache)
   */
  public static double[] loadHeatmapRatingCaps() {
    try (InputStream in = openClasspath(HEURISTICS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      JsonNode arr = root.path("heatmapRatingCaps");
      if (!arr.isArray() || arr.size() != 4) {
        return DEFAULT_HEATMAP_RATING_CAPS.clone();
      }
      double[] out = new double[4];
      for (int i = 0; i < 4; i++) {
        out[i] = arr.get(i).asDouble();
      }
      for (int i = 1; i < 4; i++) {
        if (out[i] <= out[i - 1]) {
          return DEFAULT_HEATMAP_RATING_CAPS.clone();
        }
      }
      if (out[0] <= 0.0 || out[3] >= 1.0) {
        return DEFAULT_HEATMAP_RATING_CAPS.clone();
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Board topology: one entry per field id 0..18 in order, each list is neighbour field ids.
   */
  public static List<List<Integer>> loadFieldNeighbours() {
    try (InputStream in = openClasspath(FIELDS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      JsonNode fields = root.path("fields");
      if (!fields.isArray()) {
        throw new IllegalArgumentException("catan-fields.json: \"fields\" must be an array");
      }
      List<List<Integer>> out = new ArrayList<>();
      int index = 0;
      for (JsonNode f : fields) {
        int id = f.path("id").asInt(-1);
        if (id != index) {
          throw new IllegalArgumentException(
              "catan-fields.json: expected field id " + index + ", got " + id);
        }
        List<Integer> neighbours = new ArrayList<>();
        JsonNode nb = f.path("neighbours");
        if (nb.isArray()) {
          for (JsonNode n : nb) {
            neighbours.add(n.asInt());
          }
        }
        out.add(neighbours);
        index++;
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Per-field vertex ids in clockwise order from top vertex.
   */
  public static List<List<Integer>> loadFieldVertexIds() {
    try (InputStream in = openClasspath(FIELDS_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      JsonNode fields = root.path("fields");
      if (!fields.isArray()) {
        throw new IllegalArgumentException("catan-fields.json: \"fields\" must be an array");
      }
      List<List<Integer>> out = new ArrayList<>();
      int index = 0;
      for (JsonNode f : fields) {
        int id = f.path("id").asInt(-1);
        if (id != index) {
          throw new IllegalArgumentException(
              "catan-fields.json: expected field id " + index + ", got " + id);
        }
        out.add(parseIntList(f.path("vertices")));
        index++;
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Vertex topology: one entry per vertex id in order.
   * Each entry contains field ids touching that vertex and neighbouring vertex ids.
   */
  public static List<Vertex> loadVertices() {
    try (InputStream in = openClasspath(VERTICES_CLASSPATH)) {
      JsonNode root = MAPPER.readTree(in);
      JsonNode vertices = root.path("vertices");
      if (!vertices.isArray()) {
        throw new IllegalArgumentException("catan-vertices.json: \"vertices\" must be an array");
      }

      List<Vertex> out = new ArrayList<>();
      int size = vertices.size();
      int index = 0;
      for (JsonNode v : vertices) {
        int id = v.path("id").asInt(-1);
        if (id != index) {
          throw new IllegalArgumentException(
              "catan-vertices.json: expected vertex id " + index + ", got " + id);
        }

        List<Integer> fields = parseIntList(v.path("fields"));
        HashMap<Integer, Boolean> neighbours = new HashMap<>();
        for (Integer nId : parseIntList(v.path("neighbours"))) {
          if (nId != null && nId >= 0 && nId < size) {
            neighbours.put(nId, false);
          }
        }
        out.add(new Vertex(id, fields, neighbours));
        index++;
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static InputStream openClasspath(String classpathLocation) {
    String path = classpathLocation.startsWith("/") ? classpathLocation : "/" + classpathLocation;
    InputStream in = ConfigLoader.class.getResourceAsStream(path);
    if (in == null) {
      throw new IllegalArgumentException("Classpath resource not found: " + path);
    }
    return in;
  }

  private static Map<String, Integer> parseStringIntMap(JsonNode node) {
    Map<String, Integer> map = new HashMap<>();
    if (node == null || !node.isObject()) {
      return map;
    }
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      String key = names.next();
      map.put(key, node.get(key).asInt());
    }
    return map;
  }

  private static Map<Integer, Integer> parseIntIntMap(JsonNode node) {
    Map<Integer, Integer> map = new HashMap<>();
    if (node == null || !node.isObject()) {
      return map;
    }
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      String key = names.next();
      map.put(Integer.parseInt(key), node.get(key).asInt());
    }
    return map;
  }

  private static Map<String, Double> parseStringDoubleMap(JsonNode node) {
    Map<String, Double> map = new HashMap<>();
    if (node == null || !node.isObject()) {
      return map;
    }
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      String key = names.next();
      map.put(key, node.path(key).asDouble());
    }
    return map;
  }

  private static List<Integer> parseIntList(JsonNode node) {
    List<Integer> list = new ArrayList<>();
    if (node == null || !node.isArray()) {
      return list;
    }
    for (JsonNode n : node) {
      list.add(n.asInt());
    }
    return list;
  }

}
