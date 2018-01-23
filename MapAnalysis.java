// import the API.
// See xxx for the javadocs.
import MapTools.RocketLanding;
import bc.*;

import java.util.*;
import Utils.Tuple;
import MapTools.*;

class MapAnalysis {
    //Earth only
    static MapLocation baseLocation;
    static PriorityQueue<Tuple<MapLocation, Integer>> factoryQueue;

    //private variables
    private static int[][] blankBFS;

    //public methods
    //feel free to use them
    public static MapLocation initialFactory(){
        VecUnit workers = Player.mapEarth.getInitial_units();
        ArrayList<int[][]> BFSMats = new ArrayList<>();
        ArrayList<MapLocation> enemyWorkers = new ArrayList<>();
        for(int i = 0; i < workers.size(); i++){
            Unit worker = workers.get(i);
            if(worker.team() == Player.gc.team()){
                ArrayList<MapLocation> temp = new ArrayList<>();
                temp.add(worker.location().mapLocation());
                BFSMats.add(BFS(temp, false));
            }
            else{
                enemyWorkers.add(worker.location().mapLocation());
            }
        }
        //avoid locations inside workers
        for(int i = 0; i < workers.size(); i++){
            MapLocation loc = workers.get(i).location().mapLocation();
            BFSMats.get(0)[loc.getY()][loc.getX()] = 9999;
        }
        //calculate optimal locations
        int min = 9999;
        ArrayList<Integer> candX = new ArrayList<>();
        ArrayList<Integer> candY = new ArrayList<>();
        for(int y = 0; y < Player.mapEarth.getHeight(); y++){
            for(int x = 0; x < Player.mapEarth.getWidth(); x++){
                int total = 0;
                for(int[][] Mat : BFSMats)
                    total += Mat[y][x];
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
        int[][] combotBFSMat = BFS(enemyWorkers, false);
        min = 9999;
        int minX = 3;
        int minY = 3;
        for(int i = 0; i < candX.size(); i++){
            int x = candX.get(i);
            int y = candY.get(i);
            int val2 = combotBFSMat[y][x];
            if(val2 < min){
                min = val2;
                minX = x;
                minY = y;
            }
        }
        return new MapLocation(Planet.Earth, minX, minY);
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