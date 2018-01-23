package MapTools;

import bc.*;
import java.util.*;

public class UnionFind {
    private static GameController gc;

    private static Set<Integer> compEarth, compMars;
    private static int[] karbEarth, karbMars;
    private static short[] idEarth, idMars;
    private static short[] szEarth, szMars;

    public static void setup(GameController gameC, int[][] passE, int[][] passM, int[][] karbE, int[][] karbM){
        gc = gameC;
        unionFind(Planet.Earth, passE, karbE);
        unionFind(Planet.Mars, passM, karbM);
    }
	/**
	 * Gives the collection of independent components on a given planet
	 * @param p Planet in question
	 * @return Set of components' ids
	 *     <br>Please don't modify it directly
	 */
	public static Set<Integer> components(Planet p){
		if(p == Planet.Earth)
			return compEarth;
		return compMars;
	}
	public static boolean connect(Planet p, int id1, int id2){
		return id1 == id2;
	}
    public static boolean connect(Planet p, int x1, int y1, int x2, int y2){
        int w;
        short[] id;
        if(p == Planet.Earth){
            w = (int)gc.startingMap(Planet.Earth).getWidth();
            id = idEarth;
        }
        else{
            w = (int)gc.startingMap(Planet.Earth).getWidth();
            id = idMars;
        }
        return root(id, (short)(y1*w+x1)) == root(id, (short)(y2*w+x2));
    }
    public static int size(Planet p, int id){
    	if(p == Planet.Earth)
    		return szEarth[id];
    	return szMars[id];
    }
    public static int size(Planet p, int x, int y){
        int w;
        short[] id, sz;
        if(p == Planet.Earth){
            w = (int)gc.startingMap(Planet.Earth).getWidth();
            id = idEarth;
            sz = szEarth;
        }
        else{
            w = (int)gc.startingMap(Planet.Mars).getWidth();
            id = idMars;
            sz = szMars;
        }
        return sz[root(id, (short)(y*w+x))];
    }
    public static int karbonite(Planet p, int id){
	    if(p == Planet.Earth)
		    return karbEarth[id];
	    return karbMars[id];
    }
    public static int karbonite(Planet p, int x, int y){
        int w;
        short[] id;
        int[] karb;
        if(p == Planet.Earth){
            w = (int)gc.startingMap(Planet.Earth).getWidth();
            id = idEarth;
            karb = karbEarth;
        }
        else{
            w = (int)gc.startingMap(Planet.Mars).getWidth();
            id = idMars;
            karb = karbMars;
        }
        return karb[root(id, (short)(y*w+x))];
    }
	public static int id(Planet p, int x, int y){
		int w;
		short[] id;
		if(p == Planet.Earth){
			w = (int)gc.startingMap(Planet.Earth).getWidth();
			id = idEarth;
		}
		else{
			w = (int)gc.startingMap(Planet.Mars).getWidth();
			id = idMars;
		}
		return root(id, (short)(w*y+x));
	}

    private static void unionFind(Planet p, int[][] passMat, int[][] karbMat){
        //Union-find:
        //https://www.cs.princeton.edu/~rs/AlgsDS07/01UnionFind.pdf
        //add nodes
        int w = passMat[0].length;
        int N = passMat.length*w;
        short[] id = new short[N];
        int[] karb = new int[N];
        short[] sz = new short[N];
        HashSet<Integer> comp = new HashSet<>();
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
        for(int y = 0; y < passMat.length; y++){
        	for(int x = 0; x < w; x++){
		        int k = karbMat[y][x];
		        Set<Integer> seen = new HashSet<>(4);
        		if(k > 0 && passMat[y][x] == 0){
			        for(int dy = -1; dy <= 1; dy++){
				        if (y + dy < 0 || y + dy >= passMat.length) continue;
				        for (int dx = -1; dx <= 1; dx++) {
					        if (x + dx < 0 || x + dx >= w) continue;
					        if(passMat[y + dy][x + dx] == 0) continue;
					        int r = root(id, (short)(w*(y+dy)+x+dx));
							if(seen.contains(r)) continue;
							seen.add(r);
					        karb[r] += k;
				        }
			        }
		        }
	        }
        }
	    //compute components
	    for(short i = 0; i < N; i++){
        	if(passMat[i/w][i%w] == 0)
        		continue;
        	int r = root(id, i);
        	if(!comp.contains(r))
        		comp.add(r);
	    }
        if(p == Planet.Earth){
            idEarth = id;
            karbEarth = karb;
            szEarth = sz;
            compEarth = comp;
        }
        else{
            idMars = id;
            karbMars = karb;
            szMars = sz;
            compMars = comp;
        }

    }
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
}
