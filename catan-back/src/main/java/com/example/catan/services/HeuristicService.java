package com.example.catan.services;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

  private static final int N_MAX_CANDIDATES = 5;
  private final MapService mapService;
  private final VertexService vertexService;
  private final FieldService fieldService;
  private final CopyService copyService;
  private final MathUtil.HeuristicScalingContext scalingContext;

  public HeuristicService(MapService mapService, VertexService vertexService, FieldService fieldService, CopyService copyService) {
    this.mapService = mapService;
    this.vertexService = vertexService;
    this.fieldService = fieldService;
    this.copyService = copyService;
    java.util.Map<String, Double> maxValues = ConfigLoader.loadHeuristicScalingMaxValues();
    java.util.Map<String, Double> targetShares = ConfigLoader.loadHeuristicScalingTargetShares();
    this.scalingContext = MathUtil.buildHeuristicScalingContext(maxValues, targetShares);
  }
  
  public void calculateHeuristic(Map map) {
    for (Vertex vertex : map.getVertices()) {
      calculateVertexHeuristic(map, vertex);
    }
    mapService.assignHeatmapRatings(map);
    mapService.processPlayerHeuristics(map);
  }

  public Vertex findSettlement(Map map, Player player) {
    Map simulationMap = copyService.deepCopyMap(map);
    List<Player> players = simulationMap.getPlayers();
    int currentPlayer = getPlayer(players, player.getId());
    if (currentPlayer < 0) 
      return null;
    Player simulationPlayer = players.get(currentPlayer);
    List<Integer> nMaxCandidates = getTopSettlementCandidates(simulationMap, simulationPlayer);
    double bestHeuristic = Double.NEGATIVE_INFINITY;
    Integer bestSettlement = null;
    for (Integer candidate : nMaxCandidates) {
      Map branchMap = copyService.deepCopyMap(simulationMap);
      Player branchPlayer = branchMap.getPlayers().get(currentPlayer);
      applySettlementMove(branchMap, branchPlayer, candidate);
      double[] scores = runNMax(branchMap, getNextPlayer(currentPlayer, players.size()), players.size());
      if (scores[currentPlayer] > bestHeuristic) {
        bestHeuristic = scores[currentPlayer];
        bestSettlement = candidate;
      }
    }
    return bestSettlement == null ? null : vertexService.getVertex(map, bestSettlement);
  }

  private double[] runNMax(Map map, int currentPlayerIndex, int depth) {
    List<Player> players = map.getPlayers();
    if (depth == 0) {
      return evaluateAllPlayers(map);
    }
    Player currentPlayer = players.get(currentPlayerIndex);
    List<Integer> candidates = getTopSettlementCandidates(map, currentPlayer);
    if (candidates.isEmpty()) {
      return evaluateAllPlayers(map);
    }
    double[] bestVector = null;
    for (Integer candidateId : candidates) {
      Map childMap = copyService.deepCopyMap(map);
      Player childPlayer = childMap.getPlayers().get(currentPlayerIndex);
      applySettlementMove(childMap, childPlayer, candidateId);
      double[] childVector = runNMax(childMap, getNextPlayer(currentPlayer.getId(), players.size()), depth - 1);
      if (bestVector == null || childVector[currentPlayerIndex] > bestVector[currentPlayerIndex]) {
        bestVector = childVector;
      }
    }
    return bestVector == null ? evaluateAllPlayers(map) : bestVector;
  }

  private List<Integer> getTopSettlementCandidates(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = mapService.getHeuristicsByPlayer(map, player);
    return heuristics.entrySet().stream()
      .filter(entry -> entry.getValue() > 0)
      .sorted(Entry.<Vertex, Double>comparingByValue(Comparator.reverseOrder()))
      .limit(N_MAX_CANDIDATES)
      .map(entry -> entry.getKey().getId())
      .toList();
  }

  private double[] evaluateAllPlayers(Map map) {
    List<Player> players = map.getPlayers();
    double[] scores = new double[players.size()];
    for (int i = 0; i < players.size(); i++) {
      scores[i] = mapService.evaluatePlayer(map, players.get(i), false);
    }
    return scores;
  }

  private void applySettlementMove(Map map, Player player, int settlementId) {
    player.getSettlements().add(settlementId);
    Vertex settlement = vertexService.getVertex(map, settlementId);
    settlement.setSettled(true);
    for (Integer neighbourId : settlement.getRoadFlags().keySet()) {
      Vertex neighbour = vertexService.getVertex(map, neighbourId);
      neighbour.setSettled(true);
    }
  }

  private int getNextPlayer(int currentPlayer, int playerCount) {
    return (currentPlayer + 1) % playerCount;
  }

  private int getPlayer(List<Player> players, int targetPlayerId) {
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).getId() == targetPlayerId) {
        return i;
      }
    }
    return -1;
  }

  private void calculateVertexHeuristic(Map map, Vertex vertex) {
    List<Field> fields = vertexService.getFieldsByVertex(map, vertex.getId());
    vertex.getValue().setProductionValue(fieldService.  calculateProduction(fields));
    vertex.getValue().setResourceDiversityValue(fieldService.calculateResourceDiversity(fields));
    vertex.getValue().setNumberDiversityValue(fieldService.calculateNumberDiversity(fields));
    vertex.getValue().setScarcityValue(fieldService.calculateScarcity(map, fields));
    MathUtil.roundHeuristic(vertex.getValue(), scalingContext, 1);
  }

}
