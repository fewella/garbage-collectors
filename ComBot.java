import bc.*;
// import the API.
// See xxx for the javadocs.

class ComBot {
	static Direction[] dirs = Direction.values();
	static GameController gc;
	static int fights = 8;
	static MapLocation[] fight = new MapLocation[fights];
	static Unit[] myR = new Unit[100];
	static int rangers = 0;
	static Unit[] enemy = new Unit[200];
	static int[] canHit = new int[200];
	static int[] healths = new int[200];
	static int enemies = 0;

	/*
	 * shooting priorities: 1. Things that can hit you 2. Ties broken by health
	 * 3. Ties broken by damage 4. Ties broken by closer
	 */
	static int d2(MapLocation a, MapLocation b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	static int d2m(MapLocation a, MapLocation b) {
		int d1 = Math.abs(a.getX() - b.getX()) - 1;
		int d2 = Math.abs(a.getY() - b.getY()) - 1;
		return d1 * d1 + d2 * d2;
	}

	static boolean robot(Unit u) {
		return u.unitType() != UnitType.Rocket && u.unitType() != UnitType.Factory;
	}

	static int pri(int e, Unit me) {
		Unit u = enemy[e];
		if (healths[e] <= 0)
			return 99999;
		MapLocation en = u.location().mapLocation();
		MapLocation pos = me.location().mapLocation();
		int d = d2(en, pos);
		if (d > me.attackRange() || d<=10)
			return 99999;
		int pri = (int) (3 * healths[e]);
		if (robot(u) && Math.abs(u.damage()) > 0) {
			pri -= Math.abs(u.damage());
		}
		if (canHit[e] == 0)
			pri += 1000;
		pri += d;
		return pri;
	}

	static void shootPeople() {
		for (int i = 0; i < rangers; i++) {
			if (gc.isAttackReady(myR[i].id())) {
				int minPri = 9999;
				int shoot = -1;
				for (int k = 0; k < enemies; k++) {
					int pri = pri(k, myR[i]);
					if (pri < minPri) {
						minPri = pri;
						shoot = k;
					}
				}
				if (shoot >= 0) {
					//System.out.println(myR[i].location().mapLocation().distanceSquaredTo(enemy[shoot].location().mapLocation()));
					gc.attack(myR[i].id(), enemy[shoot].id());
					healths[shoot] -= myR[i].damage();
				}
			}
		}

	}

	static void turn(GameController gameC) {
		gc = gameC;
		enemies = 0;
		rangers = 0;
		VecUnit us = gc.units();
		for (long i = us.size() - 1; i >= 0; i--) {
			Unit u = us.get(i);
			if (u.team() != gc.team() && u.location().isOnMap()) {
				enemy[enemies] = u;
				canHit[enemies] = 0;
				healths[enemies] = (int) u.health();
				enemies++;
			} else if (u.unitType() == UnitType.Ranger && u.location().isOnMap()) {
				myR[rangers++] = u;
			}
		}
		VecUnit mine = gc.myUnits();
		for (long i = mine.size() - 1; i >= 0; i--) {
			if (mine.get(i).location().isOnMap()) {
				MapLocation m = mine.get(i).location().mapLocation();
				for (int k = 0; k < enemies; k++) {
					if (robot(enemy[k]) && enemy[k].damage() > 0
							&& d2m(m, enemy[k].location().mapLocation()) <= enemy[k].abilityRange()) {
						canHit[k]++;
					}
				}
			}
		}
		shootPeople();
		for (int i = 0; i < rangers; i++) {
			int rng = (int) (Math.random() * 8);
			if (gc.isMoveReady(myR[i].id()) && gc.canMove(myR[i].id(), dirs[rng])) {
				gc.moveRobot(myR[i].id(), dirs[rng]);
			}
		}
		shootPeople();
	}
}