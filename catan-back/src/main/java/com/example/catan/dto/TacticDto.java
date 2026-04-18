package com.example.catan.dto;

import com.example.catan.models.enums.Tactic;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TacticDto {

  private List<Tactic> tactics;

}
