import bc.*;

import java.util.*;

// import the API.
// See xxx for the javadocs.
class Snipe {
	public Snipe(int health, MapLocation loc1, int id1) {
		loc = loc1;
		shots = (health + 29) / 30;
		id = id1;
	}

	void set(int health, MapLocation loc1, int id1) {
		loc = loc1;
		shots = (health + 29) / 30;
		id = id1;
	}

	int id;
	int shots;
	MapLocation loc;
}

class ComBot {

	static final int RANGERRANGE = 50;
	static Direction[] dirs = Direction.values();
	static Random rng = new Random(7);
	static GameController gc;
	static int fights = 3;
	static MapLocation[] fight = new MapLocation[50];
	static int[] fightR = new int[50];
	static MapLocation[] importantFights = new MapLocation[5];
	static Unit[] myR = new Unit[500];
	static int rangers = 0;
	static Unit[] myK = new Unit[100];
	static int knights = 0;
	static Unit[] enemy = new Unit[300];
	static int[] canHit = new int[300];
	static int[] healths = new int[300];
	static int enemies = 0;
	static int hitters = 0;
	static Unit[] hitter = new Unit[200];
	static int[][] bfs;
	static Team ot;
	static Team ourTeam;
	static int[] dontSnipe = new int[30];
	static int dontSnipePos = 0;
	static int[] hitterCanHit = new int[200];
	static short[][][][] d2m = new short[50][50][50][50];
	static int mapMaxX;
	static int mapMaxY;
	static Planet ourPlanet;
	static MapLocation[] fact = new MapLocation[20];
	static int[] enemyComps;
	// [x][y][x][y], second one always moves

	/*
	 * shooting priorities: 1. Things that can hit you 2. Ties broken by health
	 * 3. Ties broken by damage 4. Ties broken by closer
	 */

	static void makeMoveLookup() {
		int xm = (int) Player.map.getWidth();
		int ym = (int) Player.map.getHeight();
		mapMaxX = xm;
		mapMaxY = ym;
		int[][] mat = MapTools.Passable.matrix(gc.planet());
		int tooY = ym - 1;
		int tooX = xm - 1;
		for (int x1 = 0; x1 < xm; x1++) {
			for (int y1 = 0; y1 < ym; y1++) {
				if (mat[y1][x1] != 0) {
					for (int x2 = 0; x2 < xm; x2++) {
						for (int y2 = 0; y2 < ym; y2++) {
							if (mat[y2][x2] != 0) {

								int dx = x2 - x1;
								int dy = y2 - y1;
								int d = dx * dx + dy * dy;
								if (y2 != tooY && mat[y2 + 1][x2] != 0)
									d = Math.min(d, (dx) * (dx) + (dy + 1) * (dy + 1));
								if (y2 != tooY && x2 != tooX && mat[y2 + 1][x2 + 1] != 0)
									d = Math.min(d, (dx + 1) * (dx + 1) + (dy + 1) * (dy + 1));
								if (y2 != tooY && x2 != 0 && mat[y2 + 1][x2 - 1] != 0)
									d = Math.min(d, (dx - 1) * (dx - 1) + (dy + 1) * (dy + 1));
								if (x2 != tooX && mat[y2][x2 + 1] != 0)
									d = Math.min(d, (dx + 1) * (dx + 1) + (dy) * (dy));
								if (x2 != 0 && mat[y2][x2 - 1] != 0)
									d = Math.min(d, (dx - 1) * (dx - 1) + (dy) * (dy));
								if (y2 != 0 && mat[y2 - 1][x2] != 0)
									d = Math.min(d, (dx) * (dx) + (dy - 1) * (dy - 1));
								if (y2 != 0 && x2 != tooX && mat[y2 - 1][x2 + 1] != 0)
									d = Math.min(d, (dx + 1) * (dx + 1) + (dy - 1) * (dy - 1));
								if (y2 != 0 && x2 != 0 && mat[y2 - 1][x2 - 1] != 0)
									d = Math.min(d, (dx - 1) * (dx - 1) + (dy - 1) * (dy - 1));
								d2m[x1][y1][x2][y2] = (short) d;
							}
						}
					}
				}
			}
		}
	}

