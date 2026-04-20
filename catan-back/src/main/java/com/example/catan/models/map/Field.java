package com.example.catan.models.map;

import java.util.List;

import com.example.catan.models.enums.Resource;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class Field {
  
  private int id;
  private int fieldNumber;
  private Resource resource;
  private List<Integer> neighbours;
  private List<Integer> vertices;
  private int productionValue;

  public Field(int id, List<Integer> neighbours, List<Integer> vertices) {
    this.id = id;
    this.neighbours = neighbours;
    this.vertices = vertices;
  }
}
