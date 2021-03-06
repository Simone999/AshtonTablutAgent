package it.ai.montecarlo.heuristics.white;

import it.ai.game.State;
import it.ai.game.tablut.TablutState;
import it.ai.montecarlo.heuristics.HeuristicEvaluation;
import it.ai.montecarlo.heuristics.HeuristicUtils;

public class KingEscapes implements HeuristicEvaluation {
    @Override
    public double evaluate(State s, int player) {
        TablutState state = (TablutState) s;

        return HeuristicUtils.countKingEscapes(state) / 4.0;
    }
}