	static void init(GameController gameC) {
		gc = gameC;
		ot = gc.team() == Team.Red ? Team.Blue : Team.Red;
		ourTeam = gc.team();
		ourPlanet = gc.planet();
		if (ourPlanet == Planet.Earth) {
			fights = 4;
		} else {
			fights = 40;
		}
		VecUnit vu = Player.map.getInitial_units();
		makeMoveLookup();
		int p = 0;
		enemyComps = new int[(int) vu.size() / 2];
		for (long i = vu.size() - 1; i >= 0; i--) {
			Unit u = vu.get(i);
			if (u.team() != gc.team()) {
				MapLocation m = u.location().mapLocation();
				enemyComps[p] = MapTools.UnionFind.id(ourPlanet, m.getX(), m.getY());
				importantFights[p++] = m;
			}
		}

	}

	static void doSnipe(int[] ids, int p, int idt, MapLocation target, int shots) {
		boolean valid = true;
		for (int k = 0; k < dontSnipe.length; k++) {
			if (dontSnipe[k] == idt) {
				valid = false;
				break;
			}
		}
		if (valid) {
			if (p + 1 >= shots) {
				dontSnipe[dontSnipePos] = idt;
				dontSnipePos = (dontSnipePos + 1) % dontSnipe.length;
				while (shots > 0 && p >= 0) {
					gc.beginSnipe(ids[p], target);
					shots--;
					p--;
				}
			}
		}
	}

