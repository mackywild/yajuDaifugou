import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.cpu.*;
import com.example.daifugo.game.domain.*;
import com.example.daifugo.game.service.*;

/** N-GODをHard CPU 3体と対戦させる簡易ベンチマーク。 */
public class CpuBenchmark {
    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        benchmark("EASY", new EasyCpuStrategy(), games);
        benchmark("NORMAL", new NormalCpuStrategy(), games);
        benchmark("HARD", new HardCpuStrategy(), games);
        benchmark("N-GOD", new NgodCpuStrategy(), games);
    }

    static void benchmark(String name, CpuStrategy challenger, int games) {
        CpuStrategy hard = new HardCpuStrategy();
        LegalMoveGenerator generator = new LegalMoveGenerator();
        GameEngineFactory factory = new GameEngineFactory();
        YajuRuleService yaju = new YajuRuleService();
        GameRuleSettings settings = GameRuleSettings.standard();
        int wins=0; double rankSum=0; int dead=0;
        for (int g=0; g<games; g++) {
            int challengerSeat = g % 4;
            List<Player> players = new ArrayList<>();
            Map<String,CpuStrategy> strategy = new HashMap<>();
            String challengerId=null;
            for (int i=0;i<4;i++) {
                Player p=new Player(UUID.randomUUID().toString(), "P"+i);
                players.add(p);
                if(i==challengerSeat){ strategy.put(p.getId(),challenger); challengerId=p.getId(); }
                else strategy.put(p.getId(),hard);
            }
            GameState state=new GameState(players);
            new GameInitializer(settings.jokerCount()).initialize(state);
            yaju.initializeTargets(state);
            GameEngine engine=factory.create(settings);
            int steps=0;
            while(!state.isFinished() && steps++<3000){
                Player cur=state.getCurrentPlayer();
                CpuStrategy st=strategy.get(cur.getId());
                if(state.hasPendingSevenTransfer()){
                    PendingSevenTransfer p=state.getPendingSevenTransfer();
                    List<List<Card>> opts=generator.generateSevenTransfers(cur,p.cardCount(),settings);
                    engine.transferSeven(state,cur.getId(),st.chooseSevenTransfer(state,cur,opts,settings));
                } else {
                    List<CpuMove> moves=generator.generate(state,cur,settings);
                    CpuMove m=st.chooseMove(state,cur,moves,settings);
                    if(m.pass()) engine.pass(state,cur.getId()); else engine.play(state,cur.getId(),m.cards());
                }
            }
            if(!state.isFinished()){dead++; continue;}
            String targetId = challengerId;
            Player cp=state.getPlayers().stream().filter(p->p.getId().equals(targetId)).findFirst().orElseThrow();
            rankSum += cp.getRank(); if(cp.getRank()==1) wins++;
        }
        System.out.printf("%s games=%d winRate=%.3f avgRank=%.3f dead=%d%n",name,games,wins/(double)(games-dead),rankSum/(games-dead),dead);
    }
}
