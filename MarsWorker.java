import java.util.ArrayList;
import java.util.HashSet;

import bc.*;
import MapTools.Karbonite;
//import java.util.*;
public class MarsWorker {
	static Direction[] dirs = Direction.values();
	static ArrayList<MapLocation> dest = new ArrayList<MapLocation>();
	static int[][] karbMapBFS;
	static HashSet<String> removedKarb = new HashSet<String>();
	static void turn(GameController gc) {
		dest = new ArrayList<MapLocation>();
		karbBFS(gc.round());
		
		long karb = gc.karbonite();
		for (Unit u : Player.worker) {
			if (!u.location().isOnMap()) continue;
			MapLocation mapLoc = u.location().mapLocation();
			//boolean doneAction = false;
			/*for (int k = 0; k < 8; k++) {
				if (gc.canHarvest(u.id(), dirs[k])) {
					if (gc.round() > 1 && karb < 400) {
						gc.harvest(u.id(), dirs[k]);
						karb = gc.karbonite();
						break;
					}
				}
			}*/
			VecUnit nearRoc = gc.senseNearbyUnitsByType(mapLoc, 4, UnitType.Rocket);
			if (nearRoc.size() != 0) {
				/*Direction avoid = mapLoc.directionTo(nearRoc.get(0).location().mapLocation());
				for (int k = 0; k < 8; k++) {
					if (!dirs[k].equals(avoid)) {
						if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k]))
							gc.moveRobot(u.id(), dirs[k]);
					}
				}*/
			}
			//move toward karbonite
			else {
				if( dest.size() > 0 ) {
					int min = 9999;
					int min2 = 9999;
					int dire = -1;
					int dire2 = -1;
					//find where to go
					for (int k = 0; k < 8; k++) {
						MapLocation temp = u.location().mapLocation().add(dirs[k]);
						if( temp.getX() < 0 || temp.getX() >= Player.mapMars.getWidth() || temp.getY() >= Player.mapMars.getHeight() || temp.getY() < 0 ) continue;
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
								int t = min2;
								min2 = min;
								min = t;
								int td = dire2;
								dire2 = dire;
								dire = td;
							}
						}
					}
					//move there
					//System.out.println("Min: " + min + " Min2: " + min2);
					if (min == 0 ) { 
						if(gc.canHarvest(u.id(), dirs[dire])) {
								gc.harvest(u.id(), dirs[dire]);
								karb = gc.karbonite();
						}
						else {
								String temp = "" + u.location().mapLocation().add(dirs[dire]).getX() + u.location().mapLocation().add(dirs[dire]).getY();
								removedKarb.add(temp);
						}
					}
					else if( (min != 9999 && min != 0) && dire != -1 ) {
						if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire]) ) {
							gc.moveRobot(u.id(), dirs[dire]);
						}
					}
					else if( min2 != 9999 && dire2 != -1 ) {
						if( min2 != 0 ) {
							if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire2]) ) {
								gc.moveRobot(u.id(), dirs[dire2]);
							}
						}
						else {
							if(gc.canHarvest(u.id(), dirs[dire2])) {
									gc.harvest(u.id(), dirs[dire]);
									karb = gc.karbonite();
							}
						}
					}
				}
			}
		}
	}
	//[y][x] = [height][width]
	private static void karbBFS(long r) {
		//System.out.println(Math.toIntExact(r));
		int[][] currKarb = Karbonite.matrix(Planet.Mars, Math.toIntExact(r));
		for(int y = 0; y < currKarb.length; y++){
            for(int x = 0; x < currKarb[0].length; x++){
                long k = currKarb[y][x];
                MapLocation temp = new MapLocation(Planet.Mars, x, y);
                String check = "" + x + y;
                if( k > 0 && !removedKarb.contains(check)) {
                	dest.add(temp);
                }
            }
		}
		//System.out.println("dest size: " + dest.size());
		//System.out.println("removed size: " + removedKarb.size());
		karbMapBFS = MapAnalysis.BFS(dest);
	}
}
