package MapTools;

import Utils.*;
import bc.*;

import java.util.*;

public class Rollout {
	private static final int ACT_NUM = 45;

	private static Direction[] dirs;
	private static GameController gc;
	private static PlanetMap earth;
	private static Convolver c4;
	private static int[][] enemies, allies, occupied, pass, open, neigh;

	public static Map<Integer, Integer> initIds;   //id, fact id
	public static Queue<Unit> normWork;

	//updateQueue
	public static Map<Integer, Integer> factTime; //id, round
	public static Queue<Tuple<Integer, Integer>> purchQ;   //id, type
	public static Map<Integer, Integer> status; //id, actions
	public static Map<Integer, Integer> works;  //id, num
	//locs
	public static Map<Integer, MapLocation> factLocs;  //id, loc; may not contain if all occupied
	public static int[][] factBFS;
	public static Map<Integer, Integer> pathDist;  //id, dist
	public static Map<Integer, Integer> workNum;   //id, number

	//Needs: Passable, Karbonite, UnionFind
	public static void setup(GameController gameC, Convolver c, int[][] e, int[][] a, int[][] o, Map<Integer, ArrayList<Unit>> groups, Map<Integer, int[][]> BFS){
		gc = gameC;
		earth = gc.startingMap(Planet.Earth);
		dirs = Direction.values();
		c4 = c;
		enemies = e;
		allies = a;
		occupied = o;
		pass = Passable.matrix(Planet.Earth);
		neigh = blurN(pass);
		open = c4.blur(pass, true);

		workNum = new HashMap<>(groups.size());
		for(int id : groups.keySet()){
			ArrayList<Unit> works = groups.get(id);
			if(works.size() == 1)
				workNum.put(id, 1);
			else if(works.size() == 2){
				MapLocation loc1 = works.get(1).location().mapLocation();
				if(BFS.get(works.get(0).id())[loc1.getY()][loc1.getX()] < 5)
					workNum.put(id, 2);
				else
					workNum.put(id, 1);
			}
			else{
				int min = 3;
				for(int i = 0; i < 3; i++) {
					int[][] BFSi = BFS.get(works.get(i).id());
					ArrayList<MapLocation> loci = new ArrayList<>(2);
					for(int j = 0; j < 3; j++){
						if(j == i)
							continue;
						loci.add(works.get(j).location().mapLocation());
					}

					int size = 1;
					for(MapLocation loc : loci){
						if(BFSi[loc.getY()][loc.getX()] < 5) {
							size++;
						}
					}
					if(size < min)
						min = size;
					if(min == 1)
						break;
				}
				workNum.put(id, min);
			}
		}
		status = new HashMap<>(3);
		works = new HashMap<>(3);
		updateQueue(UnionFind.components(Planet.Earth), allies, enemies, workNum, neigh);
		int[][] karb = Karbonite.matrix(Planet.Earth, 1);
		locs(pass, neigh, open, karb, c4.blur(karb, false), enemies, allies, occupied);

		initIds = new HashMap<>(factLocs.size());
	}
	public static Tuple<int[][], ArrayList<MapLocation>> turn(Queue<Unit> worker, int[][] karbMapBFS, ArrayList<MapLocation> dest){
		updateQueue(UnionFind.components(Planet.Earth), allies, enemies, workNum, neigh);
		workNum = new HashMap<>(workNum.size());
		normWork = new LinkedList<>();
		for(Unit u : worker){
			if (!u.location().isOnMap()) {
				continue;
			}
			MapLocation mapLoc = u.location().mapLocation();
			int comp = UnionFind.id(Planet.Earth, mapLoc.getX(), mapLoc.getY());
			if(!factLocs.containsKey(comp)) {
				normWork.add(u);
				continue;
			}
			MapLocation initLoc = factLocs.get(comp);
			double safe = (pathDist.get(comp)-gc.round()/2 - factBFS[mapLoc.getY()][mapLoc.getX()]);
			if(!mapLoc.isAdjacentTo(initLoc) && safe < 0) {
				normWork.add(u);
				continue;
			}
			if(!workNum.containsKey(comp))
				workNum.put(comp, 0);
			workNum.put(comp, workNum.get(comp)+1);

			boolean doneAction = false;
			//initial factory strategy:
			//1. if far, move by matrix
			//2. else: blueprint/build
			//3. if possible, replicate/collect karb
			Collections.shuffle(Arrays.asList(dirs));    //to prevent workers getting stuck
			Tuple<Integer, Integer> t = Rollout.purchQ.peek();

			Team enemy = Team.Blue;
			if(gc.team() == Team.Blue)
				enemy = Team.Red;
			VecUnit vs = gc.senseNearbyUnitsByTeam(mapLoc, u.visionRange(), enemy);
			int count = 0;
			for(int i = 0; i < vs.size(); i++){
				UnitType type = vs.get(i).unitType();
				if(type == UnitType.Ranger || type == UnitType.Knight || type == UnitType.Mage)
					count++;
			}
			if(safe > 1 && count == 0){
				//insert Honor Code here
				int min = 9999;
				int min2 = 9999;
				int dire = -1;
				int dire2 = -1;
				for (int k = 0; k < 8; k++) {
					MapLocation temp = u.location().mapLocation().add(dirs[k]);
					if( temp.getX() < 0 || temp.getX() >= earth.getWidth() || temp.getY() >= earth.getHeight() || temp.getY() < 0 ) continue;
					int movetemp = karbMapBFS[temp.getY()][temp.getX()];
					if( movetemp == -1 || movetemp == 9999 )
						continue;
					if(Math.min(movetemp, min) != min) {
						dire = k;
						min2 = min;
						min = Math.min(movetemp, min);
					}
					else if( Math.min(movetemp, min2) != min2 ) {
						dire2 = k;
						min2 = Math.min(movetemp, min2);
						if( min2 < min ) {
							int t2 = min2;
							min2 = min;
							min = t2;
							int td = dire2;
							dire2 = dire;
							dire = td;
						}
					}
				}
				//System.out.println("Min: " + min + " Min2: " + min2);
				if (min == 0 ) { //0 or 1??
					if(gc.canHarvest(u.id(), dirs[dire])) {
						//if (round > 1 && karb < 300) {
						//System.out.println("Harvesting karbonite");
						gc.harvest(u.id(), dirs[dire]);
						//}
					}
					else {
						//Probably make BFS take a hashset??
						//Remove - it's empty
						ArrayList<MapLocation> temp = new ArrayList<MapLocation>();
						for( MapLocation m: dest) {
							if( !m.equals(u.location().mapLocation().add(dirs[dire])) )
								temp.add(m);
						}
						dest = temp;
						karbMapBFS = Pathing.BFS(dest, true);
					}
				}
				else if( (min != 9999 && min != 0) && dire != -1 ) {
					if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire]) ) {
						gc.moveRobot(u.id(), dirs[dire]);
						//System.out.println("Moving to karbonite");
					}
				}
				else if( min2 != 9999 && dire2 != -1 ) {
					if( min2 != 0 ) {
						if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire2]) ) {
							gc.moveRobot(u.id(), dirs[dire2]);
							//System.out.println("Moving to karbonite");
						}
					}
					else {
						if(gc.canHarvest(u.id(), dirs[dire2])) {
							//if (round > 1 && karb < 300) {
							//System.out.println("Harvesting karbonite");
							gc.harvest(u.id(), dirs[dire]);
							//}
						}
					}
				}
			}

			else{
				if (!mapLoc.isAdjacentTo(initLoc)) {    //out of reach
					//1. move
					if (gc.isMoveReady(u.id())) {
						Direction minD = Direction.Center;
						int min = factBFS[mapLoc.getY()][mapLoc.getX()];
						for (Direction d : dirs) {
							if(d == Direction.Center)
								continue;
							MapLocation newLoc = mapLoc.add(d);
							if (newLoc.getX() < 0 || newLoc.getY() < 0 || newLoc.getX() >= earth.getWidth() || newLoc.getY() >= earth.getHeight())
								continue;
							int newMin = factBFS[newLoc.getY()][newLoc.getX()];
							if (newMin < min && gc.canMove(u.id(), d)) {
								min = newMin;
								minD = d;
							}
						}
						if (gc.canMove(u.id(), minD))
							gc.moveRobot(u.id(), minD);
					}
				} else if(mapLoc.isAdjacentTo(initLoc)) {
					//2. blueprint/build
					try {
						Unit f = gc.senseUnitAtLocation(initLoc);
						if (f.team() != gc.team())
							throw new RuntimeException("ayy lmao");
						//build
						if (gc.canBuild(u.id(), f.id())) {
							gc.build(u.id(), f.id());
							status.put(comp, status.get(comp)+1);
							doneAction = true;
						}
					} catch (RuntimeException e) {
						//blueprint
						if (gc.karbonite() >= bc.bcUnitTypeBlueprintCost(UnitType.Factory) && t != null && t.x == comp && t.y == 0) {
							if (gc.canBlueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc))) {
								gc.blueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc));
								doneAction = true;
								try {
									Unit f = gc.senseUnitAtLocation(initLoc);
									initIds.put(comp, f.id());
									status.put(comp, 0);
									Pathing.factory.add(f);
									Rollout.purchQ.remove();
									t = Rollout.purchQ.peek();
								} catch (Exception e2) {
									System.out.println("can not find factory");
								}
								System.out.println("round " + gc.round() + ": Placed initial factory for component " + comp);
							} else {
								//if occupied by other team
								for (Direction d : dirs) {
									if (gc.canBlueprint(u.id(), UnitType.Factory, d)) {
										gc.blueprint(u.id(), UnitType.Factory, d);
										doneAction = true;
										MapLocation newLoc = mapLoc.add(d);
										try {
											Unit f = gc.senseUnitAtLocation(newLoc);
											initIds.put(comp, f.id());
											status.put(comp, 0);
											Pathing.factory.add(f);
											Rollout.purchQ.remove();
											t = Rollout.purchQ.peek();
										} catch (Exception e2) {
											System.out.println("can not find factory");
										}
										System.out.println("round " + gc.round() + ": Placed initial factory for component " + comp);
										factLocs.put(comp, newLoc);

										ArrayList<MapLocation> temp = new ArrayList<>(factLocs.size() * 8);
										for (int key : factLocs.keySet()) {
											MapLocation loc = factLocs.get(key);
											for (Direction d2 : Direction.values()) {
												if (d2 == Direction.Center)
													continue;
												MapLocation newLoc2 = loc.add(d2);
												if (!earth.onMap(newLoc2))
													continue;
												if (pass[newLoc2.getY()][newLoc2.getX()] == 0)
													continue;
												temp.add(newLoc2);
											}
										}
										factBFS = Pathing.BFS(temp, false);
										break;
									}
								}
							}
						}
					}
				}
			}
			//3. replicate/karb
			if (gc.karbonite() >= bc.bcUnitTypeReplicateCost(UnitType.Worker) && u.abilityHeat() < 10 && t != null && t.x == comp && t.y == 1) {
				Direction minD = Direction.Center;
				int min = 9999;
				for (Direction d : dirs) {
					if(d == Direction.Center)
						continue;
					MapLocation newLoc = mapLoc.add(d);
					if (newLoc.getX() < 0 || newLoc.getY() < 0 || newLoc.getX() >= earth.getWidth() || newLoc.getY() >= earth.getHeight())
						continue;
					int newMin = factBFS[newLoc.getY()][newLoc.getX()];
					if (newMin < min && gc.canReplicate(u.id(), d)) {
						min = newMin;
						minD = d;
					}
				}
				if (gc.canReplicate(u.id(), minD)) {
					gc.replicate(u.id(), minD);
					Rollout.purchQ.remove();
				}
			}
			if (!doneAction) {
				for (int k = 0; k < 8; k++) {
					if (gc.canHarvest(u.id(), dirs[k])) {
						gc.harvest(u.id(), dirs[k]);
						doneAction = true;
						break;
					}
				}
			}
		}
		ArrayList<Integer> toRemove = new ArrayList<>(factLocs.size());
		for(int comp : factLocs.keySet()){
			if(workNum.containsKey(comp))
				continue;
			System.out.println("round " + gc.round() + ": All workers destroyed in component " + comp);
			toRemove.add(comp);
		}
		for(int comp : toRemove){
			if(initIds.containsKey(comp)) {
				initIds.remove(comp);
				status.remove(comp);
			}
			factTime.remove(comp);
			factLocs.remove(comp);
			pathDist.remove(comp);
			Queue<Tuple<Integer, Integer>> temp = new LinkedList<>();
			while(!purchQ.isEmpty()){
				Tuple<Integer, Integer> t = purchQ.remove();
				if(t.x != comp)
					temp.add(t);
			}
			purchQ = temp;
		}
		System.out.println(status);
		return new Tuple<>(karbMapBFS, dest);
	}

	private static void updateQueue(Set<Integer> comps, int[][] allies, int[][] enemies, Map<Integer, Integer> work, int[][] neigh){
		//TODO: factory production
		//1. Insert with original scores
		PriorityQueue<Tuple<Integer, Integer>> pq = new PriorityQueue<>(comps.size(), new CustomComparator());
		int w = (int)earth.getWidth();
		for(int id : comps){
			if(allies[id/w][id%w] == -1)
				continue;
			if(enemies[id/w][id%w] == -1)
				continue;
			int score = 0;
			score += UnionFind.size(Planet.Earth, id);
			score += 2*UnionFind.karbonite(Planet.Earth, id);
			if(status.containsKey(id))
				score -= 500;
			pq.add(new Tuple<>(id, score));
		}

		//2. Loop over pq
		long karb = gc.karbonite();
		long round = gc.round();
		Map<Integer, Integer> buildStatus = new HashMap<>(status);   //also used to determine if factory was queued
		Map<Integer, Integer> workers = new HashMap<>(works);
		Map<Integer, Long> lastRound = new HashMap<>(pq.size());
		purchQ = new LinkedList<>();
		factTime = new HashMap<>(pq.size());
		while(!pq.isEmpty()){
			long need = bc.bcUnitTypeBlueprintCost(UnitType.Factory);
			Tuple<Integer, Integer> t = pq.remove();
			if(buildStatus.containsKey(t.x))
				need = bc.bcUnitTypeReplicateCost(UnitType.Worker);

			//iterate round
			long karbOld = karb;
			long roundOld = round;  //in case rep fails
			while(karb < need){
				karb = passive(karb);
				round++;
			}
			karb -= need;

			if(!buildStatus.containsKey(t.x)){
				buildStatus.put(t.x, 0);
				workers.put(t.x, work.get(t.x));
				lastRound.put(t.x, round);
				factTime.put(t.x, (int)round);
				purchQ.add(new Tuple<>(t.x, 0));
				//add back
				pq.add(new Tuple<>(t.x, t.y-500));
			}
			else {
				int wo = workers.get(t.x);
				int newStatus = buildStatus.get(t.x) + wo * (int) (round - lastRound.get(t.x));
				double remaining = (ACT_NUM-newStatus)/wo;
				//TODO: make last rep better
				if(((factLocs == null || wo < neigh[factLocs.get(t.x).getY()][factLocs.get(t.x).getX()]) && remaining > 5) || (remaining > 0 && pq.isEmpty())){
					lastRound.put(t.x, round);
					buildStatus.put(t.x, newStatus);
					workers.put(t.x, wo + 1);
					purchQ.add(new Tuple<>(t.x, 1));
					//add back
					pq.add(new Tuple<>(t.x, t.y - 250));
				}
				else{
					karb = karbOld;
					round = roundOld;
				}
			}
		}
	}
	private static void locs(int[][] pass, int[][] neigh, int[][] open, int[][] karb, int[][] karbBlur, int[][] enemies, int[][] allies, int[][] occupied){
		Map<Integer, Double> max = new HashMap<>();
		factLocs = new HashMap<>();
		for(int y = 0; y < earth.getHeight(); y++){
			for(int x = 0; x < earth.getWidth(); x++){
				if(pass[y][x] == 0)
					continue;
				if(enemies[y][x] == -1)
					continue;
				if(allies[y][x] == -1)
					continue;
				if(occupied[y][x] == 1) //prevent placing on top of workers
					continue;
				double score = 0;
				int id = UnionFind.id(Planet.Earth, x, y);
				long time = factTime.get(id);

				score += 4*neigh[y][x];
				score += open[y][x]/20;    //200
				score -= karb[y][x];    //100
				score += 2*karbBlur[y][x];    //75
				score += time*Math.sqrt(enemies[y][x])/10;     //50
				score -= 4*Math.max(time/2-3, allies[y][x]);      //50

				if(!max.containsKey(id) || max.get(id) < score){
					max.put(id, score);
					factLocs.put(id, new MapLocation(Planet.Earth, x, y));
				}
			}
		}

		//assign close workers
		ArrayList<MapLocation> temp = new ArrayList<>(factLocs.size()*8);
		pathDist = new HashMap<>(factLocs.size());
		for(int key : factLocs.keySet()){
			MapLocation loc = factLocs.get(key);
			for(Direction d : Direction.values()) {
				if(d == Direction.Center)
					continue;
				MapLocation newLoc = loc.add(d);
				if(!earth.onMap(newLoc))
					continue;
				if(pass[newLoc.getY()][newLoc.getX()] == 0)
					continue;
				temp.add(newLoc);
			}
		}
		factBFS = Pathing.BFS(temp, false);
		for(int key : factLocs.keySet()) {
			MapLocation loc = factLocs.get(key);
			pathDist.put(key, Math.max(allies[loc.getY()][loc.getX()], factTime.get(key))+5);
		}
	}
	private static int[][] blurN(int[][] img){
		int c;
		int i,ri,xl,xi,yl,yi,ym;
		int iw=img[0].length;
		int ih=img.length;

		int img2[][]=new int[ih][iw];
		int img3[][]=new int[ih][iw];

		yi=0;

		for (yl=0;yl<ih;yl++){
			for (xl=0;xl<iw;xl++){
				c=0;
				ri=xl-1;
				for (i=0;i<3;i++){
					xi=ri+i;
					if (xi>=0 && xi<iw){
						c+=img[yi][xi];
					}
				}
				img2[yi][xl]=c;
			}
			yi++;
		}
		yi=0;

		for (yl=0;yl<ih;yl++){
			ym=yl-1;
			for (xl=0;xl<iw;xl++){
				c=0;
				ri=ym;
				for (i=0;i<3;i++){
					if (ri<ih && ri>=0){
						c+=img2[ri][xl];
					}
					ri++;
				}
				img3[yi][xl]=c;
			}
			yi++;
		}
		return img3;
	}
	private static long passive(long karb){
		return karb+Math.max(0, 10-karb/40);
	}
}