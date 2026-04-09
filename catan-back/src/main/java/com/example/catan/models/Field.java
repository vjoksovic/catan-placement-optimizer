package com.example.catan.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class Field {
  private int id;
  private int fieldNumber;
  private Resource resource;
  private List<Integer> neighbours;
  private List<Integer> spots;
  private List<Integer> edges;
  private List<Integer> unavailableSpots;

  public Field(int id, int fieldNumber, Resource resource, List<Integer> neighbours) {
    this.id = id;
    this.fieldNumber = fieldNumber;
    this.resource = resource;
    this.neighbours = neighbours;
    this.spots = Arrays.asList(0, 1, 2, 3, 4, 5);
    this.edges = Arrays.asList(0, 1, 2, 3, 4, 5);
    this.unavailableSpots = new ArrayList<>();
  }

  public Field(int id, List<Integer> neighbours) {
    this.id = id;
    this.neighbours = neighbours;
    this.spots = Arrays.asList(0, 1, 2, 3, 4, 5);
    this.edges = Arrays.asList(0, 1, 2, 3, 4, 5);
    this.unavailableSpots = new ArrayList<>();
  }
}
