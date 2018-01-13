// import the API.
// See xxx for the javadocs.
import bc.*;

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

    public static MapLocation baseLocation(int[][] opennessMat, Planet planet, VecUnit workers){
        //WIP
        double max = 0;
        int maxX = 0;
        int maxY = 0;
        MapLocation workerLocs[] = new MapLocation[(int)workers.size()];
        for(int i = 0; i < workers.size(); i++){
            workerLocs[i] = workers.get(i).location().mapLocation();
        }
        for(int y = 0; y < opennessMat.length; y++){
            for(int x = 0; x < opennessMat[0].length; x++){
                //weight:
                //openness +255
                //distance -50
                MapLocation loc = new MapLocation(planet, x, y);
                double val = 0;
                for(MapLocation workerLoc : workerLocs){
                    val += Math.sqrt(workerLoc.distanceSquaredTo(loc));
                }
                val /= workerLocs.length;
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
    public static PriorityQueue<MapLocation> baseFactoryQueue(MapLocation baseLocation, short[][] baseMat, short[][] passabilityMat, long[][] karboniteMat){
        //Returns a priorityQueue of locations to build factories
        //Priority based on distance to center and lost Karbonite
        //Locations guaranteed to be valid

        PriorityQueue<MapLocation> pq = new PriorityQueue<>();

        int rx = (baseMat[0].length+1) / 2;
        int ry = (baseMat.length+1) / 2;

        for(int y = baseLocation.getY()-ry; y <= baseLocation.getY()+ry; y++){
            for(int x = baseLocation.getX()-rx; x <= baseLocation.getX()+rx; x++){
                if(baseMat[y][x] == 1){
                    MapLocation loc = new MapLocation(baseLocation.getPlanet(), x, y);
                    pq.add(loc/*, loc.distanceSquaredTo(baseLocation)+1*/);
                }
            }
        }
        return null;
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