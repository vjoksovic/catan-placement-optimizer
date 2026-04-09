package com.example.catan.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Map {
  private List<Field> fields;

  public Map() {
    this.fields = Arrays.asList(
      new Field(0, new ArrayList<>(Arrays.asList(1, 3, 4))), 
      new Field(1, new ArrayList<>(Arrays.asList(0, 2, 4, 5))), 
      new Field(2, new ArrayList<>(Arrays.asList(1, 5, 6))), 
      new Field(3, new ArrayList<>(Arrays.asList(0, 4, 7, 8))), 
      new Field(4, new ArrayList<>(Arrays.asList(0, 1, 3, 5, 8, 9))), 
      new Field(5, new ArrayList<>(Arrays.asList(1, 2, 4, 6, 9, 10))),
      new Field(6, new ArrayList<>(Arrays.asList(2, 5, 10, 11))),
      new Field(7, new ArrayList<>(Arrays.asList(3, 8, 12))),
      new Field(8, new ArrayList<>(Arrays.asList(3, 4, 7, 9, 12, 13))),
      new Field(9, new ArrayList<>(Arrays.asList(4, 5, 8, 10, 13, 14))),
      new Field(10, new ArrayList<>(Arrays.asList(5, 6, 9, 11, 14, 15))),
      new Field(11, new ArrayList<>(Arrays.asList(6, 11, 15))),
      new Field(12, new ArrayList<>(Arrays.asList(7, 8, 13, 16))),
      new Field(13, new ArrayList<>(Arrays.asList(8, 9, 12, 14, 16, 17))),
      new Field(14, new ArrayList<>(Arrays.asList(9, 10, 13, 15, 17, 18))),
      new Field(15, new ArrayList<>(Arrays.asList(10, 11, 14, 18))),
      new Field(16, new ArrayList<>(Arrays.asList(12, 13, 17))),
      new Field(17, new ArrayList<>(Arrays.asList(13, 14, 16, 18))),
      new Field(18, new ArrayList<>(Arrays.asList(14, 15, 17))));
  }
 
}

