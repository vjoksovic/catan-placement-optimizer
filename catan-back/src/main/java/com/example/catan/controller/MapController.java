package com.example.catan.controller;

import com.example.catan.models.Map;
import com.example.catan.services.MapGenerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
public class MapController {

  private final MapGenerationService mapGenerationService;

  public MapController(MapGenerationService mapGenerationService) {
    this.mapGenerationService = mapGenerationService;
  }

  @GetMapping("/generate")
  public Map generate() {
    return mapGenerationService.generateMap();
  }
}
