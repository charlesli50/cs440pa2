package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;                           // Directions in Sepia


import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue; // heap in java
import java.util.Set;


// JAVA PROJECT IMPORTS


public class DijkstraMazeAgent
    extends MazeAgent
{

    public DijkstraMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
		HashMap<Path, Float> dist = new HashMap<>();
		HashSet<Vertex> visited = new HashSet<>();
		

		PriorityQueue<Path> pq = new PriorityQueue<>(Comparator.comparing(Path::getTrueCost));
		Path init = new Path(src);
		dist.put(init, 0f);
		pq.add(init);

		int[][] directions = {{-1,0}, {1,0}, {0,1}, {0,-1}, {-1, 1}, {1, 1}, {-1, -1}, {1, -1}};
		HashMap<Direction, Float> directionWeightMap = new HashMap<>();
        directionWeightMap.put(Direction.EAST, 5f);
        directionWeightMap.put(Direction.WEST, 5f);
        directionWeightMap.put(Direction.NORTH, 10f);
        directionWeightMap.put(Direction.SOUTH, 1f);
        directionWeightMap.put(Direction.NORTHWEST, (float) Math.sqrt(Math.pow(10, 2) + Math.pow(5, 2)));
        directionWeightMap.put(Direction.NORTHEAST, (float) Math.sqrt(Math.pow(10, 2) + Math.pow(5, 2)));
        directionWeightMap.put(Direction.SOUTHWEST, (float) Math.sqrt(Math.pow(1, 2) + Math.pow(5, 2)));
        directionWeightMap.put(Direction.SOUTHEAST, (float) Math.sqrt(Math.pow(1, 2) + Math.pow(5, 2)));
		
		while (!pq.isEmpty()){
			Path u = pq.poll();
			// get the path from u to current vertex
			Vertex curr_node = u.getDestination();
			if (visited.contains(curr_node)) {
                continue;
            }
            visited.add(curr_node);

			if (curr_node.equals(goal)) {
				System.out.println(u.getTrueCost());
                return u;
            }

			// for edge of curr_node
			for (int i = 0; i < 8; i++) {
				int[] dir = directions[i];
				int adj_r = curr_node.getXCoordinate() + dir[0];
				int adj_c = curr_node.getYCoordinate() + dir[1];
				Vertex neighbor = new Vertex(adj_r, adj_c);

				if (!state.inBounds(adj_r, adj_c) || state.isResourceAt(adj_r, adj_c) || visited.contains(neighbor)) {
                    continue;
                }
				Direction direction = this.getDirectionToMoveTo(curr_node, neighbor);
				Path newPath = new Path(neighbor, directionWeightMap.get(direction), u);
				
				float newDistance = newPath.getTrueCost();

                if (!dist.containsKey(newPath) || newDistance < dist.get(newPath)) {
                    pq.add(newPath);
                    dist.put(newPath, newDistance);
                }
			}

		}

        return null;
    }

}
