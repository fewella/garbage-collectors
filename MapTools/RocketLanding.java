package MapTools;

import Utils.*;
import bc.*;

import java.net.PasswordAuthentication;
import java.util.*;

public class RocketLanding {
	//maximum capacity of chambers
	//when reached, rockets are sent to other chambers
	private static final double MAX_CAP = 0.5;
	//number of options
	private static final int SZ = 2;

    private static GameController gc;
    private static int deliveryId, orderId;
    private static short[] received, used;
    //Earth only
    private static Queue<MapLocation> q0, q1;
    private static int[][] occupied;
    //Mars only
	private static int firstContact;
	//unit chamber arrays
    public static void setup(GameController gameC, Utils.Convolver c){
    	//IMPORTANT! make sure Karbonite, Passable, and UnionFind have been set up!
	    gc = gameC;
	    deliveryId = orderId = 0;
        received = new short[SZ];
        used = new short[SZ];
        if(gc.planet() == Planet.Earth){
	        //blurring
	        int[][] passM = Passable.matrix(Planet.Mars);
	        int[][] spreadMat = new int[passM.length][passM[0].length];
	        for(int y = 1; y < passM.length; y+=3){
		        for(int x = 1; x < passM[0].length; x+=3){
			        spreadMat[y][x] = 1;
		        }
	        }
	        int[][] karb = Karbonite.matrix(Planet.Mars, 743);
	        int[][] blurKarb = c.blur(Karbonite.matrix(Planet.Mars, 743));
        	computeDefault(karb, blurKarb, spreadMat);
	        occupied = new int[passM.length][];
	        for(int y = 0; y < passM.length; y++)
	        	occupied[y] = new int[passM[0].length];
        }
        else{
        	firstContact = 0;
        }
    }
    public static void turn(){
        Veci32 earth = gc.getTeamArray(Planet.Earth);
        Veci32 mars = gc.getTeamArray(Planet.Mars);
        if(gc.planet() == Planet.Earth){
            //listen to delivery
            int newDeliveryId = mars.get(0);
            if(deliveryId != newDeliveryId){
                deliveryId = newDeliveryId;
                received = new short[SZ];
                used = new short[SZ];
                //read delivery details
                for(int i = 0; i < SZ; i++)
                    received[i] = (short)(mars.get(i+1));
            }
        }
        else{
        	if(firstContact == 0){
        		if(gc.myUnits().size() == 0) return;
                firstContact = (int)gc.round();
	        }
            //auto send
            if (gc.round()-firstContact % 50 == 5)
                compute(20, 20);
            //listen to orders
            int newOrderId = earth.get(0);
            if(orderId != newOrderId){
                orderId = newOrderId;
                //process order, send delivery
                compute(earth.get(1), earth.get(2));
            }
        }
    }
    /**
     * Call from Earth to request landing locations. While this is done periodically, calling this 100 turns before
     * requesting locations ensures that the data is most recent (50 turns old), and minimizes the risk of running out of locations.
     * Please make sure the numbers add up to 40.
     * @param option0 number of locations that will be received with requestLandingLocations(0)
     * @param option1 number of locations that will be received with requestLandingLocations(1)
     */
    //Earth only
    public static void request(int option0, int option1){
    	gc.writeTeamArray(0, ++orderId);
        gc.writeTeamArray(1, option0);
        gc.writeTeamArray(2, option1);
    }
    /**
     * Call from Earth to receive a most suitable rocket landing location.
     * For optimal results, call requestLandingLocations() 100 turns before calling this.
     * Also, if taking a location, please actually send a rocket to it.
     * @param option <br>0: cover more ground, prioritize karbonite (for worker rockets and for covering ground)
     *               <br>1: prioritize proximity of allies (for transferring units from Earth, supporting allies)
     * @return MapLocation of the best landing location
     */
    //Earth only
    public static MapLocation retrieve(int option){
        if(/*received[option] <= used[option]*/true){
            //System.out.println("No landing locations for option " + option + ". Taking from default.");
            Queue<MapLocation> q, notQ;
            if(option == 0){
            	q = q0;
            	notQ = q1;
            }
            else{
            	q = q1;
            	notQ = q0;
	        }
	        while(!q.isEmpty()){
		        MapLocation loc = q.remove();
		        if(occupied[loc.getY()][loc.getX()] == 0){
			        occupied[loc.getY()][loc.getX()] = 1;
			        return loc;
		        }
		        if(q.isEmpty()) {
			        System.out.println("Out of defaults for option " + option + ". Taking from other option.");
			        q = notQ;
		        }
	        }
            System.out.println("Out of all default locations. Finding random suitable location.");
            int[][] passMars = Passable.matrix(Planet.Mars);
            PlanetMap mars = gc.startingMap(Planet.Mars);
            for(int i = 0; i < 20; i++){
	            int x = (int)(mars.getWidth()*Math.random());
	            int y = (int)(mars.getHeight()*Math.random());
                if(passMars[y][x] == 0)
                    continue;
                return new MapLocation(Planet.Mars, x, y);
            }
            System.out.print("Can't find a suitable location. Returning 10, 10.");
            return new MapLocation(Planet.Mars, 10, 10);
        }
        int i2 = 1+SZ+used[option]++;
        for(int j = 0; j < option; j++) i2 += received[j];
        int val = gc.getTeamArray(Planet.Mars).get(i2);

        int w = (int)gc.startingMap(Planet.Mars).getWidth();
        return new MapLocation(Planet.Mars, val/w, val%w);
    }

