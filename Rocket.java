import bc.*;
import java.util.*;
class Rocket {
	static Direction[] dirs = Direction.values();
	public static void turn(GameController gc) {
		if(gc.planet() == Planet.Earth) {
			for (Unit r : Player.rocket) {
				if (r.structureGarrison().size() != r.structureMaxCapacity() && r.location().isOnPlanet(Planet.Earth)) {
					VecUnit loUnit = gc.senseNearbyUnitsByTeam(r.location().mapLocation(), 2, gc.team());
					for (int i = 0; i < loUnit.size(); i++) {
						if (!loUnit.get(i).unitType().equals(UnitType.Worker) && gc.canLoad(r.id(), loUnit.get(i).id())) {
							gc.load(r.id(), loUnit.get(i).id());
							System.out.println(loUnit.get(i).unitType() + " loaded");
						}
					}
				}
				//can't launch if not built
				if (r.structureIsBuilt() == 0)
					continue;
				if (r.structureGarrison().size() == r.structureMaxCapacity() || r.health() < 120 || gc.round() > 500) {
					//determine which option is best
					int work = 0;
					VecUnitID ids = r.structureGarrison();
					for(int i = 0; i < ids.size(); i++)
						if (gc.unit(ids.get(i)).unitType() == UnitType.Worker)
							work++;
					int option = 1;
					if(work > ids.size()/2)
						option = 0;
					//retrieve location
					MapLocation dest = MapTools.RocketLanding.retrieve(option);
					if (gc.canLaunchRocket(r.id(), dest)) {
						gc.launchRocket(r.id(), dest);
						System.out.println("Launched");
					} else {
						System.out.println("Bad dest: " + dest.getX() + ", " + dest.getY());
					}
				}
			}
		}
		else{
			for (Unit r : Player.rocket) {
				if( r.location().isOnPlanet(Planet.Mars) ) {
					for( int k = 0; k < 8; k++ ) {
						if( gc.canUnload(r.id(), dirs[k]) ) {
							gc.unload(r.id(), dirs[k]);
						}
					}
				}
			}
		}
	}
}
