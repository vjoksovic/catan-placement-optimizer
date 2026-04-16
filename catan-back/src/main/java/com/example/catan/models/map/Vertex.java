package com.example.catan.models.map;

import java.util.HashMap;
import java.util.List;

import com.example.catan.models.enums.HeatmapRating;
import com.example.catan.models.values.Heuristic;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vertex {
  private int id;
  private List<Integer> fields;
  private HashMap<Integer, Boolean> roadFlags;
  private Heuristic value;
  private HeatmapRating heatmapRating;
  private boolean isSettled;

  public Vertex(int id, List<Integer> fields, HashMap<Integer, Boolean> roadFlags) {
    this.id = id;
    this.fields = fields;
    this.roadFlags = roadFlags;
    this.value = new Heuristic();
    this.isSettled = false;
  }
}
