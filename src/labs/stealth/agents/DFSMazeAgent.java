package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(int playerNum)
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
		// // System.out.println(graph.entrySet());
        // for (HashMap.Entry<Vertex, LinkedList<Vertex>> entry : graph.entrySet()) {
		// 	Vertex key = entry.getKey(); // The Vertex key
		// 	LinkedList<Vertex> value = entry.getValue(); // The LinkedList of Vertexes
		// 	// Do something with key and value
		// 	System.out.println(key);
		// 	System.out.println(value);
		// }

		Stack<Vertex> s = new Stack<>();
		s.add(src);
		HashSet<Vertex> visited = new HashSet<Vertex>();
		// visited.add(src);
		
		HashMap<Vertex, Path> pathMap = new HashMap<>();
		pathMap.put(src, new Path(src));
		// Path start = new Path(src);

		while (!s.isEmpty()){
			Vertex current = s.pop();
			if (current.equals(goal)){
				// need to process this correctly
				return pathMap.get(goal);
			}
			if(!visited.contains(current)){
				visited.add(current);
				// for (V : graph.get(current))
				for (Vertex neighbor : graph.getOrDefault(current, new LinkedList<>())) {
					System.out.println(neighbor);
					if(!visited.contains(neighbor)){
						Path newPath = new Path(neighbor, 1f, pathMap.get(current));
						pathMap.put(neighbor, newPath);
						s.push(neighbor);
					}
				}
			}
		}

        return pathMap.get(goal);
    }

}
