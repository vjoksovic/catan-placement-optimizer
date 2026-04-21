package com.example.catan.services;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;

import org.springframework.stereotype.Service;

@Service
public class GameService {

  private int[] order;
  private final DecisionService decisionService;
  private final HeuristicService heuristicService;

  public GameService(DecisionService decisionService, HeuristicService heuristicService) {
    this.decisionService = decisionService;
    this.heuristicService = heuristicService;
    this.order = ConfigLoader.loadGameOrder();
  }

  public Integer play(Map map, int turnNumber) {
    if (turnNumber < 0 || turnNumber >= order.length) 
      throw new IllegalArgumentException("Invalid turn number: " + turnNumber);
    Player player = map.getPlayers().get(order[turnNumber]);
    placeSettlement(map, player);
    int nextTurn = turnNumber + 1;
    return nextTurn < order.length ? nextTurn : null;
  }

  private void placeSettlement(Map map, Player player) {
    Vertex settlement = decisionService.placeSettlement(map, player);
    decisionService.placeRoad(map, settlement, player);
    updateScore(map, player);
  }

  private void updateScore(Map map, Player player) {
    heuristicService.evaluatePlayer(map, player, true);
  }

}
