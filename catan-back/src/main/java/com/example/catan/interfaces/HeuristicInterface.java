package com.example.catan.interfaces;

import java.util.List;

import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;

public interface HeuristicInterface {
  
  double calculateProduction(List<Field> fields);

  double calculateResourceDiversity(List<Field> fields);

  double calculateNumberDiversity(List<Field> fields);

  double calculateScarcity(Map map, List<Field> fields);
}
