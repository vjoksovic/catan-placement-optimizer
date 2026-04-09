package com.example.catan.services;

import com.example.catan.models.Map;
import org.springframework.stereotype.Service;

@Service
public class MapGenerationService {

  public Map generateMap() {
    // Keep existing generation logic in MapService unchanged.
    return new MapService().generateNew();
  }
}
