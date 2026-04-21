package com.example.catan.interfaces;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;

public interface GameInterface {

  public Vertex placeSettlement(Map map, Player player);

  public void placeRoad(Map map, Vertex vertex, Player player);
}
