package com.example.catan.services;

import com.example.catan.models.enums.Playstyle;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;

import org.springframework.stereotype.Service;

@Service
public class GameService {

  private int[] order;
  private MapService mapService;

  public GameService(MapService mapService) {
    this.mapService = mapService;
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
    Vertex settlement = mapService.findSettlement(map, player.getPlaystyle());
    player.getSettlements().add(settlement.getId());
    mapService.setSettled(map, settlement);
    placeRoad(map, settlement, player.getPlaystyle());
    updateScore(player, settlement);
  }

  private void placeRoad(Map map, Vertex vertex, Playstyle playstyle) {
    Vertex bestNeighbour = mapService.findRoute(map, vertex, playstyle);
    vertex.getRoadFlags().put(bestNeighbour.getId(), true);
  }

  private void updateScore(Player player, Vertex vertex) {
    player.getScore().setValues(vertex.getValue().getProductionValue(), 
      vertex.getValue().getResourceDiversityValue(), 
      vertex.getValue().getNumberDiversityValue(), 
      vertex.getValue().getScarcityValue());
  }

}
