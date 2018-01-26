package MapTools;

import Utils.Tuple;
import bc.*;
import java.util.ArrayList;

public class Karbonite {
    private static GameController gc;
    private static int[][] karboniteMatEarth;
    private static int karboniteTotalEarth;
    private static ArrayList<int[][]> karbonite3dMat; //(should take .5 MB max)
    private static ArrayList<Integer> karboniteTotalArray, karboniteRoundArray;

    public static void setup(GameController gameC, int[][] passM){
        gc = gameC;
        Tuple<int[][], Integer> temp = karboniteMat(Planet.Earth);
        karboniteMatEarth = temp.x;
        karboniteTotalEarth = temp.y;
        System.out.println("Earth total karbonite: " + karboniteTotalEarth);
        karbonite3dMat(passM, karboniteMat(Planet.Mars));
    }
    public static int[][] matrix(Planet p, int round){
        if(p == Planet.Earth){
            return karboniteMatEarth;
        }
        else {
            if (round < 1) round = 1;
            else if (round > 1000) round = 1000;
            int i = Utils.Misc.binarySearch(karboniteRoundArray, round);
            if(i >= karboniteRoundArray.size() || karboniteRoundArray.get(i) != round) i--;
            return karbonite3dMat.get(i);
        }
    }
    public static int total(Planet p, int round){
        if(p == Planet.Earth){
            return karboniteTotalEarth;
        }
        else {
            if (round < 1) round = 1;
            else if (round > 1000) round = 1000;
            int i = Utils.Misc.binarySearch(karboniteRoundArray, round);
            if(i >= karboniteRoundArray.size() || karboniteRoundArray.get(i) != round) i--;
            return karboniteTotalArray.get(i);
        }
    }

    private static void karbonite3dMat(int[][] passMat, Tuple<int[][], Integer> t){
        //third dimension is time
        //modify passMat
        int[][] passMat2 = Utils.Misc.cloneMat(passMat);
        for(int y = 0; y < passMat.length; y++){
            for(int x = 0; x < passMat[0].length; x++){
                if(passMat[y][x] != 0) continue;
                kernel:
                for(int xi = -1; xi <= 1; xi++){
                    for(int yi = -1; yi <= 1; yi++){
                        if(y+yi < 0 || y+yi >= passMat.length || x+xi < 0 || x+xi >= passMat[0].length) continue;
                        if(passMat[y+yi][x+xi] == 1){
                            passMat2[y][x] = 1;
                            break kernel;
                        }
                    }
                }
            }
        }
        //process asteroids
        AsteroidPattern asteroids = gc.asteroidPattern();
        int[][] cumulativeMat = t.x;
        int cumulative = t.y;
        karbonite3dMat = new ArrayList<>();
        karboniteTotalArray = new ArrayList<>();
        karboniteRoundArray = new ArrayList<>();
        if(!asteroids.hasAsteroid(1)) {
            karbonite3dMat.add(Utils.Misc.cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(1);
        }
        for(int round = 1; round <= 1000; round++){
            if(!asteroids.hasAsteroid(round))
                continue;
            AsteroidStrike asteroid = asteroids.asteroid(round);
            cumulativeMat[asteroid.getLocation().getY()][asteroid.getLocation().getX()] += asteroid.getKarbonite();
            if(passMat2[asteroid.getLocation().getY()][asteroid.getLocation().getX()] == 1) cumulative += asteroid.getKarbonite();
            karbonite3dMat.add(Utils.Misc.cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(round);
        }
        System.out.println("Mars total reachable karbonite: " + cumulative);
    }
    private static Tuple<int[][], Integer> karboniteMat(Planet p){
        //same outputs as PlanetMap.initialKarboniteAt
        PlanetMap pm = gc.startingMap(p);
        int[][] mat = new int[(int)pm.getHeight()][(int)pm.getWidth()];
        int total = 0;
        for(int y = 0; y < pm.getHeight(); y++){
            for(int x = 0; x < pm.getWidth(); x++){
                long karb = pm.initialKarboniteAt(new MapLocation(p, x, y));
                mat[y][x] = (short)karb;
                total += karb;
            }
        }
        return new Tuple<>(mat, total);
    }
}
