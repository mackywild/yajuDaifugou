import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.cpu.CpuFeatureExtractor;
import com.example.daifugo.game.cpu.CpuMove;
import com.example.daifugo.game.cpu.CpuStrategy;
import com.example.daifugo.game.cpu.HardCpuStrategy;
import com.example.daifugo.game.cpu.LegalMoveGenerator;
import com.example.daifugo.game.cpu.NormalCpuStrategy;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.PendingSevenTransfer;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.GameEngineFactory;
import com.example.daifugo.game.service.GameInitializer;
import com.example.daifugo.game.service.YajuRuleService;

/**
 * N-GOD学習用の自己対戦データ生成器。
 * Hard/Normalに探索ノイズを加えた自己対戦からstate-action特徴量と最終順位報酬を出力する。
 */
public class SelfPlayDataGenerator {
    private record Step(String playerId, double[] features) {}

    public static void main(String[] args) throws Exception {
        int games = args.length >= 1 ? Integer.parseInt(args[0]) : 5000;
        Path output = Path.of(args.length >= 2 ? args[1] : "tools/ml/ngod_selfplay.csv");
        Random random = new Random(8101919L);
        LegalMoveGenerator generator = new LegalMoveGenerator();
        GameEngineFactory factory = new GameEngineFactory();
        YajuRuleService yaju = new YajuRuleService();
        CpuStrategy hard = new HardCpuStrategy();
        CpuStrategy normal = new NormalCpuStrategy();

        Files.createDirectories(output.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("reward");
            for (int i = 0; i < CpuFeatureExtractor.FEATURE_COUNT; i++) {
                writer.write(",f" + i);
            }
            writer.newLine();

            long rows = 0;
            for (int game = 0; game < games; game++) {
                int playerCount = 2 + random.nextInt(7);
                GameRuleSettings settings = randomSettings(random);
                List<Player> players = new ArrayList<>();
                for (int i = 0; i < playerCount; i++) {
                    players.add(new Player(UUID.randomUUID().toString(), "P" + i));
                }
                GameState state = new GameState(players);
                new GameInitializer(settings.jokerCount()).initialize(state);
                if (settings.yajuRule()) yaju.initializeTargets(state);
                GameEngine engine = factory.create(settings);
                List<Step> steps = new ArrayList<>();

                int safety = 0;
                while (!state.isFinished()) {
                    if (++safety > 2000) throw new IllegalStateException("self-play deadlock game=" + game);
                    Player current = state.getCurrentPlayer();
                    if (state.hasPendingSevenTransfer()) {
                        PendingSevenTransfer pending = state.getPendingSevenTransfer();
                        List<List<Card>> transfers = generator.generateSevenTransfers(current, pending.cardCount(), settings);
                        List<Card> selected = hard.chooseSevenTransfer(state, current, transfers, settings);
                        engine.transferSeven(state, current.getId(), selected);
                        continue;
                    }

                    List<CpuMove> moves = generator.generate(state, current, settings);
                    if (moves.isEmpty()) { System.err.println("NO LEGAL game="+game+" player="+current.getName()+" hand="+current.getHand()+" yaju="+current.getYajuStatus()+" field="+state.getFieldCombination()+" locked="+state.getLockedMark()+" revolution="+state.isRevolution()+" settings="+settings); throw new IllegalStateException("no legal moves"); }
                    CpuMove chosen;
                    double explore = 0.28;
                    if (random.nextDouble() < explore) {
                        chosen = moves.get(random.nextInt(moves.size()));
                    } else {
                        chosen = (random.nextBoolean() ? hard : normal).chooseMove(state, current, moves, settings);
                    }
                    steps.add(new Step(current.getId(), CpuFeatureExtractor.extract(state, current, chosen, settings)));
                    if (chosen.pass()) engine.pass(state, current.getId());
                    else engine.play(state, current.getId(), chosen.cards());
                }

                Map<String, Double> reward = new HashMap<>();
                for (Player p : state.getPlayers()) {
                    int rank = p.getRank();
                    double r = playerCount == 1 ? 1.0 : (playerCount - rank) / (double)(playerCount - 1);
                    reward.put(p.getId(), r);
                }
                for (Step step : steps) {
                    writer.write(Double.toString(reward.get(step.playerId())));
                    for (double f : step.features()) writer.write("," + f);
                    writer.newLine();
                    rows++;
                }
                if ((game + 1) % 500 == 0) {
                    System.out.println("games=" + (game + 1) + " rows=" + rows);
                }
            }
            System.out.println("DONE games=" + games + " rows=" + rows + " -> " + output);
        }
    }

    private static GameRuleSettings randomSettings(Random r) {
        int joker = r.nextInt(3);
        boolean eightCut = r.nextDouble() < 0.85;
        boolean yaju = eightCut && r.nextDouble() < 0.75;
        return new GameRuleSettings(
                joker,
                r.nextDouble() < 0.80,
                eightCut,
                r.nextDouble() < 0.75,
                r.nextDouble() < 0.80,
                yaju,
                r.nextDouble() < 0.80,
                r.nextDouble() < 0.90
        );
    }}
