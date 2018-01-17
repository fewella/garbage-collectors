import bc.*;

import java.util.*;

// import the API.
// See xxx for the javadocs.
class Snipe {
	public Snipe(int health, MapLocation loc1) {
		loc = loc1;
		shots = (health + 39) / 40;
	}

	void set(int health, MapLocation loc1) {
		loc = loc1;
		shots = (health + 39) / 40;
	}

	int shots;
	MapLocation loc;
}

class ComBot {
	static Direction[] dirs = Direction.values();
	static Random rng = new Random(7);
	static GameController gc;
	static int fights = 3;
	static MapLocation[] fight = new MapLocation[12];
	static int[] fightR = new int[12];
	static Unit[] myR = new Unit[500];
	static int rangers = 0;
	static Unit[] enemy = new Unit[300];
	static int[] canHit = new int[300];
	static int[] healths = new int[300];
	static int enemies = 0;
	static int hitters = 0;
	static Unit[] hitter = new Unit[200];
	static int snipePos = -1;
	static Snipe[] snipe = new Snipe[20];
	static int[][] bfs;
	static Team ot;

	/*
	 * shooting priorities: 1. Things that can hit you 2. Ties broken by health
	 * 3. Ties broken by damage 4. Ties broken by closer
	 */
	static void init(GameController gameC) {
		gc = gameC;
		ot = gc.team() == Team.Red ? Team.Blue : Team.Red;
		for (int i = 0; i < snipe.length; i++) {
			snipe[i] = new Snipe(10, null);
		}
		VecUnit vu = Player.map.getInitial_units();
		int p = 0;
		for (long i = vu.size() - 1; i >= 0; i--) {
			Unit u = vu.get(i);
			if (u.team() != gc.team()) {
				fightR[p] = 200;
				fight[p++] = u.location().mapLocation();
			}
		}
	}

	static void snipe() {
		for (int i = 0; i < enemies; i++) {
			if (!robot(enemy[i]) || enemy[i].unitType() == UnitType.Worker && enemy[i].movementHeat() == 0) {
				snipePos++;
				snipePos %= snipe.length;
				snipe[snipePos].set((int) enemy[i].maxHealth(), enemy[i].location().mapLocation());

			}
		}
		if (gc.researchInfo().getLevel(UnitType.Ranger) >= 3 && snipePos != -1) {
			int ids[] = new int[10];
			int i = 0;
			for (int sp = snipePos; sp != (snipePos + 1) % snipe.length; sp = (sp + snipe.length - 1) % snipe.length) {
				if (snipe[sp].loc == null)
					continue;
				int found = 0;
				while (i < rangers) {
					if (myR[i].abilityCooldown() < 10 && bfs[myR[i].location().mapLocation().getY()][myR[i].location()
							.mapLocation().getY()] > 9) {
						ids[found++] = myR[i].id();
						if (found == snipe[sp].shots) {
							for (int k = 0; k < found; k++) {
								gc.beginSnipe(ids[k], snipe[sp].loc);
							}
							snipe[sp].loc = null;
						}
					}
					i++;
				}
			}
		}
	}

