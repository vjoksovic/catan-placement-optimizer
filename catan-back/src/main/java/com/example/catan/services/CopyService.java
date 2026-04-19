package com.example.catan.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;
import com.example.catan.models.values.Score;

@Service
public class CopyService {
  
  public Map deepCopyMap(Map originalMap) {
    List<Tactic> tactics = new ArrayList<>();
    for (Player player : originalMap.getPlayers()) {
      tactics.add(player.getTactic());
    }
    Map copiedMap = new Map(tactics);
    copiedMap.setFields(copyFields(originalMap.getFields()));
    copiedMap.setVertices(copyVertices(originalMap.getVertices()));
    copiedMap.setPlayers(copyPlayers(originalMap.getPlayers()));
    copiedMap.setProduction(new HashMap<>(originalMap.getProduction()));
    return copiedMap;
  }

  private List<Field> copyFields(List<Field> fields) {
    List<Field> copiedFields = new ArrayList<>();
    for (Field field : fields) {
      Field copiedField = new Field(field.getId(), new ArrayList<>(field.getNeighbours()), new ArrayList<>(field.getVertices()));
      copiedField.setFieldNumber(field.getFieldNumber());
      copiedField.setResource(field.getResource());
      copiedField.setProductionValue(field.getProductionValue());
      copiedFields.add(copiedField);
    }
    return copiedFields;
  }

  private List<Vertex> copyVertices(List<Vertex> vertices) {
    List<Vertex> copiedVertices = new ArrayList<>();
    for (Vertex vertex : vertices) {
      Vertex copiedVertex = new Vertex(vertex.getId(), new ArrayList<>(vertex.getFields()), new HashMap<>(vertex.getRoadFlags()));
      copiedVertex.setSettled(vertex.isSettled());
      copiedVertex.setHeatmapRating(vertex.getHeatmapRating());
      copiedVertex.setValue(copyHeuristic(vertex.getValue()));
      copiedVertices.add(copiedVertex);
    }
    return copiedVertices;
  }

  private List<Player> copyPlayers(List<Player> players) {
    List<Player> copiedPlayers = new ArrayList<>();
    for (Player player : players) {
      Player copiedPlayer = new Player(player.getId(), player.getTactic());
      copiedPlayer.setSettlements(new ArrayList<>(player.getSettlements()));
      copiedPlayer.setPlannedSettments(new ArrayList<>(player.getPlannedSettments()));
      copiedPlayer.setScore(copyScore(player.getScore()));
      copiedPlayers.add(copiedPlayer);
    }
    return copiedPlayers;
  }

  private Heuristic copyHeuristic(Heuristic heuristic) {
    Heuristic copiedHeuristic = new Heuristic();
    copiedHeuristic.setProductionValue(heuristic.getProductionValue());
    copiedHeuristic.setResourceDiversityValue(heuristic.getResourceDiversityValue());
    copiedHeuristic.setNumberDiversityValue(heuristic.getNumberDiversityValue());
    copiedHeuristic.setScarcityValue(heuristic.getScarcityValue());
    copiedHeuristic.setBalancedValue(heuristic.getBalancedValue());
    copiedHeuristic.setProductionFocusedValue(heuristic.getProductionFocusedValue());
    copiedHeuristic.setScarcityFocusedValue(heuristic.getScarcityFocusedValue());
    copiedHeuristic.setOverallValue(heuristic.getOverallValue());
    return copiedHeuristic;
  }

  private Score copyScore(Score score) {
    Score copiedScore = new Score();
    copiedScore.setProductionScore(score.getProductionScore());
    copiedScore.setDiversityScore(score.getDiversityScore());
    copiedScore.setScarcityScore(score.getScarcityScore());
    copiedScore.setTotalScore(score.getTotalScore());
    return copiedScore;
  }
}
