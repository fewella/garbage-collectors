import bc.*;
import java.util.*;
class Rocket {
	static Direction[] dirs = Direction.values();
	public static void launch(GameController gc) {
		long x = Player.mapMars.getWidth()/2;
		long y = x;
		MapLocation dest = new MapLocation(Planet.Mars, Math.toIntExact(x), Math.toIntExact(y));
		for( Unit r:Player.rocket) {
			if(r.structureGarrison().size() != r.structureMaxCapacity() && r.location().isOnPlanet(Planet.Earth)) {
				VecUnit loUnit = gc.senseNearbyUnitsByTeam(r.location().mapLocation(),1 , gc.team());
				for( int i = 0; i < loUnit.size(); i++ ) {
					if(gc.canLoad(r.id(), loUnit.get(i).id()))
						gc.load(r.id(), loUnit.get(i).id());
				}
			}
			if(r.structureGarrison().size() == r.structureMaxCapacity() || r.health() < 120 || gc.round() > 500) {
				if( gc.canLaunchRocket(r.id(), dest) ) {
					gc.launchRocket(r.id(), dest);
					System.out.println("Launched");
				}
				else
					System.out.println("Bad dest");
			}
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
