package MapTools;

import bc.*;
import java.util.ArrayList;

public class Karbonite {
    private static GameController gc;
    private static short[][] karboniteMatEarth;
    private static int karboniteTotalEarth;
    private static ArrayList<short[][]> karbonite3dMat; //(should take .5 MB max)
    private static ArrayList<Integer> karboniteTotalArray, karboniteRoundArray;

    public static void setup(GameController gameC, short[][] passM){
        gc = gameC;
        karboniteMat();
        karbonite3dMat(passM);
    }
    public static short[][] matrix(Planet p, int round){
        if(p == Planet.Earth){
            return karboniteMatEarth;
        }
        else {
            if (round < 1) round = 1;
            else if (round > 1000) round = 1000;
            int i = Utils.Misc.binarySearch(karboniteRoundArray, round);
            if(karboniteRoundArray.get(i) != round) i--;
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
            if(karboniteRoundArray.get(i) != round) i--;
            return karboniteTotalArray.get(i);
        }
    }

    private static void karbonite3dMat(short[][] passMat){
        //third dimension is time
        //modify passMat
        short[][] passMat2 = Utils.Misc.cloneMat(passMat);
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
        AsteroidPattern asteroids = gc.asteroidPattern();
        short[][] cumulativeMat = new short[(int)gc.startingMap(Planet.Mars).getHeight()][(int)gc.startingMap(Planet.Mars).getWidth()];
        int cumulative = 0;
        karbonite3dMat = new ArrayList<>();
        karboniteTotalArray = new ArrayList<>();
        karboniteRoundArray = new ArrayList<>();
        if(!asteroids.hasAsteroid(1)) {
            karbonite3dMat.add(Utils.Misc.cloneMat(cumulativeMat));
            karboniteTotalArray.add(cumulative);
            karboniteRoundArray.add(1);
        }
        for(int round = 0; round < 1000; round++){
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
    private static void karboniteMat(){
        //same outputs as PlanetMap.initialKarboniteAt
        PlanetMap earth = gc.startingMap(Planet.Earth);
        karboniteMatEarth = new short[(int)earth.getHeight()][(int)earth.getWidth()];
        karboniteTotalEarth = 0;
        for(int y = 0; y < earth.getHeight(); y++){
            for(int x = 0; x < earth.getWidth(); x++){
                long karb = earth.initialKarboniteAt(new MapLocation(earth.getPlanet(), x, y));
                karboniteMatEarth[y][x] = (short)karb;
                karboniteTotalEarth += karb;
            }
        }
        System.out.println("Earth total karbonite: " + karboniteTotalEarth);
    }
}
