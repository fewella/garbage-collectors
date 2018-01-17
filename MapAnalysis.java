// import the API.
// See xxx for the javadocs.
import bc.*;

import java.awt.*;
import java.util.*;

class MapAnalysis {
    //coordinate system:
    //(0, 0) in bottom left (south-west) corner
    //y increases upwards (northward)
    //x increases to the right (eastward)

    //public variables
    //feel free to use them
    static short[][] passabilityMat, passabilityMatEarth, passabilityMatMars;
    static short passableTotal, passableTotalEarth, passableTotalMars;
    static short[][] karboniteMatEarth;
    static int karboniteTotalEarth;
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
    //union-find structures
    private static short[] idEarth, idMars;
    private static short[] szEarth, szMars;
    private static int[] karbEarth, karbMars;
    //rocket landing location structures
    private static int deliveryId, orderId;
    private static short[] received, used;
    //Mars only
    private static ArrayList<short[][]> karbonite3dMat; //(should take .5 MB max)
    private static ArrayList<Integer> karboniteTotalArray, karboniteRoundArray;

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
            if(MapAnalysis.passabilityMatEarth[y][x] == 0)
                continue;
            if(!MapAnalysis.connectivity(Planet.Earth, x, y, MapAnalysis.baseLocation.getX(), MapAnalysis.baseLocation.getY()))
                continue;
            int score = 0;
            for(int dy = -2; dy <= 2; dy++){
                for(int dx = -2; dx <= 2; dx++){
                    if(x+dx < 0 || y+dy < 0 || x+dx >= Player.mapEarth.getWidth() || y+dy >= Player.mapEarth.getHeight())
                        continue;
                    if(MapAnalysis.passabilityMatEarth[y+dy][x+dx] == 1) {
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
        int[][] out = cloneMat(blankBFS);
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
    public static Tuple<short[][], Short> karboniteMatMars(int round){
        if(round < 1)   round = 1;
        else if(round > 1000)   round = 1000;
        //binary search
        int high = karboniteRoundArray.size();
        int low = 0;
        int mid;
        while(low < high){
            mid = (high+low)/2;
            if(karboniteRoundArray.get(mid) < round)
                low=mid+1;
            else if(karboniteRoundArray.get(mid) == round)
                return new Tuple<>(karbonite3dMat.get(mid), karboniteTotalArray.get(mid).shortValue());
            else
                high=mid;
        }
        return new Tuple<>(karbonite3dMat.get(low-1), karboniteTotalArray.get(low-1).shortValue());
    }
    public static boolean connectivity(Planet p, int x1, int y1, int x2, int y2){
        int w;
        short[] id;
        if(p == Planet.Earth){
            w = (int)Player.mapEarth.getWidth();
            id = idEarth;
        }
        else{
            w = (int)Player.mapMars.getWidth();
            id = idMars;
        }
        return root(id, (short)(y1*w+x1)) == root(id, (short)(y2*w+x2));
    }
    public static int chamberSize(Planet p, int x, int y){
        int w;
        short[] id, sz;
        if(p == Planet.Earth){
            w = (int)Player.mapEarth.getWidth();
            id = idEarth;
            sz = szEarth;
        }
        else{
            w = (int)Player.mapMars.getWidth();
            id = idMars;
            sz = szMars;
        }
        return sz[root(id, (short)(y*w+x))];
    }
    public static int karboniteConnected(Planet p, int x, int y){
        int w;
        short[] id;
        int[] karb;
        if(p == Planet.Earth){
            w = (int)Player.mapEarth.getWidth();
            id = idEarth;
            karb = karbEarth;
        }
        else{
            w = (int)Player.mapMars.getWidth();
            id = idMars;
            karb = karbMars;
        }
        return karb[root(id, (short)(y*w+x))];
    }
    //Earth only
    /**
     * Call from Earth to request landing locations. While this is done periodically, calling this 100 turns before
     * requesting locations ensures that the data is most recent (50 turns old), and minimizes the risk of running out of locations.
     * @param tt number of locations that will be received with requestLandingLocations(true, true)
     * @param tf number of locations that will be received with requestLandingLocations(true, false)
     * @param ft number of locations that will be received with requestLandingLocations(false, true)
     * @param ff number of locations that will be received with requestLandingLocations(false, false)
     */
    public static void landingLocationsRequest(int tt, int tf, int ft, int ff){
        Player.gc.writeTeamArray(0, ++orderId);
        Player.gc.writeTeamArray(1, tt);
        Player.gc.writeTeamArray(2, tf);
        Player.gc.writeTeamArray(3, ft);
        Player.gc.writeTeamArray(4, ff);
    }
    /**
     * Call from Earth to receive a most suitable rocket landing location.
     * For optimal results, call requestLandingLocations() 51 turns before calling this.
     * @param aggressive <br>true: prioritize bombarding enemy base
     *                   <br>false: avoid enemies
     * @param congressive <br>true: prioritize sending close to friendly units
     *                    <br>false: prioritize covering more ground
     * @return MapLocation of the best landing location
     */
    public static MapLocation landingLocationsRetrieve(boolean aggressive, boolean congressive){
        int i = 0;
        if(!aggressive) i = 2;
        if(!congressive) i++;
        if(received[i] <= used[i]){
            System.out.println("Ran out of landing locations for aggressive: " + aggressive + " congressive: " + congressive);
            //return random location
            while(true) {
                int x = (int) (Player.mapMars.getWidth() * Math.random());
                int y = (int) (Player.mapMars.getHeight() * Math.random());
                if(passabilityMatMars[y][x] == 1) return new MapLocation(Planet.Mars, x, y);
            }
        }
        int i2 = 5+used[i]++;
        for(int j = 0; j < i; j++) i2 += received[j];
        int val = Player.arrayMars.get(i2);

        return new MapLocation(Planet.Mars, (int)(val/Player.mapMars.getWidth()), (int)(val%Player.mapMars.getWidth()));
    }
    //methods for Player
    public static void setup(){
        orderId = 0;
        deliveryId = 0;
        received = new short[4];
        used = new short[4];
        //primary analysis
        passabilityMat(Player.mapEarth);
        passabilityMat(Player.mapMars);
        karboniteMat();
        //secondary analysis
        karbonite3dMat(passabilityMatMars);
        //7 rounds to switch to Earth's turn, build unit, load to rocket, and launch
        connectivityArr(Planet.Earth, passabilityMatEarth, karboniteMatEarth);
        connectivityArr(Planet.Mars, passabilityMatMars, karboniteMatMars(743).x);
        if (Player.gc.planet() == Planet.Earth) {
            passabilityMat = passabilityMatEarth;
            passableTotal = passableTotalEarth;

            Convolver c4 = new Convolver(4);
            baseLocation(opennnesMat(passabilityMatEarth, c4));
            System.out.println("Base location: " + baseLocation.getX() + ", " + baseLocation.getY());

            baseFactoryQueue(baseLocation, smallBase, passabilityMatEarth, karboniteMatEarth);
        }
        else{
            passabilityMat = passabilityMatMars;
            passableTotal = passableTotalMars;
        }
        //for BFS
        int h = (int)Player.map.getHeight();
        int w = (int)Player.map.getWidth();
        blankBFS = new int[h][w];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++) {
                if(passabilityMat[y][x] == 0)
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
        if(Player.gc.planet() == Planet.Earth){
            //listen to delivery
            int newDeliveryId = Player.arrayMars.get(0);
            if(deliveryId != newDeliveryId){
                deliveryId = newDeliveryId;
                received = new short[4];
                used = new short[4];
                //read delivery details
                for(int i = 0; i < 4; i++)
                    received[i] = (short)(Player.arrayMars.get(i+1));
            }
        }
        else{
            //auto send
            if (Player.gc.round() % 50 == 1)
                landingLocationsCompute(10, 10, 10, 10);
            //listen to orders
            int newOrderId = Player.arrayEarth.get(0);
            if(orderId != newOrderId){
                orderId = newOrderId;
                //process order, send delivery
                landingLocationsCompute(Player.arrayEarth.get(1), Player.arrayEarth.get(2), Player.arrayEarth.get(3), Player.arrayEarth.get(4));
            }
        }
    }

    //private methods
    private static void passabilityMat(PlanetMap pm){
        //same outputs as PlanetMap.isPassableTerrainAt
        short[][] out = new short[(int)pm.getHeight()][(int)pm.getWidth()];
        short total = 0;
        for(int y = 0; y < pm.getHeight(); y++){
            for(int x = 0; x < pm.getWidth(); x++){
                out[y][x] = pm.isPassableTerrainAt(new MapLocation(pm.getPlanet(), x, y));
                if(out[y][x] == 1) total++;
            }
        }
        if(pm.getPlanet() == Planet.Earth){
            passabilityMatEarth = out;
            passableTotalEarth = total;
        }
        else{
            passabilityMatMars = out;
            passableTotalMars = total;
        }
    }
    private static int[][] opennnesMat(short[][] passMat, Convolver c4){
        //0 is completely occupied
        //255 is completely open
        c4.setRadius(4);
        return c4.blur(passMat);
    }
    private static void connectivityArr(Planet p, short[][] passMat, short[][] karbMat){
        //Union-find:
        //https://www.cs.princeton.edu/~rs/AlgsDS07/01UnionFind.pdf
        //add nodes
        int w = passMat[0].length;
        int N = passMat.length*w;
        short[] id = new short[N];
        int[] karb = new int[N];
        short[] sz = new short[N];
        for(short i = 0; i < N; i++){
            id[i] = i;
            karb[i] = karbMat[i/w][i%w];
            sz[i] = 1;
        }
        //connect edges
        for(short i = 0; i < N; i++){
            if(passMat[i/w][i%w] == 0)
                continue;
            if(i >= w){
                if(passMat[(i-w)/w][(i-w)%w] == 1 && root(id, i) != root(id, (short)(i-w))) unite(id, karb, sz, i, (short)(i-w));
                if(i%w > 0 && passMat[(i-w-1)/w][(i-w-1)%w] == 1 && root(id, i) != root(id, (short)(i-w-1)))
                    unite(id, karb, sz, i, (short)(i-w-1));
                if(i%w < w-1 && passMat[(i-w+1)/w][(i-w+1)%w] == 1 && root(id, i) != root(id, (short)(i-w+1)))
                    unite(id, karb, sz, i, (short)(i-w+1));
            }
            if(i%w > 0 && passMat[(i-1)/w][(i-1)%w] == 1 && root(id, i) != root(id, (short)(i-1)))
                unite(id, karb, sz, i, (short)(i-1));
        }
        //count karbonite in walls
        //TODO
        if(p == Planet.Earth){
            idEarth = id;
            karbEarth = karb;
            szEarth = sz;
        }
        else{
            idMars = id;
            karbMars = karb;
            szMars = sz;
        }

    }
    //Earth only
    private static void karboniteMat(){
        //same outputs as PlanetMap.initialKarboniteAt
        karboniteMatEarth = new short[(int)Player.mapEarth.getHeight()][(int)Player.mapEarth.getWidth()];
        karboniteTotalEarth = 0;
        for(int y = 0; y < Player.mapEarth.getHeight(); y++){
            for(int x = 0; x < Player.mapEarth.getWidth(); x++){
                long karb = Player.mapEarth.initialKarboniteAt(new MapLocation(Player.mapEarth.getPlanet(), x, y));
                karboniteMatEarth[y][x] = (short)karb;
                karboniteTotalEarth += karb;
            }
        }
        System.out.println("Earth total karbonite: " + karboniteTotalEarth);
    }
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
                    if(passabilityMatEarth[row][col] == 1) { // If legal spot
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
    private static void landingLocationsComputeInitial(){
        //TODO
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
        int[][] BFSMat = BFS(temp, false);
        Econ.workerBFSMats = new HashMap<>();
        Econ.workerLocs = new ArrayList<>();
        Econ.seen = new HashMap<>();
        for(int id : friendlyWorkers){
            Econ.workerBFSMats.put(id, BFSMat);
            Econ.seen.put(id, true);
        }
    }
    private static void baseFactory(){
        //TODO
    }
    //Mars only
    private static void karbonite3dMat(short[][] passMat){
        //third dimension is time
        //modify passMat
        short[][] passMat2 = cloneMat(passMat);
        for(int y = 0; y < passMat[0].length; y++){
            for(int x = 0; x < passMat.length; x++){
                if(passMat[y][x] != 0) continue;
                kernel:
                for(int xi = -1; xi <= 1; xi++){
                    for(int yi = -1; yi <= 1; yi++){
                        if(y+yi < 0 || y+yi >= passMat[0].length || x+xi < 0 || x+xi >= passMat.length) continue;
                        if(passMat[y+yi][x+xi] == 1){
                            passMat2[y][x] = 1;
                            break kernel;
                        }
                    }
                }
            }
        }
        //process asteroids
        AsteroidPattern asteroids = Player.gc.asteroidPattern();
        short[][] cumulativeMat = new short[(int)Player.mapMars.getHeight()][(int)Player.mapMars.getWidth()];
        int cumulative = 0;
        karbonite3dMat = new ArrayList<>();
        karboniteTotalArray = new ArrayList<>();
        karboniteRoundArray = new ArrayList<>();
        if(!asteroids.hasAsteroid(1)) {
            karbonite3dMat.add(cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(1);
        }
        for(int round = 0; round < 1000; round++){
            if(!asteroids.hasAsteroid(round))
                continue;
            AsteroidStrike asteroid = asteroids.asteroid(round);
            cumulativeMat[asteroid.getLocation().getY()][asteroid.getLocation().getX()] += asteroid.getKarbonite();
            if(passMat2[asteroid.getLocation().getY()][asteroid.getLocation().getX()] == 1) cumulative += asteroid.getKarbonite();
            karbonite3dMat.add(cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(round);
        }
        System.out.println("Mars total reachable karbonite: " + cumulative);
    }
    private static void landingLocationsCompute(int tt, int tf, int ft, int ff){
        //watch for improper arguments
        int total = tt+tf+ft+ff;
        if(total > 40) {
            System.out.println("Number of total requested locations (" + total + ") exceeds 40");
            tt = tf = ft = ff = 10;
        }
        Player.gc.writeTeamArray(0, ++deliveryId);
        Player.gc.writeTeamArray(1, tt);
        Player.gc.writeTeamArray(2, tf);
        Player.gc.writeTeamArray(3, ft);
        Player.gc.writeTeamArray(4, ff);
        //compute best locations
        //TODO
        long c = 5;
        long i = c;
        int w = (int)Player.mapMars.getWidth();
        for(c += tt; i < c; i++){
            Player.gc.writeTeamArray(i, 0);
        }
        for(c += tf; i < c; i++){
            Player.gc.writeTeamArray(i, (int)i);
        }
        for(c += ft; i < c; i++){
            Player.gc.writeTeamArray(i, (int)i*w);
        }
        for(c += ff; i < c; i++){
            Player.gc.writeTeamArray(i, (int)(i*w+i));
        }
    }

    //helper methods
    private static short root(short[] id, short i){
        while (i != id[i]){
            id[i] = id[id[i]];
            i = id[i];
        }
        return i;
    }
    private static void unite(short[] id, int[] karb, short[] sz, short p, short q){
        short i = root(id, p);
        short j = root(id, q);
        if(sz[i] < sz[j]){
            id[i] = j;
            sz[j] += sz[i];
            karb[j] += karb[i];
        }
        else{
            id[j] = i;
            sz[i] += sz[j];
            karb[i] += karb[j];
        }
    }
    private static short[][] cloneMat(short[][] mat){
        short[][] out = new short[mat.length][];
        for(int i = 0; i < mat.length; i++) {
            short[] row = mat[i];
            int len = row.length;
            out[i] = new short[len];
            System.arraycopy(row, 0, out[i], 0, len);
        }
        return out;
    }
    private static int[][] cloneMat(int[][] mat){
        int[][] out = new int[mat.length][];
        for(int i = 0; i < mat.length; i++) {
            int[] row = mat[i];
            int len = row.length;
            out[i] = new int[len];
            System.arraycopy(row, 0, out[i], 0, len);
        }
        return out;
    }
}

//Quazimondo fast gaussian blur:
//http://incubator.quasimondo.com/processing/gaussian_blur_1.php
class Convolver{
    int radius;
    int kernelSize;
    int[] kernel;
    int[][] multiples;

    Convolver(int sz){
        this.setRadius(sz);
    }

    void setRadius(int sz){

        int i,j;
        sz=Math.min(Math.max(1,sz),248);
        if (radius==sz) return;
        kernelSize=1+sz*2;
        radius=sz;
        kernel=new int[1+sz*2];
        multiples=new int[1+sz*2][256];

        for (i=1;i<sz;i++){
            int szi=sz-i;
            kernel[sz+i]=kernel[szi]=szi;
            for (j=0;j<256;j++){
                multiples[sz+i][j]= multiples[szi][j]=kernel[szi]*j;
            }
        }
        kernel[sz]=sz;
        for (j=0;j<256;j++){
            multiples[sz][j]=kernel[sz]*j;
        }
    }

    int[][] blur(short[][] img){

        int sum,c;
        int i,ri,xl,xi,yl,yi,ym;
        int iw=img[0].length;
        int ih=img.length;

        int img2[][]=new int[ih][iw];
        int img3[][]=new int[ih][iw];

        yi=0;

        for (yl=0;yl<ih;yl++){
            for (xl=0;xl<iw;xl++){
                c=sum=0;
                ri=xl-radius;
                for (i=0;i<kernelSize;i++){
                    xi=ri+i;
                    if (xi>=0 && xi<iw){
                        c+= multiples[i][255*img[yi][xi]];
                    }
                    sum+=kernel[i];
                }
                if(sum == 0)
                    img2[yi][xl]=0;
                else
                    img2[yi][xl]=c/sum;
            }
            yi++;
        }
        yi=0;

        for (yl=0;yl<ih;yl++){
            ym=yl-radius;
            for (xl=0;xl<iw;xl++){
                c=sum=0;
                ri=ym;
                for (i=0;i<kernelSize;i++){
                    if (ri<ih && ri>=0){
                        c+= multiples[i][img2[ri][xl]];
                    }
                    sum+=kernel[i];
                    ri++;
                }
                if(sum == 0)
                    img3[yi][xl] = 0;
                else
                    img3[yi][xl]=c/sum;
            }
            yi++;
        }
        return img3;
    }
}

//https://stackoverflow.com/questions/2670982/using-pairs-or-2-tuples-in-java
class Tuple<X, Y> {
    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}

class mapLocationComparator implements Comparator<Tuple<MapLocation, Integer>> {
    public int compare(Tuple<MapLocation, Integer> a, Tuple<MapLocation, Integer> b){
        return a.y - b.y;
    }
}
