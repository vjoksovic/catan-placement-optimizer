package com.example.catan.models.map;

import java.util.ArrayList;
import java.util.List;

import com.example.catan.models.enums.Playstyle;
import com.example.catan.models.values.Score;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
  private int id;
  private Playstyle playstyle;
  /** Vertex ids where this player has a settlement (two entries after placement). */
  private List<Integer> settlementVertexIds;
  /** Each entry is a pair {@code [v1, v2]} with {@code v1 < v2} (two roads per player). */
  private List<List<Integer>> roads;
  private Score score;
  private boolean isPlaying;

  public Player(int id, Playstyle playstyle) {
    this.id = id;
    this.playstyle = playstyle;
    this.settlementVertexIds = new ArrayList<>();
    this.roads = new ArrayList<>();
    this.score = new Score();
    if (id == 0) 
      this.isPlaying = true;
    else
      this.isPlaying = false;
  }

}