    //Earth only
    private static void computeDefault(int[][] karb, int[][] blurKarb, int[][] spreadMat){
	    //create queues, sort chambers
	    //q0 stuff
	    q0 = new LinkedList<>();
	    Set<Integer> temp = UnionFind.components(Planet.Mars);
	    Map<Integer, PriorityQueue<Tuple<MapLocation, Integer>>> qs = new HashMap<>(temp.size());
	    Map<Integer, Integer> sz = new HashMap<>(temp.size());
	    PriorityQueue<Tuple<Integer, Integer>> comps = new PriorityQueue<>(temp.size(), new CustomComparator());
	    PriorityQueue<Tuple<Integer, Double>> comps2 = new PriorityQueue<>(temp.size(), new CustomComparator2());
	    //q1 stuff
	    q1 = new LinkedList<>();
	    Map<Integer, PriorityQueue<Tuple<MapLocation, Integer>>> qs2 = new HashMap<>(temp.size());
	    Map<Integer, Integer> sz2 = new HashMap<>(temp.size());
	    PriorityQueue<Tuple<Integer, Integer>> comps3 = new PriorityQueue<>(temp.size(), new CustomComparator());
	    PriorityQueue<Tuple<Integer, Double>> comps4 = new PriorityQueue<>(temp.size(), new CustomComparator2());

	    for(int id : temp) {
	    	qs.put(id, new PriorityQueue<>(new CustomComparator3()));
	    	qs2.put(id, new PriorityQueue<>(new CustomComparator3()));
	    	sz.put(id, 0);
	    	sz2.put(id, 0);
		    comps.add(new Tuple<>(id, UnionFind.karbonite(Planet.Mars, id)));
		    comps3.add(new Tuple<>(id, 5*UnionFind.size(Planet.Mars, id)+UnionFind.karbonite(Planet.Mars, id)));
	    }

	    //create individual queues
    	PlanetMap mars = gc.startingMap(Planet.Mars);
        for (int y = 0; y < mars.getHeight(); y++) {
            for (int x = 0; x < mars.getWidth(); x++) {
                if (MapTools.Passable.matrix(Planet.Mars)[y][x] == 0) continue;
                MapLocation loc = new MapLocation(Planet.Mars, x, y);
                int id = UnionFind.id(Planet.Mars, x, y);
                qs.get(id).add(new Tuple<>(loc, 10 * spreadMat[y][x] + blurKarb[y][x] - karb[y][x]));
                qs2.get(id).add(new Tuple<>(loc, blurKarb[y][x] - karb[y][x]));
            }
        }

        //merge queues
	    while(!comps.isEmpty()){
			Tuple<Integer, Integer> comp = comps.remove();
			int id = comp.x;
			PriorityQueue<Tuple<MapLocation, Integer>> q = qs.get(id);
			q0.add(q.remove().x);
			int n = sz.get(id);
			sz.put(id, ++n);
			double cap = 8.0*n/UnionFind.size(Planet.Mars, id);
			if(cap < MAX_CAP)
				comps.add(new Tuple<>(id, comp.y-200));
			else if(!q.isEmpty())
				comps2.add(new Tuple<>(id, cap));
	    }
	    while(!comps3.isEmpty()){
		    Tuple<Integer, Integer> comp = comps3.remove();
		    int id = comp.x;
	        int n = 0;
	        double cap;
	        PriorityQueue<Tuple<MapLocation, Integer>> q = qs2.get(id);
	        do {
		        cap = 8.0 * ++n / UnionFind.size(Planet.Mars, id);
		        q1.add(q.remove().x);
	        }
	        while(cap < MAX_CAP);
		    sz2.put(id, n);
		    if(!q.isEmpty())
		        comps4.add(new Tuple<>(id, cap));
	    }
	    //further merging
	    while(!comps2.isEmpty()){
		    Tuple<Integer, Double> comp = comps2.remove();
		    int id = comp.x;
		    PriorityQueue<Tuple<MapLocation, Integer>> q = qs.get(id);
		    q0.add(q.remove().x);
		    int n = sz.get(id);
		    sz.put(id, ++n);
		    if(!q.isEmpty())
			    comps2.add(new Tuple<>(id, 8.0*n/UnionFind.size(Planet.Mars, id)));
	    }
	    while(!comps4.isEmpty()){
		    Tuple<Integer, Double> comp = comps4.remove();
		    int id = comp.x;
		    PriorityQueue<Tuple<MapLocation, Integer>> q = qs2.get(id);
		    q1.add(q.remove().x);
		    int n = sz2.get(id);
		    sz2.put(id, ++n);
		    if(!q.isEmpty())
			    comps4.add(new Tuple<>(id, 8.0*n/UnionFind.size(Planet.Mars, id)));
	    }
    }
    //Mars only
    private static void compute(int o0, int o1){
        //watch for improper arguments
        int total = o0+o1;
        if(total > 40) {
            System.out.println("Number of total requested locations (" + total + ") exceeds 40");
            o0 = o1 = 20;
        }
        gc.writeTeamArray(0, ++deliveryId);
        gc.writeTeamArray(1, o0);
        gc.writeTeamArray(2, o1);
        //compute best locations
        //TODO
        long c = 1+SZ;
        long i = c;
        int w = (int)gc.startingMap(Planet.Mars).getWidth();
        for(c += o0; i < c; i++){
            gc.writeTeamArray(i, 0);
        }
        for(c += o1; i < c; i++){
            gc.writeTeamArray(i, (int)i);
        }
        System.out.println("round " + gc.round() + ": Locations sent");
    }
}
class CustomComparator implements Comparator<Tuple<Integer, Integer>> {
	public int compare(Tuple<Integer, Integer> a, Tuple<Integer, Integer> b){
		return b.y - a.y;
	}
}
class CustomComparator2 implements Comparator<Tuple<Integer, Double>> {
	public int compare(Tuple<Integer, Double> a, Tuple<Integer, Double> b){
		return a.y.compareTo(b.y);
	}
}
class CustomComparator3 implements Comparator<Tuple<MapLocation, Integer>> {
	public int compare(Tuple<MapLocation, Integer> a, Tuple<MapLocation, Integer> b){
		return b.y - a.y;
	}
}