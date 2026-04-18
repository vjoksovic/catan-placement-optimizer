package com.example.catan.dto;

import java.util.List;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.values.Score;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerDto {
  private int id;
  private Tactic tactic;
  private List<Integer> settlements;
  private Score score;
}
