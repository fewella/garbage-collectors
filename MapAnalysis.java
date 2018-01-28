// import the API.
// See xxx for the javadocs.
import MapTools.RocketLanding;
import bc.*;

import java.util.*;
import MapTools.*;

class MapAnalysis {
	static final int MAX_D = 10;

    //public methods
    //feel free to use them
    public static int[][] BFS(ArrayList<MapLocation> destinations){
        return Pathing.BFS(destinations, true);
    }

    //methods for Player
    public static void setup(){
        Utils.Convolver c4 = new Utils.Convolver(4);
        Passable.setup(Player.gc);
        Karbonite.setup(Player.gc, Passable.matrix(Planet.Mars));
        //7 rounds to switch to Earth's turn, build unit, load to rocket, and launch
        UnionFind.setup(Player.gc, Passable.matrix(Planet.Earth), Passable.matrix(Planet.Mars), Karbonite.matrix(Planet.Earth, 0), Karbonite.matrix(Planet.Mars, 743));
        RocketLanding.setup(Player.gc, c4);
        Pathing.setup(Player.gc);
	    if(Player.gc.planet() == Planet.Earth) {
		    //for Rollout
		    VecUnit wks = Player.mapEarth.getInitial_units();
		    ArrayList<MapLocation> f, e;
		    f = new ArrayList<>((int)wks.size()/2);
		    e = new ArrayList<>((int)wks.size()/2);
		    int[][] occ = new int[(int)Player.mapEarth.getHeight()][(int)Player.mapEarth.getWidth()];
		    Map<Integer, ArrayList<Unit>> groups = new HashMap<>((int)wks.size()/2);
		    Map<Integer, int[][]> BFS = new HashMap<>((int)wks.size()/2);
		    for(int i = 0; i < wks.size(); i++){
			    Unit wo = wks.get(i);
			    MapLocation loc = wo.location().mapLocation();
			    if(wo.team() == Player.gc.team()) {
				    f.add(loc);
				    ArrayList<MapLocation> temp = new ArrayList<>(1);
				    temp.add(loc);
				    BFS.put(wo.id(), Pathing.BFS(temp, false));

				    int id = UnionFind.id(Planet.Earth, loc.getX(), loc.getY());
				    if(!groups.containsKey(id))
					    groups.put(id, new ArrayList<>(3));
				    groups.get(id).add(wo);
			    }
			    else
				    e.add(loc);
			    occ[loc.getY()][loc.getX()] = 1;
		    }
	    	Rollout.setup(Player.gc, c4, Pathing.BFS(e, false), Pathing.BFS(f, false), occ, groups, BFS);
	    }
    }
    public static void turn(){
    	Pathing.update(Player.worker, Player.factory, Player.rocket);
	    if (Player.gc.planet() == Planet.Earth) {
		    Rollout.turn(Player.worker);
	    }
        //MapTools.RocketLanding.turn();
    }
}