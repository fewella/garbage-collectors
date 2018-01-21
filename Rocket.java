import bc.*;
import java.util.*;
class Rocket {
	static Direction[] dirs = Direction.values();
	static int[][] occupied;
	public static void turn(GameController gc) {
		if(gc.planet() == Planet.Earth) {
			for (Unit r : Player.rocket) {
				if (r.structureGarrison().size() != r.structureMaxCapacity() && r.location().isOnPlanet(Planet.Earth)) {
					VecUnit loUnit = gc.senseNearbyUnitsByTeam(r.location().mapLocation(), 2, gc.team());
					for (int i = 0; i < loUnit.size(); i++) {
						if (gc.canLoad(r.id(), loUnit.get(i).id())) {
							gc.load(r.id(), loUnit.get(i).id());
							System.out.println(loUnit.get(i).unitType() + " loaded");
						}
					}
				}
				//can't launch if not built
				if (r.structureIsBuilt() == 0)
					continue;
				if (r.structureGarrison().size() == r.structureMaxCapacity() || r.health() < 120 || gc.round() > 500) {
					int[][] karb = MapTools.Karbonite.matrix(Planet.Mars, (int) (gc.round() + gc.currentDurationOfFlight()));
					int minScore = 999999999;
					MapLocation dest = new MapLocation(Planet.Mars, 10, 10);    //shouldn't happen
					for (int y = 0; y < Player.mapMars.getHeight(); y++) {
						for (int x = 0; x < Player.mapMars.getWidth(); x++) {
							if (MapTools.Passable.matrix(Planet.Mars)[y][x] == 0) continue;
							if (occupied[y][x] == 1) continue;
							int score = 5 * karb[y][x] - MapTools.UnionFind.karbonite(Planet.Mars, x, y);
							for (int dy = -1; dy <= 1; dy++) {
								if (y + dy < 0 || y + dy >= Player.mapMars.getHeight()) continue;
								for (int dx = -1; dx <= 1; dx++) {
									if (x + dx < 0 || x + dx >= Player.mapMars.getWidth()) continue;
									score -= karb[y + dy][x + dx];
								}
							}
							if (score < minScore) {
								minScore = score;
								dest = new MapLocation(Planet.Mars, x, y);
							}
						}
					}
					if (gc.canLaunchRocket(r.id(), dest)) {
						gc.launchRocket(r.id(), dest);
						occupied[dest.getY()][dest.getX()] = 1;
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
