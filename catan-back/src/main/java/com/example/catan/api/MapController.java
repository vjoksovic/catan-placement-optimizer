package com.example.catan.api;

import com.example.catan.dto.PlaystyleRequest;
import com.example.catan.models.map.Map;
import com.example.catan.services.GeneratorService;
import com.example.catan.services.HeuristicService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
public class MapController {

  private final HeuristicService heuristicService;
  private final ObjectProvider<GeneratorService> generatorServiceProvider;

  public MapController(
      HeuristicService heuristicService,
      ObjectProvider<GeneratorService> generatorServiceProvider) {
    this.heuristicService = heuristicService;
    this.generatorServiceProvider = generatorServiceProvider;
  }

  @PostMapping("/generate")
  public Map generate(@RequestBody PlaystyleRequest request) {
    GeneratorService generatorService = generatorServiceProvider.getObject();
    Map map = generatorService.generateNew(request.getPlaystyles());
    heuristicService.calculateHeuristic(map);
    return map;
  }
}
