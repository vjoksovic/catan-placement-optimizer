package com.example.catan.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MapGeneratorUtil;
import com.example.catan.utils.MathUtil;

import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

  private final VertexService vertexService;
  private final FieldService fieldService;
  private final MathUtil.HeuristicScalingContext scalingContext;
  private final int nMaxCandidates;

  public HeuristicService(VertexService vertexService, FieldService fieldService) {
    this.vertexService = vertexService;
    this.fieldService = fieldService;
    java.util.Map<String, Double> maxValues = ConfigLoader.loadHeuristicScalingMaxValues();
    java.util.Map<String, Double> targetShares = ConfigLoader.loadHeuristicScalingTargetShares();
    this.scalingContext = MathUtil.buildHeuristicScalingContext(maxValues, targetShares);
    this.nMaxCandidates = ConfigLoader.loadNMaxCandidates();
  }
  
  public void calculateHeuristic(Map map) {
    for (Vertex vertex : map.getVertices()) {
      fieldService.calculateVertexHeuristic(map, vertex);
    }
    MapGeneratorUtil.assignHeatmapRatings(map);
    processPlayerHeuristics(map);
  }

  public List<Integer> getTopSettlementCandidates(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = getHeuristicsByPlayer(map, player);
    return heuristics.entrySet().stream()
      .filter(entry -> entry.getValue() > 0)
      .sorted(Entry.<Vertex, Double>comparingByValue(Comparator.reverseOrder()))
      .limit(nMaxCandidates)
      .map(entry -> entry.getKey().getId())
      .toList();
  }

  public double[] evaluateAllPlayers(Map map) {
    List<Player> players = map.getPlayers();
    double[] scores = new double[players.size()];
    for (int i = 0; i < players.size(); i++) {
      scores[i] = evaluatePlayer(map, players.get(i), false);
    }
    return scores;
  }

  private void processPlayerHeuristics(Map map) {
    for (Player player : map.getPlayers()) {
      processPlayerVertices(map, player);
    }
  }

  private void processPlayerVertices(Map map, Player player) {
    for (Vertex vertex : map.getVertices()) {
      double totalValue = getTotalPlayerValue(vertex, player);
      switch (player.getTactic()) {
        case BALANCED:
          vertex.getValue().setBalancedValue(MathUtil.round2(totalValue));
          break;
        case PRODUCTION_FOCUSED:
          vertex.getValue().setProductionFocusedValue(MathUtil.round2(totalValue));
          break;
        case SCARCITY_FOCUSED:
          vertex.getValue().setScarcityFocusedValue(MathUtil.round2(totalValue));
          break;
      }
    }
  }

  private double getTotalPlayerValue(Vertex vertex, Player player) {
    return vertex.getValue().getProductionValue() * player.getTactic().getProductionWeight()
    + vertex.getValue().getResourceDiversityValue() * player.getTactic().getResourceDiversityWeight()
    + vertex.getValue().getNumberDiversityValue() * player.getTactic().getNumberDiversityWeight()
    + vertex.getValue().getScarcityValue() * player.getTactic().getScarcityWeight();
  }

  private double simulateSettlement(Map map, Player player, Vertex vertex) {
    player.getSettlements().add(vertex.getId());
    double score = evaluatePlayer(map, player, false);
    player.getSettlements().remove(player.getSettlements().size()-1);
    return score;
  }

  public java.util.Map<Vertex, Double> getHeuristicsByPlayer(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = new HashMap<>();
    for (Vertex vertex : map.getVertices()) {
      if (vertex.isSettled() || player.getPlannedSettments().contains(vertex.getId())) 
        heuristics.put(vertex, 0.0);
      else {
        double score = simulateSettlement(map, player, vertex);
        heuristics.put(vertex, score);
      }
    }
    return heuristics;
  }

  public double evaluatePlayer(Map map, Player player, boolean isSettled) {
    List<Field> fields = new ArrayList<>();
    for (Integer vertexId : player.getSettlements()) {
      fields.addAll(vertexService.getFieldsByVertex(map, vertexId));
    }
    double productionValue = fieldService.calculateProduction(fields);
    double resourceDiversityValue = fieldService.calculateResourceDiversity(fields);
    double numberDiversityValue = fieldService.calculateNumberDiversity(fields);
    double scarcityValue = fieldService.calculateScarcity(map, fields);
    Heuristic heuristic = new Heuristic(productionValue, resourceDiversityValue, numberDiversityValue, scarcityValue);
    MathUtil.roundHeuristic(heuristic, scalingContext, player.getSettlements().size());
    if (isSettled)
      player.getScore().setValues(heuristic.getProductionValue(), heuristic.getResourceDiversityValue(), heuristic.getNumberDiversityValue(), heuristic.getScarcityValue());
    return player.getScore().calculateTotal(heuristic);
  }

}
