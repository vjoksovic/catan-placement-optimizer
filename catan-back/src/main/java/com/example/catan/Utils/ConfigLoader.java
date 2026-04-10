package com.example.catan.utils;

import com.example.catan.models.Vertex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {

  private static final String DEFAULT_CLASSPATH =
      "/com/example/catan/config/catan-game.json";

  private static final String FIELDS_CLASSPATH =
      "/com/example/catan/config/catan-fields.json";
  private static final String VERTICES_CLASSPATH =
      "/com/example/catan/config/catan-vertices.json";

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
        List<Integer> neighbourIds = new ArrayList<>();
        for (Integer nId : parseIntList(v.path("neighbours"))) {
          if (nId != null && nId >= 0 && nId < size) {
            neighbourIds.add(nId);
          }
        }
        out.add(new Vertex(id, fields, neighbourIds));
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
