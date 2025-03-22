package src.pas.pokemon.agents;

// SYSTEM IMPORTS....feel free to add your own imports here! You may need/want to import more from the .jar!
import edu.bu.pas.pokemon.core.Agent;
import edu.bu.pas.pokemon.core.Battle;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Team;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.enums.Stat;
import edu.bu.pas.pokemon.core.Move;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.utils.Pair;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


// JAVA PROJECT IMPORTS


public class TreeTraversalAgent
    extends Agent
{
	private class MoveNode {
		private List<Pair<Double, BattleView>> resultsArray;
		private double avgUtility;
		private MoveView currMove;

		public MoveNode(BattleView state, MoveView currMove, int maxTeamIdx, int minTeamIdx){
			int myTeamIdx = getMyTeamIdx();
			
			this.resultsArray = currMove.getPotentialEffects(state, myTeamIdx, 1 - myTeamIdx);
			double util = 0;
			for (Pair<Double, BattleView> result : this.resultsArray){
				if(result.getSecond() != null){
					util += result.getFirst() * getBattleHeuristic(result.getSecond());
				}
			}
			this.avgUtility = util;
			this.currMove = currMove;

		}

		public double getAvgUtility(){ return avgUtility; }
		public List<Pair<Double, BattleView>> getResultsArray(){ return resultsArray; }
		public MoveView getCurrMove() { return this.currMove; }

		
	}

	private class BattleViewNode {
		private final BattleView state;

		public BattleViewNode(BattleView state){
			this.state = state;
		}

		public List<MoveNode> generateChildren(){
			if (this.state.isOver()){
				return new ArrayList<>();
			}
			

			List<MoveNode> resultsArray = new ArrayList<MoveNode>();
			// for every move, add to resultsArray
			Team.TeamView team1 = this.state.getTeam1View();
			Team.TeamView team2 = this.state.getTeam2View();
			int maxTeamIdx = team1.getActivePokemonIdx();
			PokemonView maxTeamPokemon = team1.getPokemonView(maxTeamIdx);
			int minTeamIdx = team2.getActivePokemonIdx();

			List<MoveView> possibleMoves = maxTeamPokemon.getAvailableMoves();
			System.out.println("generating possible moves: ");
			System.out.println("Team 1: " +maxTeamIdx +" | " + "Team 2: " +minTeamIdx);

			for(MoveView move : possibleMoves){
				resultsArray.add(new MoveNode(state, move, maxTeamIdx, minTeamIdx));
			}

			return resultsArray;
		}
	}

	private class StochasticTreeSearcher
        extends Object
        implements Callable<Pair<MoveView, Long> >  // so this object can be run in a background thread
	{

        // TODO: feel free to add any fields here! If you do, you should probably modify the constructor
        // of this class and add some getters for them. If the fields you add aren't final you should add setters too!
		private final BattleView rootView;
        private final int maxDepth;
        private final int myTeamIdx;

        // If you change the parameters of the constructor, you will also have to change
        // the getMove(...) method of TreeTraversalAgent!
		public StochasticTreeSearcher(BattleView rootView, int maxDepth, int myTeamIdx)
        {
            this.rootView = rootView;
            this.maxDepth = maxDepth;
            this.myTeamIdx = myTeamIdx;
        }

        // Getter methods. Since the default fields are declared final, we don't need setters
        // but if you make any fields that aren't final you should give them setters!
		public BattleView getRootView() { return this.rootView; }
        public int getMaxDepth() { return this.maxDepth; }
        public int getMyTeamIdx() { return this.myTeamIdx; }

		/**
		 * TODO: implement me!
		 * This method should perform your tree-search from the root of the entire tree.
         * You are welcome to add any extra parameters that you want! If you do, you will also have to change
         * The call method in this class!
		 * @param node the node to perform the search on (i.e. the root of the entire tree)
		 * @return The MoveView that your agent should execute
		 */
        public MoveView stochasticTreeSearch(BattleView rootView) //, int depth)
        {
			// should choose the heighest heurstic move and submit that
			BattleViewNode chanceNode = new BattleViewNode(rootView);
			List<MoveNode> nodeChildren = chanceNode.generateChildren();

			if (nodeChildren.isEmpty()) {
				return null;
			}

			// MoveView selectedMove = minimaxTreeSearch(chanceNode, 5);

			MoveNode selectedMove = nodeChildren.get(0);
			for (MoveNode currMove : nodeChildren){
				MoveView consideredMove = currMove.getCurrMove();
				System.out.println(consideredMove.getName() + " calculated utility: " + currMove.getAvgUtility());

				if (currMove.getAvgUtility() > selectedMove.getAvgUtility()){
					selectedMove = currMove;
				}
			}
			
			return selectedMove.getCurrMove();
        }


		// should return a list of pairs of doubles with moveviews
		// does this recursively
		private MoveView minimaxTreeSearch(BattleView rootView, int depth){
			if(depth == 0){
				return null;
			}

			BattleViewNode chanceNode = new BattleViewNode(rootView);
			List<MoveNode> nodeChildren = chanceNode.generateChildren();

			if (nodeChildren.isEmpty()) {
				return null;
			}

			MoveNode selectedMove = nodeChildren.get(3);
			return selectedMove.getCurrMove();
		}

        @Override
        public Pair<MoveView, Long> call() throws Exception
        {
            double startTime = System.nanoTime();

            MoveView move = this.stochasticTreeSearch(this.getRootView());
            double endTime = System.nanoTime();

            return new Pair<MoveView, Long>(move, (long)((endTime-startTime)/1000000));
        }
		
	}

	private final int maxDepth;
    private long maxThinkingTimePerMoveInMS;

	public TreeTraversalAgent()
    {
        super();
        this.maxThinkingTimePerMoveInMS = 180000 * 2; // 6 min/move
        this.maxDepth = 1000; // set this however you want
    }


    /**
     * Some constants
     */
    public int getMaxDepth() { return this.maxDepth; }
    public long getMaxThinkingTimePerMoveInMS() { return this.maxThinkingTimePerMoveInMS; }


    public Double getBattleHeuristic(BattleView view)
	{
		TeamView myTeamView = this.getMyTeamView(view);
		TeamView opponentTeamView = this.getOpponentTeamView(view);
		Double finalHeuristic = 0.0;
		
		double myTotalHP = 0, opponentTotalHP = 0;
		
		for (int i = 0; i < myTeamView.size(); ++i){
			PokemonView currPokemon = myTeamView.getPokemonView(i);
			if(currPokemon.hasFainted()){
				finalHeuristic -= 50;
			}
		}

		for (int j = 0; j < opponentTeamView.size(); ++j){
			PokemonView currPokemon = opponentTeamView.getPokemonView(j);
			opponentTotalHP += currPokemon.getCurrentStat(Stat.HP);
			if(currPokemon.hasFainted()){
				finalHeuristic += 50; 
			}
		}


		// double damageDealt = myTotalHP - opponentTotalHP;
		finalHeuristic -= opponentTotalHP; 

		return finalHeuristic;
	}

    @Override
    public Integer chooseNextPokemon(BattleView view)
    {
        // TODO: replace me! This code calculates the first-available pokemon.
        // It is likely a good idea to expand a bunch of trees with different choices as the active pokemon on your
        // team, and see which pokemon is your best choice by comparing the values of the root nodes.
		Double maxUtility = Double.NEGATIVE_INFINITY;
		int maxUtilityIdx = -1;
		TeamView opponentTeamView = this.getOpponentTeamView(view);
		PokemonView opponentActivePokemonView = opponentTeamView.getActivePokemonView();
        for(int idx = 0; idx < this.getMyTeamView(view).size(); ++idx)
        {
			PokemonView curPokemon = this.getMyTeamView(view).getPokemonView(idx);
            if(!curPokemon.hasFainted())
            {
				Double avgUtility = 0.0;
				List<MoveView> availableMoves = curPokemon.getAvailableMoves();
				int numCalculatedMoves = 0;
				System.out.println(curPokemon.getName() +" has " +availableMoves.size() +" available moves");
				for (MoveView move : availableMoves){
					if (move.getName().equals("Pin Missile")) {
						continue;
					}
					double util = 0;
					for (Pair<Double, BattleView> result : move.getPotentialEffects(view, getMyTeamIdx(), 1 - getMyTeamIdx())){
						if(result.getSecond() != null){
							util += result.getFirst() * getBattleHeuristic(result.getSecond());
						}
					}
					avgUtility += util;
					numCalculatedMoves += 1;
				}
				avgUtility /= numCalculatedMoves;
				System.out.println("Average Utility for " +curPokemon.getName() +": "+avgUtility);
				if (avgUtility > maxUtility) {
					maxUtility = avgUtility;
					maxUtilityIdx = idx;
				}
            }
        }
		System.out.println(this.getMyTeamView(view).getPokemonView(maxUtilityIdx).getName() + " chosen");
        return maxUtilityIdx;
    }

    /**
     * This method is responsible for getting a move selected via the minimax algorithm.
     * There is some setup for this to work, namely making sure the agent doesn't run out of time.
     * Please do not modify.
     */
    @Override
    public MoveView getMove(BattleView battleView)
    {

        // will run the minimax algorithm in a background thread with a timeout
        ExecutorService backgroundThreadManager = Executors.newSingleThreadExecutor();

        // preallocate so we don't spend precious time doing it when we are recording duration
        MoveView move = null;
        long durationInMs = 0;

        // this obj will run in the background
        StochasticTreeSearcher searcherObject = new StochasticTreeSearcher(
            battleView,
            this.getMaxDepth(),
            this.getMyTeamIdx()
        );

        // submit the job
        Future<Pair<MoveView, Long> > future = backgroundThreadManager.submit(searcherObject);

        try
        {
            // set the timeout
            Pair<MoveView, Long> moveAndDuration = future.get(
                this.getMaxThinkingTimePerMoveInMS(),
                TimeUnit.MILLISECONDS
            );

            // if we get here the move was chosen quick enough! :)
            move = moveAndDuration.getFirst();
            durationInMs = moveAndDuration.getSecond();

            // convert the move into a text form (algebraic notation) and stream it somewhere
            // Streamer.getStreamer(this.getFilePath()).streamMove(move, Planner.getPlanner().getGame());
        } catch(TimeoutException e)
        {
            // timeout = out of time...you lose!
            System.err.println("Timeout!");
            System.err.println("Team [" + (this.getMyTeamIdx()+1) + " loses!");
            System.exit(-1);
        } catch(InterruptedException e)
        {
            e.printStackTrace();
            System.exit(-1);
        } catch(ExecutionException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return move;
    }
}
