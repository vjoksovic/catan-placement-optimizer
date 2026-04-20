package com.example.catan.models.map;

import java.util.ArrayList;
import java.util.List;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.values.Score;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
  
  private int id;
  private Tactic tactic;
  private List<Integer> settlements;
  private Score score;
  private List<Integer> plannedSettments;

  public Player() {}

  public Player(int id, Tactic tactic) {
    this.id = id;
    this.tactic = tactic;
    this.settlements = new ArrayList<>();
    this.score = new Score();
    this.plannedSettments = new ArrayList<>();
  }

}
