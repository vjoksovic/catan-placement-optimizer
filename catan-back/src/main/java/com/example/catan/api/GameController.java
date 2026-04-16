package com.example.catan.api;

import com.example.catan.dto.GameTurnRequest;
import com.example.catan.dto.GameTurnResponse;
import com.example.catan.dto.Response;
import com.example.catan.dto.ResponseMapper;
import com.example.catan.models.map.Map;
import com.example.catan.services.GameService;
import com.example.catan.services.GeneratorService;
import com.example.catan.services.HeuristicService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

  private final GeneratorService generatorService;
  private final HeuristicService heuristicService;
  private final GameService gameService;

  public GameController(
      GeneratorService generatorService,
      HeuristicService heuristicService,
      GameService gameService) {
    this.generatorService = generatorService;
    this.heuristicService = heuristicService;
    this.gameService = gameService;
  }

  @PostMapping("/turn")
  public GameTurnResponse playTurn(@RequestBody GameTurnRequest request) {
    Map map = generatorService.getCurrentMap();
    heuristicService.calculateHeuristic(map);
    Integer nextTurn = gameService.play(map, request.getTurnNumber());
    heuristicService.calculateHeuristic(map);
    Response response = ResponseMapper.from(map);
    return new GameTurnResponse(response, nextTurn);
  }
}