	static int d2(MapLocation a, MapLocation b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	static int d2m(MapLocation a, MapLocation b) {
		int d1 = Math.abs(a.getX() - b.getX()) - 1;
		int d2 = Math.abs(a.getY() - b.getY()) - 1;
		return d1 * d1 + d2 * d2;
	}

	static int d2m2(MapLocation a, MapLocation b) {
		int d1 = Math.max(0, Math.abs(a.getX() - b.getX()) - 2);
		int d2 = Math.max(0, Math.abs(a.getY() - b.getY()) - 2);
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
		if (d > me.attackRange() || d <= 10)
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
					// System.out.println(myR[i].location().mapLocation().distanceSquaredTo(enemy[shoot].location().mapLocation()));
					gc.attack(myR[i].id(), enemy[shoot].id());
					healths[shoot] -= myR[i].damage();
				}
			}
		}

	}

	static void resetFights() {
		int r = (int) gc.round();
		for (int i = 0; i < fights; i++) {
			if (r - fightR[i] > 100 || fight[i] == null
					|| (gc.senseNearbyUnitsByTeam(fight[i], 0, gc.team()).size() != 0)) {

				while (true) {
					int x = rng.nextInt((int) Player.map.getWidth());
					int y = rng.nextInt((int) Player.map.getHeight());
					if (MapAnalysis.passabilityMat[y][x] != 0) {
						fight[i] = new MapLocation(gc.planet(), x, y);
						fightR[i] = r;
						break;
					}
				}
			}
		}
	}

	static void turn() {
		enemies = 0;
		rangers = 0;
		hitters = 0;
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

		ArrayList<MapLocation> rocks = new ArrayList<MapLocation>(12);
		int rocketspace = 0;
		VecUnit mine = gc.myUnits();
		for (long i = mine.size() - 1; i >= 0; i--) {
			if (mine.get(i).location().isOnMap()) {
				Unit u = mine.get(i);
				MapLocation m = u.location().mapLocation();

				if (u.unitType() == UnitType.Rocket
						&& (u.health() > (4 * u.maxHealth() / 5) || u.structureIsBuilt() != 0)
						&& u.structureGarrison().size() < u.structureMaxCapacity() && u.rocketIsUsed() == 0) {
					rocks.add(m);
					rocketspace += u.structureMaxCapacity() - u.structureGarrison().size();
				}

				for (int k = 0; k < enemies; k++) {
					if (robot(enemy[k]) && enemy[k].damage() > 0
							&& d2m(m, enemy[k].location().mapLocation()) <= enemy[k].attackRange()) {
						canHit[k]++;
					}
				}
			}
		}
		boolean[] toRock = new boolean[rangers];
		shootPeople();
		resetFights();
		ArrayList<MapLocation> targs = new ArrayList<MapLocation>(enemies + 12);
		for (int i = 0; i < fight.length; i++) {
			if (fight[i] != null) {
				targs.add(fight[i]);
			}
		}
		for (int i = 0; i < enemies; i++) {
			if (healths[i] > 0) {
				targs.add(enemy[i].location().mapLocation());
				if (robot(enemy[i]) && enemy[i].damage() > 0 && canHit[i] < 2) {
					hitter[hitters++] = enemy[i];
				}
			}
		}
		int[][] pRock=null;
		if (rocketspace != 0) {
			pRock = MapAnalysis.BFS(rocks);
			int[] low = new int[rocketspace];
			int p = 0;
			int ip = 0;
			while (p < rocketspace && ip < rangers) {
				int d = pRock[myR[ip].location().mapLocation().getY()][myR[ip].location().mapLocation().getX()];
				if (d != -1) {
					low[p] = ip;
					p++;
				}
				ip++;
			}
			int rocketLoaders = p;
			for (int i = ip; i < rangers; i++) {
				int d = pRock[myR[i].location().mapLocation().getY()][myR[i].location().mapLocation().getX()];
				if (d != -1) {
					int maxp = 0;
					int maxd = pRock[myR[low[0]].location().mapLocation().getY()][myR[low[0]].location().mapLocation()
							.getX()];
					for (int k = 1; k < rocketspace; k++) {
						int dr = pRock[myR[low[k]].location().mapLocation().getY()][myR[low[k]].location().mapLocation()
								.getX()];
						if (dr < maxd) {
							maxd = dr;
							maxp = k;
						}
					}
					if (maxd > d) {
						low[maxp] = i;
					}
				}
			}

			for (int i = 0; i < rocketLoaders; i++) {
				toRock[low[i]] = true;
			}
		}
		bfs = MapAnalysis.BFS(targs);
		for (int i = 0; i < rangers; i++) {
			if (gc.isMoveReady(myR[i].id())) {
				int best = -1;
				int val = 99999;
				MapLocation myloc = myR[i].location().mapLocation();
				for (int d = 0; d < 9; d++) {
					if (gc.canMove(myR[i].id(), dirs[d])) {
						MapLocation nloc = myloc.add(dirs[d]);
						int v = 0;
						for (int k = 0; k < hitters; k++) {
							if (canHit[k] == 0 && d2m2(nloc, hitter[k].location().mapLocation()) <= hitter[k].attackRange()
									
									|| d2m(myloc, hitter[k].location().mapLocation()) <= hitter[k].attackRange()) {
								v++;
							}
						}
						if (gc.senseNearbyUnitsByTeam(nloc, 50, ot).size() > 0) {
							v--;
						}
						v *= 1000;
						if (gc.isAttackReady(myR[i].id())) {
							if (toRock[i]) {
								v += pRock[myR[i].location().mapLocation().getY()][myR[i].location().mapLocation()
										.getX()];
							} else {
								v += bfs[nloc.getY()][nloc.getX()];
							}
						} else {
							int mind = 999;
							for (int k = 0; k < enemies; k++) {
								mind = Math.min(mind, d2(nloc, enemy[k].location().mapLocation()));
							}
							if (mind <= myR[i].attackRange()) {
								v -= mind;
							} else {
								v += mind;
							}
						}
						if (v < val) {
							val = v;
							best = d;
						}
					}
				}
				if (best != -1) {
					gc.moveRobot(myR[i].id(), dirs[best]);
				}
			}
		}
		shootPeople();
		snipe();

	}
}