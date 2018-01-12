import bc.*;

public class Healer {
    public static void run(GameController gc) {
        Team team = gc.team();
        Team enemy;

        if(team.equals(Team.Red))
            enemy = Team.Blue;
        else
            enemy = Team.Red;

        VecUnit units = gc.myUnits();
        for(int i = 0; i < units.size(); i++) { //...there's gotta be a better way to do this
            Unit currentHealer = units.get(i);

            if(currentHealer.unitType().equals(UnitType.Healer)) { // Start actual healer code

                MapLocation myMapLocation = currentHealer.location().mapLocation();
                int id = currentHealer.id();
                VecUnit friends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.attackRange(), team);

                // If can heal, search for friend with lowest health and heal
                // TODO: Should take unit type into account at some point\
                long minhealth = 9999;
                int targetID = -1;
                if(gc.isHealReady(id)) {
                    for (int j = 0; j < friends.size(); j++) {
                        Unit friend = friends.get(j);
                        if (friend.health() < minhealth) {
                            minhealth = friend.health();
                            id = friend.id();
                        }
                    }
                    if(gc.canHeal(id, targetID)) {  // Should ALWAYS be true
                        gc.heal(id, targetID);
                    }
                }

                // Run away from nearest enemy
                // TODO: Should run away from largest "cluster" of enemies

                VecUnit notFriends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.visionRange(), enemy);

            }
        }
    }

}
