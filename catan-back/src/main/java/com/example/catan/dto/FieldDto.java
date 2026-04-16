package com.example.catan.dto;

import java.util.List;

import com.example.catan.models.enums.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldDto {
  private int id;
  private int fieldNumber;
  private Resource resource;
  private List<Integer> neighbours;
  private List<Integer> vertices;
  private int productionValue;
}
