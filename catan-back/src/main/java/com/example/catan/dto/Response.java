package com.example.catan.dto;

import java.util.List;
import java.util.Map;

import com.example.catan.models.enums.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response {
  private List<FieldDto> fields;
  private List<VertexDto> vertices;
  private List<PlayerDto> players;
  private Map<Resource, Integer> production;
}
