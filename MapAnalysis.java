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

    public static short[][] smallBase =
    {{1, 1, 0, 1, 1},
    {1, 1, 0, 1, 1},
    {0, 0, 0, 0, 0},
    {1, 1, 0, 1, 1},
    {1, 1, 0, 1, 1}};

    public static short[][] passabilityMat(PlanetMap map){
        //same outputs as PlanetMap.isPassableTerrainAt
        short[][] out = new short[(int)map.getHeight()][(int)map.getWidth()];
        for(int y = 0; y < map.getHeight(); y++){
            for(int x = 0; x < map.getWidth(); x++){
                out[y][x] = map.isPassableTerrainAt(new MapLocation(map.getPlanet(), x, y));
            }
        }
        return out;
    }
    public static long[][] karboniteMat(PlanetMap map){
        //same outputs as PlanetMap.initialKarboniteAt
        long[][] out = new long[(int)map.getHeight()][(int)map.getWidth()];
        for(int y = 0; y < map.getHeight(); y++){
            for(int x = 0; x < map.getWidth(); x++){
                out[y][x] = map.initialKarboniteAt(new MapLocation(map.getPlanet(), x, y));
            }
        }
        return out;
    }
    public static int[][] opennnesMat(short[][] passabilityMap, Convolver c4){
        //0 is completely occupied
        //255 is completely open
        c4.setRadius(4);
        return c4.blur(passabilityMap);
    }

    public static MapLocation baseLocation(int[][] opennessMat, Planet planet, Team team, VecUnit workers){
        //Returns suitable location for a base
        //Based on available space and distance from initial workers
        double max = 0;
        int maxX = 0;
        int maxY = 0;
        ArrayList<MapLocation> workerLocs = new ArrayList<>();
        for(int i = 0; i < workers.size(); i++){
            Unit worker = workers.get(i);
            if(worker.team() == team) {
                workerLocs.add(worker.location().mapLocation());
            }
        }
        for(int y = 0; y < opennessMat.length; y++){
            for(int x = 0; x < opennessMat[0].length; x++){
                //weight:
                //openness +[0, 255]   +openness
                //distance -[0, 100]   -5*linear_dist, 30 blocks = -150, caps at -150
                MapLocation loc = new MapLocation(planet, x, y);
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
        return new MapLocation(planet, maxX, maxY);
    }
    public static PriorityQueue<Tuple<MapLocation, Integer>> baseFactoryQueue(MapLocation baseLocation, short[][] baseMat, short[][] passabilityMat, long[][] karboniteMat){
        //Returns a priorityQueue of locations to build factories
        //Priority based on distance to center and lost Karbonite
        //Locations guaranteed to be valid

        PriorityQueue<Tuple<MapLocation, Integer>> pq = new PriorityQueue<>(16, new mapLocationComparator());

        int xtemp = baseLocation.getX()-baseMat[0].length/2;
        int ytemp = baseLocation.getY()-baseMat.length/2;

        for(int yi = 0; yi < baseMat.length; yi++){
            for(int xi = 0; xi < baseMat[0].length; xi++){
                int x = xtemp+xi;
                int y = ytemp+yi;
                if(baseMat[yi][xi]==1 && x>=0 && y>=0 && x<passabilityMat[0].length && y<passabilityMat.length && passabilityMat[y][x]==1){
                    MapLocation loc = new MapLocation(baseLocation.getPlanet(), x, y);
                    pq.add(new Tuple<>(loc, (int)(5*Math.sqrt(loc.distanceSquaredTo(baseLocation)))+(int)karboniteMat[y][x]));
                }
            }
        }
        return pq;
    }
}

//Quazimondo fast gaussian blur:
//http://incubator.quasimondo.com/processing/gaussian_blur_1.php

class Convolver{
    int radius;
    int kernelSize;
    int[] kernel;
    int[][] mult;

    Convolver(int sz){
        this.setRadius(sz);
    }

    void setRadius(int sz){

        int i,j;
        sz=Math.min(Math.max(1,sz),248);
        if (radius==sz) return;
        kernelSize=1+sz*2;
        radius=sz;
        kernel= new int[1+sz*2];
        mult=new int[1+sz*2][256];

        for (i=1;i<sz;i++){
            int szi=sz-i;
            kernel[sz+i]=kernel[szi]=szi;
            for (j=0;j<256;j++){
                mult[sz+i][j]=mult[szi][j]=kernel[szi]*j;
            }
        }
        kernel[sz]=sz;
        for (j=0;j<256;j++){
            mult[sz][j]=kernel[sz]*j;
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
                        c+=mult[i][255*img[yi][xi]];
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
                        c+=mult[i][img2[ri][xl]];
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
