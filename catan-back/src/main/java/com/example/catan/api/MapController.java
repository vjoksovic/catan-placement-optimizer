package com.example.catan.api;

import com.example.catan.dto.PlaystyleDto;
import com.example.catan.dto.Response;
import com.example.catan.dto.ResponseMapper;
import com.example.catan.services.GeneratorService;
import com.example.catan.services.HeuristicService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
public class MapController {

  private final HeuristicService heuristicService;
  private final GeneratorService generatorService;

  public MapController(
      HeuristicService heuristicService,
      GeneratorService generatorService) {
    this.heuristicService = heuristicService;
    this.generatorService = generatorService;
  }

  @PostMapping("/generate")
  public Response generate(@RequestBody PlaystyleDto request) {
    com.example.catan.models.map.Map map = generatorService.generateNew(request.getPlaystyles());
    heuristicService.calculateHeuristic(map);
    return ResponseMapper.from(map);
  }
}