	static void snipe() {
		if (gc.researchInfo().getLevel(UnitType.Ranger) >= 3) {
			int ids[] = new int[200];
			int p = 0;
			for (int i = 0; i < rangers; i++) {
				int bfsd = bfs[myR[i].location().mapLocation().getY()][myR[i].location().mapLocation().getX()];
				if (myR[i].abilityHeat() < 10 && (bfsd >= 9 || bfsd == -1) && gc.unit(myR[i].id()).attackHeat() == 0) {
					ids[p++] = myR[i].id();
				}
			}
			p -= 1;
			for (int i = 0; i < enemies; i++) {
				if (enemy[i].unitType() == UnitType.Rocket) {
					doSnipe(ids, p, enemy[i].id(), enemy[i].location().mapLocation(), 7);
					p -= 7;
				}
			}

			if (p <= 8)
				return;

			for (int i = 0; i < fact.length; i++) {
				if (fact[i] != null) {
					doSnipe(ids, p, fact[i].getX() * 10000 + fact[i].getY(), fact[i], 10);
					p -= 10;
				}
			}

			if (p <= 5)
				return;

			for (int i = 0; i < enemies; i++) {
				if (robot(enemy[i]) && enemy[i].movementHeat() == 0) {
					doSnipe(ids, p, enemy[i].id(), enemy[i].location().mapLocation(),
							(((int) enemy[i].maxHealth() + 29) / 30));
					p -= (((int) enemy[i].maxHealth() + 29) / 30);
				}
			}
			
			if (p <= 5)
				return;

			for (int i = 0; i < enemies; i++) {
				if (robot(enemy[i])) {
					doSnipe(ids, p, enemy[i].id(), enemy[i].location().mapLocation(),
							(((int) enemy[i].maxHealth() + 29) / 30));
					p -= (((int) enemy[i].maxHealth() + 29) / 30);
				}
			}
			
			if (p <= 5)
				return;
			
			for (int tryo = 0; tryo < 200 && p > 5; tryo++) {
				int x = rng.nextInt(mapMaxX);
				int y = rng.nextInt(mapMaxY);
				MapLocation loc = new MapLocation(ourPlanet, x, y);
				if (MapTools.Passable.matrix(ourPlanet)[y][x] != 0) {
					int comp = MapTools.UnionFind.id(ourPlanet, x, y);
					if (MapTools.UnionFind.size(ourPlanet, comp) > 2 && !gc.canSenseLocation(loc)) {
						boolean valid = false;
						for (int i = 0; i < enemyComps.length; i++) {
							if (comp == enemyComps[i])
								valid = true;
						}
						if (valid) {
							doSnipe(ids, p, x * 10000 + y, loc, 7);
							p -= 7;
						}
					}
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

	static int pri(int e, MapLocation pos) {
		Unit u = enemy[e];
		if (healths[e] <= 0)
			return 99999;
		MapLocation en = u.location().mapLocation();
		int d = d2(en, pos);
		if (d > RANGERRANGE || d <= 10)
			return 99999;
		int pri = (int) (3 * healths[e]);
		if (robot(u) && Math.abs(u.damage()) > 0) {
			pri -= Math.abs(u.damage());
		}
		if (canHit[e] == 0)
			pri += 1000;
		if (enemy[e].unitType() == UnitType.Healer)
			pri -= 1010;
		pri += d;
		return pri;
	}

	static int kPri(int e, MapLocation pos) {
		Unit u = enemy[e];
		if (healths[e] <= 0)
			return 99999;
		MapLocation en = u.location().mapLocation();
		int d = d2(en, pos);
		if (d > 2)
			return 99999;
		int pri = (int) (3 * healths[e]);
		if (robot(u) && Math.abs(u.damage()) > 0) {
			pri -= Math.abs(u.damage());
		}
		if (canHit[e] == 0)
			pri += 1000;
		if (enemy[e].unitType() == UnitType.Healer)
			pri -= 1010;
		pri += d;
		return pri;
	}

	static void shootPeople() {
		for (int i = 0; i < knights; i++) {
			int mr = myK[i].id();
			if (gc.isAttackReady(mr)) {
				MapLocation loc = gc.unit(mr).location().mapLocation();
				int minPri = 9999;
				int shoot = -1;
				for (int k = 0; k < enemies; k++) {
					int pri = kPri(k, loc);
					if (pri < minPri) {
						minPri = pri;
						shoot = k;
					}
				}
				if (shoot >= 0) {
					// System.out.println(myR[i].location().mapLocation().distanceSquaredTo(enemy[shoot].location().mapLocation()));
					gc.attack(mr, enemy[shoot].id());
					healths[shoot] -= 80;
				}
			}
		}
		for (int i = 0; i < rangers; i++) {
			int mr = myR[i].id();
			if (gc.isAttackReady(mr)) {
				MapLocation loc = gc.unit(mr).location().mapLocation();
				int minPri = 9999;
				int shoot = -1;
				for (int k = 0; k < enemies; k++) {
					int pri = pri(k, loc);
					if (pri < minPri) {
						minPri = pri;
						shoot = k;
					}
				}
				if (shoot >= 0) {
					// System.out.println(myR[i].location().mapLocation().distanceSquaredTo(enemy[shoot].location().mapLocation()));
					gc.attack(mr, enemy[shoot].id());
					healths[shoot] -= 30;
				}
			}
		}

	}

	static void resetFights() {
		for (int i = 0; i < importantFights.length; i++) {
			if (importantFights[i] != null && gc.hasUnitAtLocation(importantFights[i])
					&& gc.senseUnitAtLocation(importantFights[i]).team() == ourTeam) {
				importantFights[i] = null;
			}
		}
		int r = (int) gc.round();
		for (int i = 0; i < fights; i++) {
			if (r - fightR[i] > 100 || fight[i] == null
					|| (gc.senseNearbyUnitsByTeam(fight[i], 0, gc.team()).size() != 0)) {

				while (true) {
					int x = rng.nextInt(mapMaxX);
					int y = rng.nextInt(mapMaxY);
					if (MapTools.Passable.matrix(ourPlanet)[y][x] != 0) {
						fight[i] = new MapLocation(ourPlanet, x, y);
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
		knights = 0;
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
			} else if (u.unitType() == UnitType.Knight && u.location().isOnMap()) {
				myK[knights++] = u;
			}
		}

		ArrayList<MapLocation> rocks = new ArrayList<MapLocation>(12);
		int rocketspace = 0;
		VecUnit mine = gc.myUnits();
		for (long i = mine.size() - 1; i >= 0; i--) {
			if (mine.get(i).location().isOnMap()) {
				Unit u = mine.get(i);
				MapLocation m = u.location().mapLocation();
				int x = m.getX();
				int y = m.getY();
				if (u.unitType() == UnitType.Rocket
						&& (u.health() > (4 * u.maxHealth() / 5) || u.structureIsBuilt() != 0)
						&& u.structureGarrison().size() < u.structureMaxCapacity() && u.rocketIsUsed() == 0) {
					rocks.add(m);
					rocketspace += u.structureMaxCapacity() - u.structureGarrison().size();
				}

				for (int k = 0; k < enemies; k++) {
					if (robot(enemy[k]) && enemy[k].damage() > 0
							&& d2m[x][y][enemy[k].location().mapLocation().getX()][enemy[k].location().mapLocation()
									.getY()] <= enemy[k].attackRange()) {
						canHit[k]++;
					}
				}
			}
		}
		boolean[] toRock = new boolean[rangers];
		shootPeople();
		resetFights();
		ArrayList<MapLocation> targs = new ArrayList<MapLocation>(enemies + 12);
		ArrayList<MapLocation> rngTargs = new ArrayList<MapLocation>(fights);
		for (int i = 0; i < importantFights.length; i++) {
			if (importantFights[i] != null) {
				targs.add(importantFights[i]);
			}
		}
		for (int i = 0; i < fact.length; i++) {
			if (fact[i] != null) {
				targs.add(fact[i]);
			}
		}
		for (int i = 0; i < fights; i++) {
			if (fight[i] != null) {
				rngTargs.add(fight[i]);
			}
		}
		for (int i = 0; i < enemies; i++) {
			if (healths[i] > 0) {
				targs.add(enemy[i].location().mapLocation());
				if (robot(enemy[i]) && enemy[i].damage() > 0 && canHit[i] < 2) {
					hitterCanHit[hitters] = canHit[i];
					hitter[hitters++] = enemy[i];

				}
			}
		}
		int[][] pRock = null;
		if (rocketspace != 0) {
			// System.out.println("in");
			pRock = MapAnalysis.BFS(rocks);
			// System.out.println("out");
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
		int[][] rngBfs = MapAnalysis.BFS(rngTargs);
		for (int i = 0; i < knights; i++) {
			if (gc.isMoveReady(myK[i].id())) {
				int best = -1;
				int val = 99999;
				Unit mr = myK[i];
				MapLocation myloc = mr.location().mapLocation();
				int x = myloc.getX();
				int y = myloc.getY();
				for (int d = 0; d < 9; d++) {
					if (gc.canMove(mr.id(), dirs[d])) {
						MapLocation nloc = myloc.add(dirs[d]);
						int nx = nloc.getX();
						int ny = nloc.getY();
						int v = 0;
						for (int k = 0; k < hitters; k++) {
							MapLocation mp = hitter[k].location().mapLocation();
							if ((hitterCanHit[k] == 0 || d2m[x][y][mp.getX()][mp.getY()] <= hitter[k].attackRange())
									&& d2m[nx][ny][mp.getX()][mp.getY()] <= hitter[k].attackRange()) {
								v++;
							}
						}
						v += 1000 * bfs[ny][nx];
						v += 10 * rngBfs[ny][nx];
						if (v < val) {
							val = v;
							best = d;
						}
					}
				}
				if (best != -1) {
					gc.moveRobot(mr.id(), dirs[best]);
				}
			}
		}
		for (int i = 0; i < rangers; i++) {
			if (gc.isMoveReady(myR[i].id())) {
				int best = -1;
				int val = 99999;
				Unit mr = myR[i];
				MapLocation myloc = mr.location().mapLocation();
				int x = myloc.getX();
				int y = myloc.getY();
				for (int d = 0; d < 9; d++) {
					if (gc.canMove(mr.id(), dirs[d])) {
						MapLocation nloc = myloc.add(dirs[d]);
						int nx = nloc.getX();
						int ny = nloc.getY();
						int v = 0;
						for (int k = 0; k < hitters; k++) {
							MapLocation mp = hitter[k].location().mapLocation();
							if ((hitterCanHit[k] == 0 || d2m[x][y][mp.getX()][mp.getY()] <= hitter[k].attackRange())
									&& d2m[nx][ny][mp.getX()][mp.getY()] <= hitter[k].attackRange()) {
								v++;
							}
						}
						v *= 1000;
						if (gc.senseNearbyUnitsByTeam(nloc, RANGERRANGE, ot).size() > 0) {
							v -= 1100;
						}

						if (gc.unit(mr.id()).attackHeat() == 0) {
							if (toRock[i]) {
								v += pRock[ny][nx];
								// System.out.println("Headed to rocket");
							} else {
								v += 10 * bfs[ny][nx];
								v += rngBfs[ny][nx];
							}
						} else {
							int mind = 999;
							for (int k = 0; k < enemies; k++) {
								mind = Math.min(mind, d2(nloc, enemy[k].location().mapLocation()));
							}
							if (mind <= RANGERRANGE) {
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
					gc.moveRobot(mr.id(), dirs[best]);
				}
			}
		}
		shootPeople();
		for (int i = 0; i < enemies; i++) {
			if (enemy[i].unitType() == UnitType.Factory) {
				MapLocation mp = enemy[i].location().mapLocation();
				boolean newf = true;
				for (int k = 0; k < fact.length; k++) {
					if (fact[k] != null && mp.equals(fact[k])) {
						newf = false;
						break;
					}
				}
				if (newf) {
					for (int k = 0; k < fact.length; k++) {
						if (fact[k] == null) {
							fact[k] = enemy[i].location().mapLocation();
							break;
						}
					}
				}
			}
		}
		for (int i = 0; i < fact.length; i++) {
			if (fact[i] != null && gc.hasUnitAtLocation(fact[i])) {
				fact[i] = null;
			}
		}
		snipe();
	}
}