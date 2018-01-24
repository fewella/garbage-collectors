// import the API.
// See xxx for the javadocs.
import MapTools.RocketLanding;
import bc.*;

import java.util.*;
import Utils.Tuple;
import MapTools.*;

class MapAnalysis {
	static final int MAX_D = 10;

    //Earth only
    static MapLocation baseLocation;
    static PriorityQueue<Tuple<MapLocation, Integer>> factoryQueue;

    //private variables
    private static int[][] blankBFS;

    //public methods
    //feel free to use them
    public static ArrayList<MapLocation> initialFactory(){
        //1. divvy up workers by chambers/distances
	    //preliminary
	    VecUnit workers = Player.mapEarth.getInitial_units();
	    ArrayList<MapLocation> enemies = new ArrayList<>();
	    ArrayList<MapLocation> allies = new ArrayList<>();
	    //separate by chambers
	    Map<Integer, ArrayList<Unit>> chamberMap = new HashMap<>(3);
	    for(int i = 0; i < workers.size(); i++){
		    Unit worker = workers.get(i);
	    	if(worker.team() == Player.gc.team()){
			    MapLocation loc = worker.location().mapLocation();
	    		allies.add(loc);
			    //add to chamber collection
			    int id = UnionFind.id(Planet.Earth, loc.getX(), loc.getY());
			    if(!chamberMap.containsKey(id))
			    	chamberMap.put(id, new ArrayList<>(3));
			    chamberMap.get(id).add(worker);
		    }
		    else{
			    enemies.add(worker.location().mapLocation());
		    }
	    }
        //weed out workers without reach to enemy
	    int[][] enemyBFS = BFS(enemies, false);
		for(int key : chamberMap.keySet()){
	    	MapLocation loc = chamberMap.get(key).get(0).location().mapLocation();
			if(enemyBFS[loc.getY()][loc.getX()] == -1)
				chamberMap.remove(key);
		}
	    //divide workers that are far
	    ArrayList<ArrayList<Unit>> sep = new ArrayList<>(3);
	    Map<Unit, int[][]> cache = new HashMap<>(3);
	    for(int key : chamberMap.keySet()){
			ArrayList<Unit> val = chamberMap.get(key);
			if(val.size() == 1)
				sep.add(val);
			else{
				//compute distances from 0
				ArrayList<MapLocation> temp = new ArrayList<>(1);
				temp.add(val.get(0).location().mapLocation());
				int[][] BFS0 = BFS(temp, false);
				cache.put(val.get(0), BFS0);

				MapLocation loc1 = val.get(1).location().mapLocation();
				if(val.size() == 2){
					if(BFS0[loc1.getY()][loc1.getX()] > MAX_D) {
						ArrayList<Unit> split = new ArrayList<>(1);
						split.add(val.remove(1));
						sep.add(split);
					}
					sep.add(val);
				}
				else{
					boolean spl01, spl02;
					MapLocation loc2 = val.get(2).location().mapLocation();
					spl01 = BFS0[loc1.getY()][loc1.getX()] > MAX_D;
					spl02 = BFS0[loc2.getY()][loc2.getX()] > MAX_D;
					if(spl01 || spl02){
						boolean spl12;
						ArrayList<MapLocation> temp2 = new ArrayList<>(1);
						temp2.add(val.get(1).location().mapLocation());
						int[][] BFS1 = BFS(temp2, false);
						cache.put(val.get(1), BFS1);
						spl12 = BFS1[loc2.getY()][loc2.getX()] > MAX_D;

						if(spl02 && spl12){
							ArrayList<Unit> split = new ArrayList<>(1);
							split.add(val.remove(2));
							sep.add(split);
							if(spl01){
								split = new ArrayList<>(1);
								split.add(val.remove(1));
								sep.add(split);
							}
						}
						else if(spl01){
							if(spl12){
								ArrayList<Unit> split = new ArrayList<>(1);
								split.add(val.remove(1));
								sep.add(split);
							}
							else if(spl02) {
								ArrayList<Unit> split = new ArrayList<>(1);
								split.add(val.remove(0));
								sep.add(split);
							}
						}
					}
					sep.add(val);
				}
			}
	    }

	    //2. Get a factory location for each group of workers
	    ArrayList<MapLocation> out = new ArrayList<>(sep.size());
	    Econ.initAssignments = new HashMap<>(sep.size());
	    for(ArrayList<Unit> group : sep){
	    	if(group.size() == 1) {
			    //special case: 1 worker
			    Unit worker = group.get(0);
			    //build immediately
			    int w = (int)Player.gc.startingMap(Planet.Earth).getWidth();
			    int h = (int)Player.gc.startingMap(Planet.Earth).getHeight();
			    int[][] pass = Passable.matrix(Planet.Earth);
			    MapLocation workerLoc = worker.location().mapLocation();
			    MapLocation bestLoc = workerLoc;
			    int max = -999999999;
			    Econ.initAssignments.put(group.get(0).id(), out.size());
			    for(int i = 1; i < Player.dirs.length; i++){
			    	MapLocation factLoc = workerLoc.add(Player.dirs[i]);
				    if(factLoc.getX() < 0 || factLoc.getY() < 0 || factLoc.getX() >= w || factLoc.getY() >= h)
					    continue;
			    	if(pass[factLoc.getY()][factLoc.getX()] == 0)
			    		continue;
				    int score = -Karbonite.matrix(Planet.Earth, 1)[factLoc.getY()][factLoc.getX()];
				    Queue<MapLocation> nq = new LinkedList<>();
				    int[][] ns = new int[h][w];
				    int nsize = 1;
				    nq.add(workerLoc);
				    ns[workerLoc.getY()][workerLoc.getX()] = 1;
				    while(!nq.isEmpty()){
				    	MapLocation og = nq.remove();
				    	for(int j = 1; j < Player.dirs.length; j++){
						    MapLocation neighborLoc = og.add(Player.dirs[j]);
						    if(ns[neighborLoc.getY()][neighborLoc.getX()] == 1)
						        continue;
						    if(neighborLoc.getX() < 0 || neighborLoc.getY() < 0 || neighborLoc.getX() >= w || neighborLoc.getY() >= h)
							    continue;
						    if(pass[neighborLoc.getY()][neighborLoc.getX()] == 0)
							    continue;
						    if(neighborLoc.equals(factLoc))
						        continue;
							if(!neighborLoc.isAdjacentTo(factLoc))
								continue;
							ns[neighborLoc.getY()][neighborLoc.getX()] = 1;
							nsize++;
							nq.add(neighborLoc);
					    }
				    }
				    score += 300*nsize;
				    score -= 100*enemyBFS[factLoc.getY()][factLoc.getX()];
				    if(score < max){
				    	max = score;
				    	bestLoc = factLoc;
				    }
			    }
			    out.add(bestLoc);
		    }
		    else{
	    		//compute distances
	    		int[][][] BFSMats = new int[group.size()][][];
	    		for(int i = 0; i < group.size(); i++){
	    			Unit worker = group.get(i);
				    Econ.initAssignments.put(worker.id(), out.size());
					if(cache.containsKey(worker))
						BFSMats[i] = cache.get(worker);
					else{
						ArrayList<MapLocation> temp = new ArrayList<>(1);
						temp.add(worker.location().mapLocation());
						BFSMats[i] = BFS(temp, false);
					}
			    }
			    //avoid locations inside workers
			    for(int i = 0; i < workers.size(); i++){
				    MapLocation loc = workers.get(i).location().mapLocation();
				    BFSMats[0][loc.getY()][loc.getX()] = 9999;
			    }
			    //calculate optimal locations
			    int min = 9999;
			    ArrayList<Integer> candX = new ArrayList<>();
			    ArrayList<Integer> candY = new ArrayList<>();
			    for(int y = 0; y < Player.mapEarth.getHeight(); y++){
			    	cell:
				    for(int x = 0; x < Player.mapEarth.getWidth(); x++){
					    int total = 0;
					    int min2 = 9999;
					    for(int[][] Mat : BFSMats) {
						    int val = Mat[y][x];
						    if(val == -1)
						    	continue cell;
						    if(val < min2)
						    	min2 = val;
					    	total += val;
					    }
					    total += 5*min2;
					    if(total < min){
						    min = total;
						    candX = new ArrayList<>();
						    candY = new ArrayList<>();
						    candX.add(x);
						    candY.add(y);
					    }
					    else if(total == min){
						    candX.add(x);
						    candY.add(y);
					    }
				    }
			    }
			    //tiebreaks - find location
			    min = 9999;
			    int minX = 3;
			    int minY = 3;
			    for(int i = 0; i < candX.size(); i++){
				    int x = candX.get(i);
				    int y = candY.get(i);
				    int val2 = enemyBFS[y][x];
				    if(val2 < min){
					    min = val2;
					    minX = x;
					    minY = y;
				    }
			    }
			    out.add(new MapLocation(Planet.Earth, minX, minY));
		    }
	    }
	    return out;
    }
    public static int[][] BFS(ArrayList<MapLocation> destinations){
        return BFS(destinations, true);
    }
    public static int[][] BFS(ArrayList<MapLocation> destinations, boolean structures){
        Queue<Integer> xq = new LinkedList<>();
        Queue<Integer> yq = new LinkedList<>();
        int h = (int)Player.map.getHeight();
        int w = (int)Player.map.getWidth();
        int[][] out = Utils.Misc.cloneMat(blankBFS);
        //add factories, rockets, (workers for now)
        if(structures){
            for(Unit wo : Player.worker){
                Location loc = wo.location();
                if(!loc.isOnMap()) continue;
                MapLocation mapLoc = loc.mapLocation();
                out[mapLoc.getY()][mapLoc.getX()] = 9999;
            }
            for(Unit f : Player.factory){
                MapLocation mapLoc = f.location().mapLocation();
                out[mapLoc.getY()][mapLoc.getX()] = 9999;
            }
            for(Unit r : Player.rocket){
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

    //methods for Player
    public static void setup(){
        Utils.Convolver c4 = new Utils.Convolver(4);
        Passable.setup(Player.gc);
        Karbonite.setup(Player.gc, Passable.matrix(Planet.Mars));
        //7 rounds to switch to Earth's turn, build unit, load to rocket, and launch
        UnionFind.setup(Player.gc, Passable.matrix(Planet.Earth), Passable.matrix(Planet.Mars), Karbonite.matrix(Planet.Earth, 0), Karbonite.matrix(Planet.Mars, 743));
        RocketLanding.setup(Player.gc, c4);
        //for BFS
        int h = (int)Player.map.getHeight();
        int w = (int)Player.map.getWidth();
        blankBFS = new int[h][w];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++) {
                if(Passable.matrix(Player.gc.planet())[y][x] == 0)
                    blankBFS[y][x] = 9999;
                else{
                    blankBFS[y][x] = -1;
                }
            }
        }
    }
    public static void turn(){
        MapTools.RocketLanding.turn();
    }
}