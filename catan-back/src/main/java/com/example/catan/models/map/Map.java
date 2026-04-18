package com.example.catan.models.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.enums.Resource;
import com.example.catan.utils.ConfigLoader;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Map {
  private List<Field> fields;
  private List<Vertex> vertices;
  private List<Player> players;
  private HashMap<Resource, Integer> production;

  public Map(List<Tactic> tactics) {
    List<List<Integer>> neighbourLists = ConfigLoader.loadFieldNeighbours();
    List<List<Integer>> fieldVertexIds = ConfigLoader.loadFieldVertexIds();
    if (fieldVertexIds.size() != neighbourLists.size()) {
      throw new IllegalArgumentException("catan-fields.json: neighbours/vertices field count mismatch");
    }
    List<Field> list = new ArrayList<>(neighbourLists.size());
    for (int i = 0; i < neighbourLists.size(); i++) {
      list.add(new Field(i, new ArrayList<>(neighbourLists.get(i)), new ArrayList<>()));
    }
    this.fields = list;
    this.vertices = ConfigLoader.loadVertices();

    for (int fieldId = 0; fieldId < fieldVertexIds.size(); fieldId++) {
      for (Integer vertexId : fieldVertexIds.get(fieldId)) {
        if (vertexId != null && vertexId >= 0 && vertexId < this.vertices.size()) {
          this.fields.get(fieldId).getVertices().add(vertexId);
        }
      }
    }
    this.players = new ArrayList<>();
    for (int i = 0; i < tactics.size(); i++) {
      this.players.add(new Player(i, tactics.get(i)));
    }
    this.production = new HashMap<>();
    for (Resource resource : Resource.values()) {
      this.production.put(resource, 0);
    }
  }
}

