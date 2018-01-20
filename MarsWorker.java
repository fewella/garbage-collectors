import bc.*;
//import java.util.*;
public class MarsWorker {
	static Direction[] dirs = Direction.values();
	static void turn(GameController gc) {
		long karb = gc.karbonite();
		for (Unit u : Player.worker) {
			if (!u.location().isOnMap()) continue;
			MapLocation mapLoc = u.location().mapLocation();
			//boolean doneAction = false;
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
