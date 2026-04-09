package com.example.catan.Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ConfigLoader {

  private static final String DEFAULT_CLASSPATH =
      "/com/example/catan/config/catan-game.json";

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

}
