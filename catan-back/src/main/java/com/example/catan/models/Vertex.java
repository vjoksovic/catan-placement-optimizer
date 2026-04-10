package com.example.catan.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vertex {
  private int id;
  private List<Integer> fields;
  private List<Integer> neighbours;
  private boolean isAccessible;
  public Vertex(int id, List<Integer> fields, List<Integer> neighbours) {
    this.id = id;
    this.fields = fields;
    this.neighbours = neighbours;
    this.isAccessible = true;
  }
}
