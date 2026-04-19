package com.example.catan.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Vertex;

@Service
public class VertexService {

  public List<Field> getFieldsByVertex(Map map, int vertexId) {
    List<Field> fields = new ArrayList<>();
    for (int fieldId : map.getVertices().get(vertexId).getFields()) {
      fields.add(map.getFields().get(fieldId));
    }
    return fields;
  }

  public Vertex getVertexByHeuristic(Map map, double heuristic, Tactic tactic) {
    for (Vertex vertex : map.getVertices()) {
      switch (tactic) {
        case BALANCED:
          if (vertex.getValue().getBalancedValue() == heuristic) {
            return vertex;
          }
        case PRODUCTION_FOCUSED:
          if (vertex.getValue().getProductionFocusedValue() == heuristic) {
            return vertex;
          }
        case SCARCITY_FOCUSED:
          if (vertex.getValue().getScarcityFocusedValue() == heuristic) {
            return vertex;
          }
      }
    }
    return null;
  }

  public Vertex getVertex(Map map, int vertexId) {
    return map.getVertices().get(vertexId);
  }

  public void setSettled(Map map, Vertex vertex) {
    vertex.setSettled(true);
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = getVertex(map, neighbourId);
      neighbour.setSettled(true);
    }
  }
}
