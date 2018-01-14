// import the API.
// See xxx for the javadocs.
import bc.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

class MapAnalysis {
    //coordinate system:
    //(0, 0) in bottom left (south-west) corner
    //y increases upwards (northward)
    //x increases to the right (eastward)

    //TODO: (if you want me to do something, you can add it here)
    //Rocket landing locations
    //Asteroid pattern analysis
    //First factory placement (for fast rollout)

    //issues:
    //If workers aren't all on one side of the map, the initial base placement will be next to the center
    //Initial base placement does not look at karbonite

    //public variables
    //feel free to use them
    static short[][] passabilityMat, passabilityMatEarth, passabilityMatMars;
    static long[][] karboniteMat;
    static long karboniteTotal;
    //Earth only
    static MapLocation baseLocation;
    static PriorityQueue<Tuple<MapLocation, Integer>> factoryQueue;

    //private variables
    private static ArrayList<short[][]> karbonite3dMat; //(should take .5 MB max)
    private static ArrayList<Integer> karboniteTotalArray;
    private static ArrayList<Integer> karboniteRoundArray;
    private static short[][] smallBase =
        {{1, 1, 0, 1, 1},
        {1, 1, 0, 1, 1},
        {0, 0, 0, 0, 0},
        {1, 1, 0, 1, 1},
        {1, 1, 0, 1, 1}};

    //public methods
    //feel free to use them
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
    public static MapLocation rocketTarget(boolean aggressive){
        //returns a most suitable rocket landing location
        //aggressive:
        //false: send to a safe location next to friendly units
        //true: bombard enemy base

        //TODO
        return null;
    }
    //methods for Player
    public static void setup(){
        passabilityMatEarth = passabilityMat(Player.mapEarth);
        passabilityMatMars = passabilityMat(Player.mapMars);
        karboniteMat();
        karbonite3dMat();
        if (Player.gc.planet() == Planet.Earth) {
            passabilityMat = passabilityMatEarth;

            Convolver c4 = new Convolver(4);
            baseLocation(opennnesMat(passabilityMatEarth, c4));
            System.out.println("Base location: " + baseLocation.getX() + ", " + baseLocation.getY());

            baseFactoryQueue(baseLocation, smallBase);
        }
        else{
            passabilityMat = passabilityMatMars;
        }
    }

    //private methods
    private static short[][] passabilityMat(PlanetMap pm){
        //same outputs as PlanetMap.isPassableTerrainAt
        short[][] out = new short[(int)pm.getHeight()][(int)pm.getWidth()];
        for(int y = 0; y < pm.getHeight(); y++){
            for(int x = 0; x < pm.getWidth(); x++){
                out[y][x] = pm.isPassableTerrainAt(new MapLocation(pm.getPlanet(), x, y));
            }
        }
        return out;
    }
    private static int[][] opennnesMat(short[][] passMat, Convolver c4){
        //0 is completely occupied
        //255 is completely open
        c4.setRadius(4);
        return c4.blur(passMat);
    }
    //Earth only
    private static void karboniteMat(){
        //same outputs as PlanetMap.initialKarboniteAt
        karboniteMat = new long[(int)Player.mapEarth.getHeight()][(int)Player.mapEarth.getWidth()];
        karboniteTotal = 0;
        for(int y = 0; y < Player.mapEarth.getHeight(); y++){
            for(int x = 0; x < Player.mapEarth.getWidth(); x++){
                long karb = Player.mapEarth.initialKarboniteAt(new MapLocation(Player.mapEarth.getPlanet(), x, y));
                karboniteMat[y][x] = karb;
                karboniteTotal += karb;
            }
        }
        System.out.println("Earth total karbonite: " + karboniteTotal);
    }
    private static void baseLocation(int[][] opennessMat){
        //Returns suitable location for a base
        //Based on available space and distance from initial workers
        double max = 0;
        int maxX = 0;
        int maxY = 0;
        VecUnit workers = Player.mapEarth.getInitial_units();
        ArrayList<MapLocation> workerLocs = new ArrayList<>();
        for(int i = 0; i < workers.size(); i++){
            Unit worker = workers.get(i);
            if(worker.team() == Player.gc.team()) {
                workerLocs.add(worker.location().mapLocation());
            }
        }
        for(int y = 0; y < opennessMat.length; y++){
            for(int x = 0; x < opennessMat[0].length; x++){
                //weight:
                //openness +[0, 255]   +openness
                //distance -[0, 100]   -5*linear_dist, 30 blocks = -150, caps at -150
                MapLocation loc = new MapLocation(Planet.Earth, x, y);
                double val = 0;
                for(MapLocation workerLoc : workerLocs){
                    val -= Math.sqrt(workerLoc.distanceSquaredTo(loc));
                }
                val /= workerLocs.size();
                val = Math.max(-30, val);
                val *= 5;
                val += opennessMat[y][x];
                if(val > max){
                    max = val;
                    maxX = x;
                    maxY = y;
                }
            }
        }
        baseLocation = new MapLocation(Planet.Earth, maxX, maxY);
    }
    private static void baseFactoryQueue(MapLocation baseLocation, short[][] baseMat){
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
                if(baseMat[yi][xi]==1 && x>=0 && y>=0 && x<passabilityMatEarth[0].length && y<passabilityMatEarth.length && passabilityMatEarth[y][x]==1){
                    MapLocation loc = new MapLocation(Planet.Earth, x, y);
                    factoryQueue.add(new Tuple<>(loc, (int)(5*Math.sqrt(loc.distanceSquaredTo(baseLocation)))+(int)karboniteMat[y][x]));
                }
            }
        }
    }
    //Mars only
    private static void karbonite3dMat(){
        //third dimension is time
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
            cumulative += asteroid.getKarbonite();
            karbonite3dMat.add(cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(round);
        }
        System.out.println("Mars total karbonite: " + cumulative);
    }

    //helper methods
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