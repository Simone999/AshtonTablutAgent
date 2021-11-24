package it.ai;

import it.ai.agents.Agent;
import it.ai.agents.MctsAgent;
import it.ai.client.TablutClient;
import it.ai.game.Action;
import it.ai.game.Game;
import it.ai.game.tablut.ashton.AshtonTablutGame;
import it.ai.montecarlo.AbstractMCTS;
import it.ai.montecarlo.MCTS;
import it.ai.montecarlo.MCTSImpl;
import it.ai.montecarlo.NeuralNetworkMonteCarlo;
import it.ai.montecarlo.strategies.bestaction.MaxChildStrategy;
import it.ai.montecarlo.strategies.score.Ucb1SelectionScoreStrategy;
import it.ai.montecarlo.strategies.winscore.DefaultWinScoreStrategy;
import it.ai.montecarlo.termination.TimeoutTerminationCondition;
import it.ai.neuralnetworks.ValueNeuralNetwork;
import it.ai.players.AgentPlayer;
import it.ai.protocol.State;
import it.ai.protocol.Turn;
import it.ai.util.AshtonMapper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

public class Main {
//    private static boolean check(State state, Action action, int pawn){
//        Board board = ((TablutState)state).getBoard();
//        return checkBoard(board) && checkMovedPawn(board, action,pawn);
//    }
//    private static boolean checkMovedPawn(Board board, Action action, int pawn) {
//        int from = board.get(action.getFrom());
//        return (from == Pawn.EMPTY || from == Pawn.THRONE) &&  Pawn.getOwner(board.get(action.getTo())) == pawn;
//    }
//
//    private static boolean checkBoard(Board board) {
//        if (isDifferent(board, Pawn.BLACK)) return false;
//        if (isDifferent(board, Pawn.WHITE)) return false;
//        if (isDifferent(board, Pawn.KING)) return false;
//        return true;
//    }
//
//    private static boolean isDifferent(Board board, int pawn) {
//        for (Coords coords : board.getPawnCoords(pawn)) {
//            if (board.get(coords) != pawn)
//                return true;
//        }
//        return false;
//    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(Main.class.getName());
        String blackNN = "value_model_b_1.h5";
        String whiteNN = "value_model_b_1.h5";
        String playerName = "AI";
        String playerTeam = Turn.BLACK;

        double exploration = 2;
        double networkThreshold = 0.6;

        configureLogger();

        Game game = new AshtonTablutGame(0);

        AbstractMCTS mctsImpl = new MCTSImpl(game,
                new Ucb1SelectionScoreStrategy(exploration),
                new MaxChildStrategy(),
                new DefaultWinScoreStrategy());

        MCTS mcts = new NeuralNetworkMonteCarlo(mctsImpl,
                new ValueNeuralNetwork(blackNN), new ValueNeuralNetwork(whiteNN), networkThreshold);

//            MCTS mcts = new MCTSRootParallelization(() -> new MCTSImpl(game,
//                    new Ucb1SelectionScoreStrategy(2),
//                    new RobustChildStrategy(),
//                    new DefaultWinScoreStrategy()), 4);

        Agent agent = new MctsAgent(game, mcts, () -> new TimeoutTerminationCondition(50));
        Player player = new AgentPlayer(playerName, playerTeam, agent);
//            Action bestAction = player.getAction(game.start());
//            Logger.getLogger(Main.class.getName()).info(bestAction.toString());
//            Player player = new RandomPlayer(playerTeam);

        Mapper mapper = new AshtonMapper();
        TablutClient client = new TablutClient(player, mapper, "127.0.0.1");

        try {
            client.run();

            State state = client.getState();
            String winner = state.getTurn();
            logger.info("Winner: " + winner);
            state.setTurn(playerTeam);
            it.ai.game.State gameState = mapper.mapToGameState(state);
            agent.updateStateWithOpponentAction(gameState);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An exception was thrown", e);
        } finally {
            Iterable<Action> actions = agent.getActions();
            logger.info("Action history:\n" + actions);
        }
    }

    private static void configureLogger() throws IOException {
        String logFolder = "logs";
        ensureDirectoryExists(logFolder);

        Logger logger = Logger.getLogger("");
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        logger.setLevel(Level.ALL);

        String filename = logFolder + File.separator + new Date().getTime() + ".log";
        FileHandler fileHandler = new FileHandler(filename);
        fileHandler.setLevel(Level.FINE);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
    }

    private static void ensureDirectoryExists(String dir) {
        File directory = new File(dir);
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
    }
}
