package MapTools;

import bc.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Pathing {
	private static int[][] blankBFS;
	private static PlanetMap map;
	public static Queue<Unit> worker, factory, rocket;

	public static void setup(GameController gc){
		//for BFS
		map = gc.startingMap(gc.planet());
		int h = (int)map.getHeight();
		int w = (int)map.getWidth();
		blankBFS = new int[h][w];
		for(int y = 0; y < h; y++){
			for(int x = 0; x < w; x++) {
				if(Passable.matrix(gc.planet())[y][x] == 0)
					blankBFS[y][x] = 9999;
				else{
					blankBFS[y][x] = -1;
				}
			}
		}
	}
	public static void update(Queue<Unit> work, Queue<Unit> fact, Queue<Unit> rock){
		worker = work;
		factory = fact;
		rocket = rock;
	}
	public static int[][] BFS(ArrayList<MapLocation> destinations, boolean structures){
		Queue<Integer> xq = new LinkedList<>();
		Queue<Integer> yq = new LinkedList<>();
		int h = (int) map.getHeight();
		int w = (int) map.getWidth();
		int[][] out = Utils.Misc.cloneMat(blankBFS);
		//add factories, rockets, (workers for now)
		if(structures){
			for(Unit wo : worker){
				Location loc = wo.location();
				if(!loc.isOnMap()) continue;
				MapLocation mapLoc = loc.mapLocation();
				out[mapLoc.getY()][mapLoc.getX()] = 9999;
			}
			for(Unit f : factory){
				MapLocation mapLoc = f.location().mapLocation();
				out[mapLoc.getY()][mapLoc.getX()] = 9999;
			}
			for(Unit r : rocket){
				Location loc = r.location();
				if(!loc.isOnMap()) continue;
				MapLocation mapLoc = loc.mapLocation();
				out[mapLoc.getY()][mapLoc.getX()] = 9999;
			}
		}
		//add initial locations
		for(MapLocation mapLoc : destinations){
			int x = mapLoc.getX();
			int y = mapLoc.getY();
			out[y][x] = 0;
			xq.add(x);
			yq.add(y);
		}
		//BFS
		while(!xq.isEmpty()){
			int cx = xq.remove();
			int cy = yq.remove();
			for(int dy = -1; dy <= 1; dy++) {
				int y = cy+dy;
				if(y < 0 || y >= h) continue;
				for (int dx = -1; dx <= 1; dx++){
					int x = cx+dx;
					if(x < 0 || x >= w) continue;
					if(out[y][x] == -1){
						out[y][x] = out[cy][cx]+1;
						xq.add(x);
						yq.add(y);
					}
				}
			}
		}
		return out;
	}
}
