package com.example.catan.interfaces;

import java.util.List;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.map.Map;

public interface MapInterface {
  
  public Map generateMap(List<Tactic> tactics);
}
