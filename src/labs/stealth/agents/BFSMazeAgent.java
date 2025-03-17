package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.HashMap;
import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;           // will need for bfs


// JAVA PROJECT IMPORTS


public class BFSMazeAgent
    extends MazeAgent
{

    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
		HashMap<Vertex, LinkedList<Vertex>> graph = new HashMap<>();

		int row_len = state.getXExtent();
		int col_len = state.getYExtent();
		int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1,1}, {-1,-1}, {1, -1}, {-1, 1}};

		for(int r = 0; r < row_len; r++){
			for(int c = 0; c < col_len; c++){
				if (state.resourceAt(r, c) == null) {
                    Vertex current = new Vertex(r, c);
                    graph.putIfAbsent(current, new LinkedList<>());
                    for (int[] dir : directions) {
                        int adj_r = r + dir[0];
                        int adj_c = c + dir[1];

                        if (state.inBounds(adj_r, adj_c) && state.resourceAt(adj_r, adj_c) == null) {
                            Vertex neighbor = new Vertex(adj_r, adj_c);
                            graph.putIfAbsent(neighbor, new LinkedList<>());
                            graph.get(current).add(neighbor);
                        }
                    }
                }
			}
		}

		// System.out.println("Graph adjacency list:");
		// System.out.println(graph.entrySet());
        // for (HashMap.Entry<Vertex, LinkedList<Vertex>> entry : graph.entrySet()) {
		// 	Vertex key = entry.getKey(); // The Vertex key
		// 	LinkedList<Vertex> value = entry.getValue(); // The LinkedList of Vertexes
		// 	// Do something with key and value
		// 	System.out.println(key);
		// 	System.out.println(value);
		// }

		
		Queue<Vertex> q = new LinkedList<>();
		q.add(src);
		HashSet<Vertex> visited = new HashSet<Vertex>();
		visited.add(src);
		
		HashMap<Vertex, Path> pathMap = new HashMap<>();
		pathMap.put(src, new Path(src));

		while (!q.isEmpty()){
			Vertex current = q.remove();
			if (current.equals(goal)){
				// need to process this correctly
				System.out.println(pathMap.get(goal));
				return pathMap.get(goal);
			}
			for(Vertex neighbor : graph.get(current)){
				if (!visited.contains(neighbor)){
					visited.add(neighbor);
					Path newPath = new Path(neighbor, 1f, pathMap.get(current));
					pathMap.put(neighbor, newPath);
					q.add(neighbor);
				}
			}
		}

        return null;
    }

}
