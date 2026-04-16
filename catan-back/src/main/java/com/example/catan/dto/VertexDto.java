package com.example.catan.dto;

import java.util.HashMap;
import java.util.List;

import com.example.catan.models.enums.HeatmapRating;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VertexDto {
  private int id;
  private List<Integer> fields;
  private HashMap<Integer, Boolean> roadFlags;
  private HeatmapRating heatmapRating;
  private HeuristicDto value;
  private boolean isSettled;
}
