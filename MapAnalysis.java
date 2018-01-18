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
    private static short[][] smallBase =
            {{1, 1, 0, 1, 1},
            {1, 1, 0, 1, 1},
            {0, 0, 0, 0, 0},
            {1, 1, 0, 1, 1},
            {1, 1, 0, 1, 1}};
    private static int[][] blankBFS;

    //public methods
    //feel free to use them
    public static MapLocation tempWorkerLoc(Unit v){
        int bestScore = -9999;
        MapLocation bestLoc = v.location().mapLocation();
        for(int tries = 0; tries < 50; tries++){
            int x = MapAnalysis.baseLocation.getX() + (int)(20*Math.random())-10;
            int y = MapAnalysis.baseLocation.getY() + (int)(20*Math.random())-10;
            if(x < 0 || y < 0 || x >= Player.mapEarth.getWidth() || y >= Player.mapEarth.getHeight())
                continue;
            if(MapTools.Passable.matrix(Planet.Earth)[y][x] == 0)
                continue;
            if(!MapTools.UnionFind.connect(Planet.Earth, x, y, MapAnalysis.baseLocation.getX(), MapAnalysis.baseLocation.getY()))
                continue;
            int score = 0;
            for(int dy = -2; dy <= 2; dy++){
                for(int dx = -2; dx <= 2; dx++){
                    if(x+dx < 0 || y+dy < 0 || x+dx >= Player.mapEarth.getWidth() || y+dy >= Player.mapEarth.getHeight())
                        continue;
                    if(MapTools.Passable.matrix(Planet.Earth)[y+dy][x+dx] == 1) {
                        int temp = Math.abs(dx*dy);
                        if(temp == 0)
                            score += 8;
                        else
                            score += 8/temp;
                    }
                }
            }
            MapLocation loc = new MapLocation(Planet.Earth, x, y);
            VecUnit initW = Player.mapEarth.getInitial_units();
            int minD = 9999;
            for(int i = 0; i < initW.size(); i++){
                if(initW.get(i).team()!=Player.gc.team()){
                    int d = (int)Math.sqrt(loc.distanceSquaredTo(initW.get(i).location().mapLocation()));
                    if(d < minD)
                        minD = d;
                }
            }
            score += 5*minD;
            score -= 2*(int)Math.sqrt(loc.distanceSquaredTo(MapAnalysis.baseLocation));
            minD = 9999;
            for(MapLocation other : Econ.workerLocs){
                int d = (int)Math.sqrt(loc.distanceSquaredTo(other));
                if(d < minD)
                    minD = d;
            }
            score += 10*Math.min(0, minD-6);
            if(score > bestScore){
                bestScore = score;
                bestLoc = loc;
            }
        }
        Econ.workerLocs.add(bestLoc);
        return bestLoc;
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
        RocketLanding.setup(Player.gc);
        Passable.setup(Player.gc);
        Karbonite.setup(Player.gc, Passable.matrix(Planet.Mars));
        //7 rounds to switch to Earth's turn, build unit, load to rocket, and launch
        UnionFind.setup(Player.gc, Passable.matrix(Planet.Earth), Passable.matrix(Planet.Mars), Karbonite.matrix(Planet.Earth, 0), Karbonite.matrix(Planet.Earth, 743));
        if (Player.gc.planet() == Planet.Earth) {
            Utils.Convolver c4 = new Utils.Convolver(4);
            baseLocation(opennnesMat(Passable.matrix(Planet.Earth), c4));
            System.out.println("Base location: " + baseLocation.getX() + ", " + baseLocation.getY());

            baseFactoryQueue(baseLocation, smallBase, Passable.matrix(Planet.Earth), Karbonite.matrix(Planet.Earth, 0));
        }
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
        //first Factory
        if(Player.gc.planet() == Planet.Earth){
            initialFactory();
            Econ.stage = 0;
        }
    }
    public static void turn(){
        MapTools.RocketLanding.turn();
    }

    //private methods
    private static int[][] opennnesMat(short[][] passMat, Utils.Convolver c4){
        //0 is completely occupied
        //255 is completely open
        c4.setRadius(4);
        return c4.blur(passMat);
    }
    //Earth only
    private static void baseLocation(int[][] opennessMat){
        //Returns suitable location for a base
        //Based on available space and distance from initial workers
        double max = 0;
        int maxX = 0;
        int maxY = 0;
        VecUnit workers = Player.mapEarth.getInitial_units();
        ArrayList<MapLocation> workerLocs = new ArrayList<>();
        ArrayList<MapLocation> enemyWorkerLocs = new ArrayList<>();
        for(int i = 0; i < workers.size(); i++){
            Unit worker = workers.get(i);
            if(worker.team() == Player.gc.team()) {
                workerLocs.add(worker.location().mapLocation());
            }
            else{
                enemyWorkerLocs.add(worker.location().mapLocation());
            }
        }
        MapLocation center = new MapLocation(Planet.Earth, opennessMat[0].length/2, opennessMat.length/2);
        for(int y = 0; y < opennessMat.length; y++){
            for(int x = 0; x < opennessMat[0].length; x++){
                //weight:
                //friendly distance -[0, 150]   -5*linear_dist, 30 blocks = -150, caps at -150
                //enemy distance +[0, 150]   +5*linear_dist, 30 blocks = 150, caps at 150
                //openness +[0, 255]   +openness
                //center   +[0, 100]    +2.5*linear_dist, 40 blocks = 100, caps at 100

                //workers
                MapLocation loc = new MapLocation(Planet.Earth, x, y);
                double val = 0;
                for(MapLocation workerLoc : workerLocs){
                    val -= Math.sqrt(workerLoc.distanceSquaredTo(loc));
                }
                val /= workerLocs.size();
                val = Math.max(-30, val);
                val *= 5;
                double val2 = 0;
                for(MapLocation workerLoc : enemyWorkerLocs){
                    val2 += Math.sqrt(workerLoc.distanceSquaredTo(loc));
                }
                val2 /= enemyWorkerLocs.size();
                val2 = Math.min(30, val2);
                val2 *= 5;
                val += val2;

                //openness, center
                val += opennessMat[y][x];
                val += Math.min(100, 2.5*Math.sqrt(center.distanceSquaredTo(center)));
                if(val > max){
                    max = val;
                    maxX = x;
                    maxY = y;
                }
            }
        }
        baseLocation = new MapLocation(Planet.Earth, maxX, maxY);
    }
    public static MapLocation[] computeLaunchingLocations(int launchingLocations) {
        MapLocation[] locs = new MapLocation[launchingLocations];
        //distance from base: 3-6 tiles
        int x0 = baseLocation.getX();
        int y0 = baseLocation.getY();
        int minX = x0 - 6;
        int minY = y0 - 6;
        int maxX = x0 + 6;
        int maxY = y0 + 6;
        int i = 0;
        for(int row = minY; row < maxY; row++ ){
            for(int col = minX; col < maxX; col++) {
                if(col >= 0 && row >= 0 && col < Player.mapEarth.getWidth() && row < Player.mapEarth.getHeight()) { // If on the map
                    if(MapTools.Passable.matrix(Planet.Earth)[row][col] == 1) { // If legal spot
                        if(Math.abs(col-x0) >= 3 && Math.abs(row-y0) >= 3) { //If far enough away from base
                            locs[i++] = new MapLocation(Planet.Earth, col, row);
                            launchingLocations--;
                            if(launchingLocations == 0)
                                return locs;
                        }
                    }
                }
            }
        }
        return locs;
    }
    private static void baseFactoryQueue(MapLocation baseLocation, short[][] baseMat, short[][] passMat, short[][] karbMat){
        //Returns a priorityQueue of locations to build factories
        //Priority based on distance to center and lost Karbonite
        //Locations guaranteed to be valid

        factoryQueue = new PriorityQueue<>(16, new mapLocationComparator());

        int xTemp = baseLocation.getX()-baseMat[0].length/2;
        int yTemp = baseLocation.getY()-baseMat.length/2;

        for(int yi = 0; yi < baseMat.length; yi++){
            for(int xi = 0; xi < baseMat[0].length; xi++){
                int x = xTemp+xi;
                int y = yTemp+yi;
                if(baseMat[yi][xi]==1 && x>=0 && y>=0 && x<passMat[0].length && y<passMat.length && passMat[y][x]==1){
                    MapLocation loc = new MapLocation(Planet.Earth, x, y);
                    factoryQueue.add(new Tuple<>(loc, (int)(5*Math.sqrt(loc.distanceSquaredTo(baseLocation)))+(int)karbMat[y][x]));
                }
            }
        }
    }
    private static void initialFactory(){
        VecUnit workers = Player.mapEarth.getInitial_units();
        ArrayList<int[][]> BFSMats = new ArrayList<>();
        ArrayList<MapLocation> enemyWorkers = new ArrayList<>();
        ArrayList<Integer> friendlyWorkers = new ArrayList<>();
        for(int i = 0; i < workers.size(); i++){
            Unit worker = workers.get(i);
            if(worker.team() == Player.gc.team()){
                friendlyWorkers.add(worker.id());
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
        //create matrix
        ArrayList<MapLocation> temp = new ArrayList<>();
        temp.add(new MapLocation(Planet.Earth, minX, minY));
        Econ.workerBFSMat = BFS(temp, false);
        Econ.workerBFSMats = new HashMap<>();
        Econ.workerLocs = new ArrayList<>();
        Econ.seen = new HashMap<>();
        for(int id : friendlyWorkers){
            Econ.workerBFSMats.put(id, Econ.workerBFSMat);
            Econ.seen.put(id, true);
        }
    }
    private static void baseFactory(){
        //TODO
    }
}

class mapLocationComparator implements Comparator<Tuple<MapLocation, Integer>> {
    public int compare(Tuple<MapLocation, Integer> a, Tuple<MapLocation, Integer> b){
        return a.y - b.y;
    }
}
