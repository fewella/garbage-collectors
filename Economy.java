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
//		if(Earth)
//		    loop factories
//		    loop workers
//              initialFactory OR
//              normalCode
//		else
//		    loop workers

		long karb = gc.karbonite();    //NOTE: global update after every action that affects it
		if (Player.gc.planet() == Planet.Earth) {
			//Earth strategy:
			//1. Initial factory
			//2. Normal Code
			boolean madeRocket = false;
			int replicate = 0;
			HashSet<Unit> stayFactory = new HashSet<>(); //these units will not go out to look for karbonite/make new factories
			//FACTORIES
			for (Unit u : Player.factory) {
				if(factoryUp) {
					VecUnit wF = gc.senseNearbyUnitsByType(u.location().mapLocation(), 2, UnitType.Worker);
					for (int i = 0; i < wF.size() && i < 2; i++) {
						Unit temp = wF.get(i);
						stayFactory.add(temp);
						if (u.health() == u.maxHealth()) {
							for (int k = 0; k < 8; k++) {
								if (/*gc.round()%50==4 && */gc.canReplicate(temp.id(), dirs[k])) {
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
					//uncomment when Healers move away
						/*if(Player.ranger.size()/6 > Player.healer.size()){
							if (gc.canProduceRobot(u.id(), UnitType.Healer))
								gc.produceRobot(u.id(), UnitType.Healer);
						}*/
					//else{
					if (gc.canProduceRobot(u.id(), UnitType.Ranger))
						gc.produceRobot(u.id(), UnitType.Ranger);
					//}
					karb = gc.karbonite();
					for (int k = 0; k < 8; k++)
						if (gc.canUnload(u.id(), dirs[k])) gc.unload(u.id(), dirs[k]);
				}
				else{
					//temporary quickfix - remove later
					if(u.structureIsBuilt() == 1) {
						factoryUp = true;
						System.out.println("round " + gc.round() + ": Built initial factory!");
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
									System.out.println("round " + gc.round() + ": Built initial factory!");
								}
							}
						} catch (RuntimeException e){
							//blueprint
							if (gc.canBlueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc))) {
								gc.blueprint(u.id(), UnitType.Factory, mapLoc.directionTo(initLoc));
								karb = gc.karbonite();
								doneAction = true;
								factoryPlaced = true;
								System.out.println("round " + gc.round() + ": Placed initial factory");
							}
						}
					}
					//3. replicate/karb
					if(u.abilityHeat() < 10 && /*gc.round()%50==4 && */(karb > bc.bcUnitTypeBlueprintCost(UnitType.Factory)+bc.bcUnitTypeReplicateCost(UnitType.Worker) || (factoryPlaced && karb > bc.bcUnitTypeReplicateCost(UnitType.Worker)))){
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
					VecUnit v = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Factory);
					if(!stayFactory.contains(u)) {
						VecUnit nearRock = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Rocket);
						if (gc.round() > 250 && nearRock.size() != 0) {
							if (gc.canBuild(u.id(), nearRock.get(0).id())) {
								gc.build(u.id(), nearRock.get(0).id());
								doneAction = true;
							} else if (gc.canLoad(nearRock.get(0).id(), u.id())) {
								gc.load(nearRock.get(0).id(), u.id());
								System.out.println("worker loaded");
								doneAction = true;
							}
						}
						if (gc.round() > 250 && !madeRocket && Player.rocket.size() == 0 && !doneAction) {
							//move from factories
							for (int k = 0; k < 8; k++) {
								if (gc.canBlueprint(u.id(), UnitType.Rocket, dirs[k])) {
									gc.blueprint(u.id(), UnitType.Rocket, dirs[k]);
									karb = gc.karbonite();
									madeRocket = true;
								}
							}
						}
						VecUnit nearFac = gc.senseNearbyUnitsByType(mapLoc, 4, UnitType.Factory);
						if (nearFac.size() == 0) {
							for (int k = 0; k < 8; k++) {
								if (gc.canBlueprint(u.id(), UnitType.Factory, dirs[k])) {
									gc.blueprint(u.id(), UnitType.Factory, dirs[k]);
									karb = gc.karbonite();
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
						/*if( (Player.worker.size() <= Player.factory.size()*2 && gc.round() < 150 && replicate < 3) || Player.worker.size() <= Player.factory.size()*1.5) {
 	        	            for( int k = 0; k < 8; k++) {
	                            if( gc.canReplicate(u.id(), dirs[k])) {
		                            gc.replicate(u.id(), dirs[k]);
		                            replicate++;
		                            karb = gc.karbonite();
		                            System.out.println("replicated");
	                            }
	                        }
	                    }*/
						/*if( gc.round() < (225/u.workerBuildHealth())+1 ) {
 	        	            VecUnit v=gc.senseNearbyUnitsByType(mapLoc,2,UnitType.Factory);
 	                        for (long k=v.size()1; k>=0; k) {
 	                            if (gc.canBuild(u.id(),v.get(k).id())) {
 	                                gc.build(u.id(),v.get(k).id());
 	                            }
 	                        }
 	                    }*/
						for (int k = 0; k < 8; k++) {
							if (gc.canHarvest(u.id(), dirs[k])) {
								if (gc.round() > 1 && karb < 400) {
									gc.harvest(u.id(), dirs[k]);
									karb = gc.karbonite();
									break;
								}
							}
						}
					}
				}
			}
		} else {
			//Mars code
			for (Unit u : Player.worker) {
				if (!u.location().isOnMap()) continue;
				MapLocation mapLoc = u.location().mapLocation();
				boolean doneAction = false;
				for (int k = 0; k < 8; k++) {
					if (gc.canHarvest(u.id(), dirs[k])) {
						if (gc.round() > 1 && karb < 400) {
							gc.harvest(u.id(), dirs[k]);
							karb = gc.karbonite();
							break;
						}
					}
				}
				VecUnit nearRoc = gc.senseNearbyUnitsByType(mapLoc, 10, UnitType.Rocket);
				if (nearRoc.size() != 0) {
					Direction avoid = mapLoc.directionTo(nearRoc.get(0).location().mapLocation());
					for (int k = 0; k < 8; k++) {
						if (!dirs[k].equals(avoid)) {
							if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k]))
								gc.moveRobot(u.id(), dirs[k]);
						}
					}
				}
				//if time make it move toward karbonite
				else {
					for (int k = 0; k < 8; k++) {
						if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k]))
							gc.moveRobot(u.id(), dirs[k]);
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
}