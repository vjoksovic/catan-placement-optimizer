package com.example.catan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameTurnResponse {
  private Response map;
  private Integer nextTurnNumber;

  public GameTurnResponse(Response map, Integer nextTurnNumber) {
    this.map = map;
    this.nextTurnNumber = nextTurnNumber;
  }
}
