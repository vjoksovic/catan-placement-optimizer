package com.example.catan.services;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PlaygroundService implements ApplicationRunner {

  private static final Path RESULTS_FILE = Path.of(
      "src", "main", "resources", "playground-results", "headless-results.csv");
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaygroundService.class);
  private static final AtomicBoolean HEADLESS_RUN_GUARD = new AtomicBoolean(false);
  private static final List<Tactic> PLAYGROUND_TACTICS = List.of(
      Tactic.BALANCED,
      Tactic.PRODUCTION_FOCUSED,
      Tactic.SCARCITY_FOCUSED
  );

  private final GeneratorService generatorService;
  private final HeuristicService heuristicService;
  private final GameService gameService;
  private final int gamesPerTactic;
  private final int totalGames;

  @Value("${playground.headless.enabled:false}")
  private boolean headlessModeEnabled;

  public PlaygroundService(
      GeneratorService generatorService,
      HeuristicService heuristicService,
      GameService gameService
  ) {
    this.generatorService = generatorService;
    this.heuristicService = heuristicService;
    this.gameService = gameService;
    this.gamesPerTactic = ConfigLoader.loadGamesPerTactic();
    int configuredTotal = ConfigLoader.loadTotalGames();
    int expectedTotal = this.gamesPerTactic * PLAYGROUND_TACTICS.size();
    this.totalGames = configuredTotal > 0 ? configuredTotal : expectedTotal;
    if (this.totalGames != expectedTotal) {
      LOGGER.warn(
          "Configured totalGames ({}) does not match gamesPerTactic * tacticCount ({}).",
          this.totalGames,
          expectedTotal
      );
    }
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!headlessModeEnabled) {
      return;
    }
    if (!HEADLESS_RUN_GUARD.compareAndSet(false, true)) {
      LOGGER.warn("Headless playground already started once. Skipping duplicate trigger.");
      return;
    }
    runHeadlessSimulation();
  }

  private void runHeadlessSimulation() {
    try {
      Files.createDirectories(RESULTS_FILE.getParent());
      StringBuilder csvContent = new StringBuilder(buildCsvHeader());
      csvContent.append(System.lineSeparator());

      LOGGER.info("Headless playground started: {} games queued.", totalGames);
      int gameNumber = 1;
      for (Tactic tactic : PLAYGROUND_TACTICS) {
        gameNumber = runBatch(csvContent, tactic, gameNumber);
      }

      Files.writeString(
          RESULTS_FILE,
          csvContent.toString(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
      );
      LOGGER.info("Headless playground finished. Results saved to {}", RESULTS_FILE);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write playground CSV results", e);
    }
  }

  private int runBatch(StringBuilder csvContent, Tactic tactic, int gameStartNumber) {
    int currentGame = gameStartNumber;
    for (int i = 0; i < gamesPerTactic; i++) {
      GameSnapshot result = playSingleGame(tactic);
      csvContent.append(buildCsvRow(currentGame, tactic, result));
      csvContent.append(System.lineSeparator());
      LOGGER.info("Game {}/{} completed [{}].", currentGame, totalGames, tactic.name());
      currentGame++;
    }
    return currentGame;
  }

  private GameSnapshot playSingleGame(Tactic tactic) {
    List<Tactic> tactics = List.of(tactic, tactic, tactic);
    Map map = generatorService.generateNew(tactics);
    heuristicService.calculateHeuristic(map);

    Integer turn = 0;
    while (turn != null) {
      turn = gameService.play(map, turn);
      heuristicService.calculateHeuristic(map);
    }
    return buildGameSnapshot(map);
  }

  private String buildCsvHeader() {
    StringBuilder header = new StringBuilder("game,tactic");
    for (int i = 0; i < PLAYGROUND_TACTICS.size(); i++) {
      int playerIndex = i + 1;
      header.append(String.format(
          Locale.US,
          ",p%d_prod,p%d_div,p%d_scar,p%d_overall",
          playerIndex,
          playerIndex,
          playerIndex,
          playerIndex
      ));
    }
    for (Resource resource : Resource.values()) {
      header.append(",mapProd_").append(resource.name());
    }
    header.append(",winnerIndex");
    return header.toString();
  }

  private String buildCsvRow(int gameNumber, Tactic tactic, GameSnapshot snapshot) {
    StringBuilder row = new StringBuilder(String.format(Locale.US, "%d,%s", gameNumber, tactic.name()));
    for (ScoreSnapshot playerScore : snapshot.playerScores()) {
      row.append(String.format(
          Locale.US,
          ",%.2f,%.2f,%.2f,%.2f",
          playerScore.production(),
          playerScore.diversity(),
          playerScore.scarcity(),
          playerScore.total()
      ));
    }
    for (Resource resource : Resource.values()) {
      row.append(",").append(snapshot.mapProduction().getOrDefault(resource, 0));
    }
    row.append(",").append(resolveWinnerIndex(snapshot.playerScores()));
    return row.toString();
  }

  private int resolveWinnerIndex(List<ScoreSnapshot> playerScores) {
    int winnerIndex = 1;
    double bestOverall = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < playerScores.size(); i++) {
      double currentOverall = playerScores.get(i).total();
      if (currentOverall > bestOverall) {
        bestOverall = currentOverall;
        winnerIndex = i + 1;
      }
    }
    return winnerIndex;
  }

  private GameSnapshot buildGameSnapshot(Map map) {
    List<ScoreSnapshot> playerScores = map.getPlayers()
        .stream()
        .map(this::buildPlayerScoreSnapshot)
        .toList();
    EnumMap<Resource, Integer> production = new EnumMap<>(Resource.class);
    production.putAll(map.getProduction());
    return new GameSnapshot(playerScores, production);
  }

  private ScoreSnapshot buildPlayerScoreSnapshot(Player player) {
    return new ScoreSnapshot(
        player.getScore().getProductionScore(),
        player.getScore().getDiversityScore(),
        player.getScore().getScarcityScore(),
        player.getScore().getTotalScore()
    );
  }

  private record GameSnapshot(
      List<ScoreSnapshot> playerScores,
      EnumMap<Resource, Integer> mapProduction
  ) {}

  private record ScoreSnapshot(
      double production,
      double diversity,
      double scarcity,
      double total
  ) {}
}
