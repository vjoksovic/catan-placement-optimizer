package com.example.catan.services;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.catan.interfaces.GameInterface;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;

@Service
public class DecisionService implements GameInterface {

  private final CopyService copyService;
  private final VertexService vertexService;
  private final HeuristicService heuristicService;
  private final int maxNDepth;

  public DecisionService(CopyService copyService, VertexService vertexService, HeuristicService heuristicService) {
    this.copyService = copyService;
    this.vertexService = vertexService;
    this.heuristicService = heuristicService;
    this.maxNDepth = ConfigLoader.loadMaxNDepth();
  }

  public Vertex placeSettlement(Map map, Player player) {
    Vertex settlement = findSettlement(map, player);
    if (settlement != null) {
      player.getSettlements().add(settlement.getId());
      vertexService.setSettled(map, settlement);
    }
    return settlement;
  }
  
  private Vertex findSettlement(Map map, Player player) {
    Map simulationMap = copyService.deepCopyMap(map);
    List<Player> players = simulationMap.getPlayers();
    int currentPlayer = player.getId();
    Player simulationPlayer = players.get(currentPlayer);
    List<Integer> nMaxCandidates = heuristicService.getTopSettlementCandidates(simulationMap, simulationPlayer);
    return getBestSettlement(map, simulationMap, currentPlayer, nMaxCandidates);
  }

  private Vertex getBestSettlement(Map originalMap, Map simulationMap, int currentPlayer, List<Integer> nMaxCandidates) {
    double bestHeuristic = Double.NEGATIVE_INFINITY;
    Integer bestSettlement = null;
    for (Integer candidate : nMaxCandidates) {
      Map branchMap = copyService.deepCopyMap(simulationMap);
      Player branchPlayer = branchMap.getPlayers().get(currentPlayer);
      vertexService.simulateSettlementMove(branchMap, branchPlayer, candidate);
      placeRoad(branchMap, vertexService.getVertex(branchMap, candidate), branchPlayer);
      double[] scores = runNMax(branchMap, getNextPlayer(currentPlayer, simulationMap.getPlayers().size()), maxNDepth);
      if (scores[currentPlayer] > bestHeuristic) {
        bestHeuristic = scores[currentPlayer];
        bestSettlement = candidate;
      }
    }
    return vertexService.getVertex(originalMap, bestSettlement == null ? null : bestSettlement);
  }

  private double[] runNMax(Map map, int currentPlayerIndex, int depth) {
    List<Player> players = map.getPlayers();
    if (depth == 0) {
      return heuristicService.evaluateAllPlayers(map);
    }
    Player currentPlayer = players.get(currentPlayerIndex);
    List<Integer> candidates = heuristicService.getTopSettlementCandidates(map, currentPlayer);
    if (candidates.isEmpty()) {
      return heuristicService.evaluateAllPlayers(map);
    }
    double[] bestVector = null;
    for (Integer candidateId : candidates) {
      Map childMap = copyService.deepCopyMap(map);
      Player childPlayer = childMap.getPlayers().get(currentPlayerIndex);
      vertexService.simulateSettlementMove(childMap, childPlayer, candidateId);
      double[] childVector = runNMax(childMap, getNextPlayer(currentPlayer.getId(), players.size()), depth - 1);
      if (bestVector == null || childVector[currentPlayerIndex] > bestVector[currentPlayerIndex]) {
        bestVector = childVector;
      }
    }
    return bestVector;
  }

  private Vertex findRoute(Map map, Vertex vertex, Player player) {
    double bestHeuristic = 0;
    Vertex bestNeighbour = null;
    Vertex bestPlanned = null;
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = vertexService.getVertex(map, neighbourId);
      java.util.Map<Vertex, Double> result = getBestNeighbour(map, neighbour, player);
      for (java.util.Map.Entry<Vertex, Double> entry : result.entrySet()) {
        if (entry.getValue() > bestHeuristic) {
          bestHeuristic = entry.getValue();
          bestNeighbour = neighbour;
          bestPlanned = entry.getKey();
        }
      }
    }
    if (bestPlanned != null)
      player.getPlannedSettments().add(bestPlanned.getId());
    return bestNeighbour;
  }

  public void placeRoad(Map map, Vertex vertex, Player player) {
    Vertex bestNeighbour = findRoute(map, vertex, player);
    if (bestNeighbour != null) 
      vertex.getRoadFlags().put(bestNeighbour.getId(), true);
  }

  private int getNextPlayer(int currentPlayer, int playerCount) {
    return (currentPlayer + 1) % playerCount;
  }

  private java.util.Map<Vertex, Double> getBestNeighbour(Map map, Vertex vertex, Player player) {
    double bestHeuristic = 0;
    java.util.Map<Vertex, Double> bestEntry = new HashMap<>();
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = vertexService.getVertex(map, neighbourId);
      if (neighbour.isSettled() || player.getPlannedSettments().contains(neighbour.getId())) 
        continue;
      double heuristic = heuristicService.getHeuristicsByPlayer(map, player).get(neighbour);
      if (heuristic > bestHeuristic) {
        bestHeuristic = heuristic;
        bestEntry.put(neighbour, heuristic);
      }
    }
    return bestEntry;
  }

}
