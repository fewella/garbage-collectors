// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;

class Econ {
	static Direction[] dirs = Direction.values();

	//initial factory stuff
	static boolean factoryUp = false;
	static boolean factoryPlaced = false;
	static int[][] initBFS;
	static MapLocation initLoc;
	static int[][] karbMapBFS;
	static ArrayList<MapLocation> dest = new ArrayList<MapLocation>();
	static void setup() {
		if (Player.gc.planet() == Planet.Earth) {
			//initial factory
			ArrayList<MapLocation> temp = new ArrayList<>();
			initLoc = MapAnalysis.initialFactory();
			temp.add(initLoc);
			initBFS = MapAnalysis.BFS(temp, false);
		}
	}

	static void turn(GameController gc) {
		//overall structure:
//		loop factories
//		    loop workers
//              initialFactory OR
//              normalCode
		long round = Player.gc.round();
		long karb = gc.karbonite();    //NOTE: global update after every action that affects it
		//Earth strategy:
		//1. Initial factory
		//2. Normal Code
		int madeRocket = 0;
		int replicate = 0;
		HashSet<Unit> stayFactory = new HashSet<>(); //these units will not go out to look for karbonite/make new factories
		//FACTORIES
		for (Unit u : Player.factory) {
			if(factoryUp) {
				if( round > 500 && Player.rocket.size()  == 0 ) {
					
				}
				else {
					VecUnit wF = gc.senseNearbyUnitsByType(u.location().mapLocation(), 2, UnitType.Worker);
					for (int i = 0; i < wF.size() && i < 2; i++) {
						Unit temp = wF.get(i);
						stayFactory.add(temp);
						if (u.health() == u.maxHealth()) {
							for (int k = 0; k < 8; k++) {
								if (/*round%50==4 && */gc.canReplicate(temp.id(), dirs[k])) {
									//NOTE: ^^^ contains an artificial cap; remove later
									gc.replicate(temp.id(), dirs[k]);
									karb = gc.karbonite();
								}
							}
						} else {
							if (gc.canBuild(temp.id(), u.id()))
								gc.build(temp.id(), u.id());
						}
					}
				}
				//uncomment when Healers move away
				if(Player.ranger.size()/6 > Player.healer.size()){
					if (gc.canProduceRobot(u.id(), UnitType.Healer))
						gc.produceRobot(u.id(), UnitType.Healer);
				}
				else{
					if (gc.canProduceRobot(u.id(), UnitType.Ranger))
						gc.produceRobot(u.id(), UnitType.Ranger);
				}
				karb = gc.karbonite();
				for (int k = 0; k < 8; k++)
					if (gc.canUnload(u.id(), dirs[k])) gc.unload(u.id(), dirs[k]);
			}
			else{
				//temporary quickfix - remove later
				if(u.structureIsBuilt() == 1) {
					factoryUp = true;
					System.out.println("round " + round + ": Built initial factory!");
				}
			}
		}

		//WORKERS
		for (Unit u : Player.worker) {
			if (!u.location().isOnMap()) continue;
			MapLocation mapLoc = u.location().mapLocation();
			boolean doneAction = false;
			if (!factoryUp) {
				//initial factory strategy:
				//1. if far, move by matrix
				//2. else: blueprint/build
				//3. if possible, replicate/collect karb
				Collections.shuffle(Arrays.asList(dirs));    //to prevent workers getting stuck
				if (mapLoc.distanceSquaredTo(initLoc) > 2) {    //out of reach
					//1. move
					if (gc.isMoveReady(u.id())) {
						Direction minD = Direction.Center;
						int min = 9999;
						for (Direction d : dirs) {
							MapLocation newLoc = mapLoc.add(d);
							if (newLoc.getX() < 0 || newLoc.getY() < 0 || newLoc.getX() >= gc.startingMap(Planet.Earth).getWidth() || newLoc.getY() >= gc.startingMap(Planet.Earth).getHeight())
								continue;
							int newMin = initBFS[newLoc.getY()][newLoc.getX()];
							if (newMin < min && gc.canMove(u.id(), d)) {
								min = initBFS[newLoc.getY()][newLoc.getX()];
								minD = d;
							}
						}
						if (gc.canMove(u.id(), minD))
							gc.moveRobot(u.id(), minD);
					}
				} else {
					//2. blueprint/build
					try {
						Unit f = gc.senseUnitAtLocation(initLoc);
						//build
						if (gc.canBuild(u.id(), f.id())) {
							gc.build(u.id(), f.id());
							doneAction = true;
							//for some reason doesn't work - fix later
							if(f.structureIsBuilt() == 1) {
								factoryUp = true;
								System.out.println("round " + round + ": Built initial factory!");
							}
						}
					} catch (RuntimeException e){
						//blueprint
						if (gc.canBlueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc))) {
							gc.blueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc));
							karb = gc.karbonite();
							doneAction = true;
							factoryPlaced = true;
							System.out.println("round " + round + ": Placed initial factory");
						}
					}
				}
				//3. replicate/karb
				if(u.abilityHeat() < 10 && /*round%50==4 && */(karb > bc.bcUnitTypeBlueprintCost(UnitType.Factory)+bc.bcUnitTypeReplicateCost(UnitType.Worker) || (factoryPlaced && karb > bc.bcUnitTypeReplicateCost(UnitType.Worker)))){
					//NOTE: contains an artificial ^^^ cap; remove later
					//improve later
					for (int k = 0; k < 8; k++) {
						if (gc.canReplicate(u.id(), dirs[k])) {
							gc.replicate(u.id(), dirs[k]);
							karb = gc.karbonite();
							break;
						}
					}
				}
				if (!doneAction) {
					for (int k = 0; k < 8; k++) {
						if (gc.canHarvest(u.id(), dirs[k])) {
							gc.harvest(u.id(), dirs[k]);
							karb = gc.karbonite();
							doneAction = true;
							break;
						}
					}
				}
			} else {
				//normal code
				karbBFS(round);
				VecUnit nearFac = gc.senseNearbyUnitsByType(mapLoc, 4, UnitType.Factory);
				if(!stayFactory.contains(u)) {
					//System.out.println("Normal");
					VecUnit nearRock = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Rocket);
					if (round > 250 && nearRock.size() != 0) {
						if (gc.canBuild(u.id(), nearRock.get(0).id())) {
							gc.build(u.id(), nearRock.get(0).id());
							doneAction = true;
						} else if (gc.canLoad(nearRock.get(0).id(), u.id())) {
							gc.load(nearRock.get(0).id(), u.id());
							System.out.println("worker loaded");
							doneAction = true;
						}
					}
					if (round > 250 && madeRocket < 3 && Player.rocket.size() < 3 && !doneAction && (nearFac.size() == 0 || round >500) ) {
						//move from factories
						//System.out.println("Trying to make rocket");
						for (int k = 0; k < 8; k++) {
							if (gc.canBlueprint(u.id(), UnitType.Rocket, dirs[k])) {
								gc.blueprint(u.id(), UnitType.Rocket, dirs[k]);
								karb = gc.karbonite();
								madeRocket++;
								doneAction = true;
								break;
							}
						}
					}
					if (nearFac.size() == 0 && !doneAction ) {
						if( (gc.karbonite() < 100) && dest.size() > 0 ) {
							int min = 9999;
							int min2 = 9999;
							int dire = -1;
							int dire2 = -1;
							for (int k = 0; k < 8; k++) {
								MapLocation temp = u.location().mapLocation().add(dirs[k]);
								if( temp.getX() < 0 || temp.getX() >= Player.mapEarth.getWidth() || temp.getY() >= Player.mapEarth.getHeight() || temp.getY() < 0 ) continue;
								int movetemp = karbMapBFS[temp.getY()][temp.getX()];
								if(Math.min(movetemp, min) != min) {
									dire = k;
									min2 = min;
									min = Math.min(movetemp, min);
								}
								else if( Math.min(movetemp, min2) != min2 ) {
									dire2 = k;
									min2 = Math.min(movetemp, min2);
									if( min2 < min ) {
										int t = min2;
										min2 = min;
										min = t;
										int td = dire2;
										dire2 = dire;
										dire = td;
									}
								}
							}
							//System.out.println("Min: " + min + " Min2: " + min2);
							if (min == 0 ) { //0 or 1??
								if(gc.canHarvest(u.id(), dirs[dire])) {
									System.out.println("could harvest");
									if (round > 1 && karb < 300) {
										//System.out.println("Harvesting karbonite");
										gc.harvest(u.id(), dirs[dire]);
										karb = gc.karbonite();
									}
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
										if (round > 1 && karb < 300) {
											//System.out.println("Harvesting karbonite");
											gc.harvest(u.id(), dirs[dire]);
											karb = gc.karbonite();
										}
									}
								}
							}
						}
						for (int k = 0; k < 8; k++) {
							if (gc.canBlueprint(u.id(), UnitType.Factory, dirs[k])) {
								gc.blueprint(u.id(), UnitType.Factory, dirs[k]);
								karb = gc.karbonite();
								doneAction = false;
								break;
							}
						}
					}
					if (nearFac.size() != 0) {
						Unit fac = nearFac.get(0);
						Direction avoid = mapLoc.directionTo(fac.location().mapLocation());
						for (int k = 0; k < 8; k++) {
							if (!dirs[k].equals(avoid)) {
								if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k]))
									gc.moveRobot(u.id(), dirs[k]);
							}
						}
					}
				}
			}
		}
	}
	private static boolean stayByRocket(GameController gc, Unit u, Unit r) {
		if (!u.location().isOnMap()){
			System.out.println("ERROR: Attempted call to stayByRocket on a unit in space/garrison");
			return false;
		}
		MapLocation mapLoc = u.location().mapLocation();
		if (!mapLoc.isAdjacentTo(r.location().mapLocation())) {
			if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), mapLoc.directionTo(r.location().mapLocation()))) {
				gc.moveRobot(u.id(), mapLoc.directionTo(r.location().mapLocation()));
				return false;
			}
			return true;
		}
		return true;
	}
	//[y][x] = [height][width]
	private static void karbBFS(long r) {
		if( r < 2 ) {
			System.out.println("Karbonite Map initiated");
			PlanetMap earth = Player.gc.startingMap(Planet.Earth);
			for(int y = 0; y < earth.getHeight(); y++){
	            for(int x = 0; x < earth.getWidth(); x++){
	                long karb = earth.initialKarboniteAt(new MapLocation(earth.getPlanet(), x, y));
	                if( karb > 0 ) {
	                	dest.add(new MapLocation(Planet.Earth, x, y));
	                }
	            }
			}
		}
		karbMapBFS = MapAnalysis.BFS(dest);
	}
}