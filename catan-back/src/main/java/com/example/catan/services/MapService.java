package com.example.catan.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Field;
import com.example.catan.models.enums.Resource;
import org.springframework.stereotype.Service;

@Service
public class MapService {
  
  public List<Field> getFields(Map map, int vertexId) {
    List<Field> fields = new ArrayList<>();
    for (int fieldId : map.getVertices().get(vertexId).getFields()) {
      fields.add(map.getFields().get(fieldId));
    }
    return fields;
  }

  public double calculateMaxProduction(Map map) {
    int maxProduction = 0;
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      maxProduction = Math.max(maxProduction, entry.getValue());
    }
    return maxProduction;
  }
}
