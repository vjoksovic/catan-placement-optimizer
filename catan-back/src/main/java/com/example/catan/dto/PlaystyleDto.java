package com.example.catan.dto;

import com.example.catan.models.enums.Playstyle;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaystyleDto {

  private List<Playstyle> playstyles;

}
