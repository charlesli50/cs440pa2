package src.pas.stealth.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;


// import java.io.InputStream;
// import java.io.OutputStream;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

// JAVA PROJECT IMPORTS
import edu.bu.pas.stealth.agents.AStarAgent;                // the base class of your class
import edu.bu.pas.stealth.distance.DistanceMetric;
// import edu.bu.pas.stealth.agents.AStarAgent.AgentPhase;     // INFILTRATE/EXFILTRATE enums for your state machine
// import edu.bu.pas.stealth.agents.AStarAgent.ExtraParams;    // base class for creating your own params objects
import edu.bu.pas.stealth.graph.Vertex;                     // Vertex = coordinate
import edu.bu.pas.stealth.graph.Path;                       // see the documentation...a Path is a linked list



public class StealthAgent
    extends AStarAgent
{

    // Fields of this class
    // TODO: add your fields here! For instance, it might be a good idea to
    // know when you've killed the enemy townhall so you know when to escape!
    // TODO: implement the state machine for following a path once we calculate it
    //       this will for sure adding your own fields.
    private int enemyChebyshevSightLimit;
	private float[][] currEdgeWeights;

	public int myUnitId;
	public int townhallId;
	public Vertex homeVertex;

	private Stack<Vertex> currentPlan;

	private enum Goal{
		GOLD,
		TOWNHALL,
		RUN
	}

	private Goal currentGoal;

	public static int[][] directions = {{-1,0}, {1,0}, {0,1}, {0,-1}, {-1, 1}, {1, 1}, {-1, -1}, {1, -1}};
    
    public StealthAgent(int playerNum)
    {
        super(playerNum);

        this.enemyChebyshevSightLimit = -1; // invalid value....we won't know this until initialStep()

    }

    // TODO: add some getter methods for your fields! Thats the java way to do things!
    public final int getEnemyChebyshevSightLimit() { return this.enemyChebyshevSightLimit; }

    public void setEnemyChebyshevSightLimit(int i) { this.enemyChebyshevSightLimit = i; }

	public Goal getCurrentGoal(){ return this.currentGoal; }
	public void setCurrentGoal(Goal newGoal) { this.currentGoal = newGoal; }

	public Vertex popFromCurrentPlan() { return this.currentPlan.pop(); }
	public void setCurrentPlan(Stack<Vertex> new_path) { this.currentPlan = new_path;}
	public boolean planFinished(){ return this.currentPlan.isEmpty(); }

	public final float[][] getCurrEdgeWeights() {return this.currEdgeWeights; };

	public void setCurrEdgeWeights(StateView state){
		int x_len = state.getXExtent();
		int y_len = state.getYExtent();
		float[][] tempEdgeWeights = new float[x_len][y_len];
		for(int r = 0; r < x_len; r++){
			for(int c = 0; c < y_len; c++){
				tempEdgeWeights[r][c] = 1f;
			}
		}

		UnitView otherEnemyUnitView = null;
        Iterator<Integer> otherEnemyUnitIDsIt = this.getOtherEnemyUnitIDs().iterator();
        while(otherEnemyUnitIDsIt.hasNext() && otherEnemyUnitView == null)
        {
            otherEnemyUnitView = state.getUnit(otherEnemyUnitIDsIt.next());
			int enemy_x_pos = otherEnemyUnitView.getXPosition();
			int enemy_y_pos = otherEnemyUnitView.getYPosition();
			int enemy_sight = this.enemyChebyshevSightLimit;

			for (int i = 0; i < x_len; i++) {
				for (int j = 0; j < y_len; j++) {
					int distance = Math.max(Math.abs(i - enemy_x_pos), Math.abs(j - enemy_y_pos));
					
					if (distance <= enemy_sight + 1) {
						tempEdgeWeights[i][j] += 10;
					} else if (distance <= (enemy_sight + 1)*2) {
						tempEdgeWeights[i][j] += 5;
					}
				}
			}
        }

		// for (float[] arr : tempEdgeWeights){
		// 	System.out.println(Arrays.toString(arr));
		// }

		this.currEdgeWeights = tempEdgeWeights;

	}

    ///////////////////////////////////////// Sepia methods to override ///////////////////////////////////

    /**
        TODO: if you add any fields to this class it might be a good idea to initialize them here
              if they need sepia information!
     */
    @Override
    public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
        super.initialStep(state, history); // call AStarAgent's initialStep() to set helpful fields and stuff

        // now some fields are set for us b/c we called AStarAgent's initialStep()
        // let's calculate how far away enemy units can see us...this will be the same for all units (except the base)
        // which doesn't have a sight limit (nor does it care about seeing you)
        // iterate over the "other" (i.e. not the base) enemy units until we get a UnitView that is not null
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
        UnitView otherEnemyUnitView = null;
        Iterator<Integer> otherEnemyUnitIDsIt = this.getOtherEnemyUnitIDs().iterator();
        while(otherEnemyUnitIDsIt.hasNext() && otherEnemyUnitView == null)
        {
            otherEnemyUnitView = state.getUnit(otherEnemyUnitIDsIt.next());
        }

        if(otherEnemyUnitView == null)
        {
            System.err.println("[ERROR] StealthAgent.initialStep: could not find a non-null 'other' enemy UnitView??");
            System.exit(-1);
        }

        // lookup an attribute from the unit's "template" (which you can find in the map .xml files)
        // When I specify the unit's (i.e. "footman"'s) xml template, I will use the "range" attribute
        // as the enemy sight limit

		Set<Integer> myUnitIds = new HashSet<Integer>();
		Set<Integer> townHallIds = new HashSet<Integer>();
		for(Integer unitID : state.getUnitIds(this.getPlayerNumber())) // for each unit on my team
        {
            myUnitIds.add(unitID);
        }
		for(Integer unitID : state.getUnitIds(1 - this.getPlayerNumber()))
        {
            UnitView unit = state.getUnit(unitID);
			String unitTypeName = unit.getTemplateView().getName();
			if(unitTypeName.equals("TownHall")) townHallIds.add(unitID);
        }

		this.setCurrentGoal(Goal.GOLD);
		

		this.myUnitId = myUnitIds.iterator().next();
		this.townhallId = townHallIds.iterator().next();
        this.setEnemyChebyshevSightLimit(otherEnemyUnitView.getTemplateView().getRange());
		this.setCurrEdgeWeights(state);

		UnitView myUnit = state.getUnit(myUnitId);
		Vertex src = new Vertex(myUnit.getXPosition(), myUnit.getYPosition());
		this.homeVertex = src;

		UnitView townhallUnit = state.getUnit(townhallId);
		Vertex dst = new Vertex(townhallUnit.getXPosition(), townhallUnit.getYPosition());


		// logic to get new path
		Path new_path = aStarSearch(src, dst, state, null);

		Stack<Vertex> steps_stack;
		for(steps_stack = new Stack<Vertex>(); new_path.getParentPath() != null; new_path = new_path.getParentPath()){
			steps_stack.add(new_path.getDestination());
		}

		this.setCurrentPlan(steps_stack);
		Vertex next_move = this.popFromCurrentPlan();
	
		// UnitView myUnit = state.getUnit(myUnitId);
		int x1 = myUnit.getXPosition(); int y1 = myUnit.getYPosition();
		actions.put(myUnitId, Action.createPrimitiveMove(myUnitId, this.getDirectionToMoveTo(new Vertex(x1, y1), next_move)));

        return actions;
    }

    /**
        TODO: implement me! This is the method that will be called every turn of the game.
              This method is responsible for assigning actions to all units that you control
              (which should only be a single footman in this game)
     */
    @Override
    public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

	
		// later to be shouldReplacePlan
		if(this.shouldReplacePlan(state, null)){
			System.out.println("oopsies! recalculating, but not now :)");
		} else if(this.planFinished() && state.getUnit(townhallId) != null) {
			actions.put(myUnitId, Action.createPrimitiveAttack(myUnitId, townhallId));

		} else if(this.planFinished()){
			UnitView myUnit = state.getUnit(myUnitId);
			Vertex src = new Vertex(myUnit.getXPosition(), myUnit.getYPosition());

			Path new_path = aStarSearch(src, homeVertex, state, null);

			Stack<Vertex> steps_stack;
			for(steps_stack = new Stack<Vertex>(); new_path.getParentPath() != null; new_path = new_path.getParentPath()){
				steps_stack.add(new_path.getDestination());
			}
	
			this.setCurrentPlan(steps_stack);
		} else{
			// you should make your move
			
			Vertex next_move = this.popFromCurrentPlan();
	
			UnitView myUnit = state.getUnit(myUnitId);
			int x1 = myUnit.getXPosition(); int y1 = myUnit.getYPosition();
	
			actions.put(myUnitId, Action.createPrimitiveMove(myUnitId, this.getDirectionToMoveTo(new Vertex(x1, y1), next_move)));
		}


        /**
            I would suggest implementing a state machine here to calculate a path when neccessary.
            For instance beginning with something like:

            if(this.shouldReplacePlan(state))
            {
                // recalculate the plan
            }

            then after this, worry about how you will follow this path by submitting sepia actions
            the trouble is that we don't want to move on from a point on the path until we reach it
            so be sure to take that into account in your design

            once you have this working I would worry about trying to detect when you kill the townhall
            so that you implement escaping
         */

        return actions;
    }

    ////////////////////////////////// End of Sepia methods to override //////////////////////////////////


    /////////////////////////////////// AStarAgent methods to override ///////////////////////////////////

    public LinkedList<Vertex> getNeighbors(Vertex v,
                                           StateView state,
                                           ExtraParams extraParams)
    {
		LinkedList<Vertex> result = new LinkedList<>();
		int v_x = v.getXCoordinate();
		int v_y = v.getYCoordinate();
		for (int i = 0; i < 8; i++) {
			int[] dir = StealthAgent.directions[i];
			int adj_r = v_x + dir[0];
			int adj_c = v_y + dir[1];
			if(state.inBounds(adj_r, adj_c) && !state.isResourceAt(adj_r, adj_c)){
				result.add(new Vertex(adj_r, adj_c));
			}
		}

		return result;

    }

    public Path aStarSearch(Vertex src,
                            Vertex dst,
                            StateView state,
                            ExtraParams extraParams)
    {
		HashMap<Path, Float> dist = new HashMap<>();
		HashSet<Vertex> visited = new HashSet<>();
		PriorityQueue<Path> pq = new PriorityQueue<>(new Comparator<Path>() {
            @Override
            public int compare(Path path1, Path path2) {
                return Float.compare(path1.getTrueCost() + path1.getEstimatedPathCostToGoal(), path2.getTrueCost() + path2.getEstimatedPathCostToGoal());
            }
        });
		Path init = new Path(src);
		dist.put(init, 0f);
		pq.add(init);

		while (!pq.isEmpty()){
			Path u = pq.poll();
			// get the path from u to current vertex
			Vertex curr_node = u.getDestination();
			if (visited.contains(curr_node)) {
                continue;
            }
            visited.add(curr_node);

			if (curr_node.equals(dst)) {
				System.out.print("Current cost to Townhall: ");
				System.out.println(u.getTrueCost());
                return u;
            }

			LinkedList<Vertex> neighbors = this.getNeighbors(curr_node, state, null);
			for(Vertex neighbor : neighbors){
				Path newPath = new Path(neighbor, this.getEdgeWeight(curr_node, neighbor, state, null), DistanceMetric.chebyshevDistance(neighbor, dst), u);
				float newDistance = newPath.getTrueCost();

				if (!dist.containsKey(newPath) || newDistance < dist.get(newPath)) {
                    pq.add(newPath);
                    dist.put(newPath, newDistance);
                }
			}

		}
		
		return null;
    }

    public float getEdgeWeight(Vertex src,
                               Vertex dst,
                               StateView state,
                               ExtraParams extraParams)
    {
        int dst_x = dst.getXCoordinate();
		int dst_y = dst.getYCoordinate();

		return this.getCurrEdgeWeights()[dst_x][dst_y];
    }

    public boolean shouldReplacePlan(StateView state,
                                     ExtraParams extraParams)
    {
        UnitView myUnit = state.getUnit(myUnitId);
        int x1 = myUnit.getXPosition(), y1 = myUnit.getYPosition();

		for (Integer enemyID : this.getOtherEnemyUnitIDs()) {
            UnitView enemy = state.getUnit(enemyID);
            if (enemy != null) {
                int x2 = enemy.getXPosition(), y2 = enemy.getYPosition();
				int currChevDist = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));;
				if (currChevDist < this.getEnemyChebyshevSightLimit() + 3){
					return true;
				}
            }
        }
		
		
		return false;
    }

    //////////////////////////////// End of AStarAgent methods to override ///////////////////////////////

}

