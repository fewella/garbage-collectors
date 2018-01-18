package MapTools;

import bc.*;

public class RocketLanding {
    private static GameController gc;

    private static int deliveryId, orderId;
    private static short[] received, used;

    public static void setup(GameController gameC){
        gc = gameC;
        orderId = 0;
        deliveryId = 0;
        received = new short[4];
        used = new short[4];
    }
    public static void turn(){
        Veci32 earth = gc.getTeamArray(Planet.Earth);
        Veci32 mars = gc.getTeamArray(Planet.Mars);
        if(gc.planet() == Planet.Earth){
            //listen to delivery
            int newDeliveryId = mars.get(0);
            if(deliveryId != newDeliveryId){
                deliveryId = newDeliveryId;
                received = new short[4];
                used = new short[4];
                //read delivery details
                for(int i = 0; i < 4; i++)
                    received[i] = (short)(mars.get(i+1));
            }
        }
        else{
            //auto send
            if (gc.round() % 50 == 1)
                compute(10, 10, 10, 10);
            //listen to orders
            int newOrderId = earth.get(0);
            if(orderId != newOrderId){
                orderId = newOrderId;
                //process order, send delivery
                compute(earth.get(1), earth.get(2), earth.get(3), earth.get(4));
            }
        }
    }
    /**
     * Call from Earth to request landing locations. While this is done periodically, calling this 100 turns before
     * requesting locations ensures that the data is most recent (50 turns old), and minimizes the risk of running out of locations.
     * @param tt number of locations that will be received with requestLandingLocations(true, true)
     * @param tf number of locations that will be received with requestLandingLocations(true, false)
     * @param ft number of locations that will be received with requestLandingLocations(false, true)
     * @param ff number of locations that will be received with requestLandingLocations(false, false)
     */
    //Earth only
    public static void request(int tt, int tf, int ft, int ff){
        gc.writeTeamArray(0, ++orderId);
        gc.writeTeamArray(1, tt);
        gc.writeTeamArray(2, tf);
        gc.writeTeamArray(3, ft);
        gc.writeTeamArray(4, ff);
    }
    /**
     * Call from Earth to receive a most suitable rocket landing location.
     * For optimal results, call requestLandingLocations() 51 turns before calling this.
     * @param aggressive <br>true: prioritize bombarding enemy base
     *                   <br>false: avoid enemies
     * @param congressive <br>true: prioritize sending close to friendly units
     *                    <br>false: prioritize covering more ground
     * @return MapLocation of the best landing location
     */
    //Earth only
    public static MapLocation retrieve(boolean aggressive, boolean congressive){
        int i = 0;
        if(!aggressive) i = 2;
        if(!congressive) i++;
        if(received[i] <= used[i]){
            System.out.println("Ran out of landing locations for aggressive: " + aggressive + " congressive: " + congressive);
            //TODO
            return null;
        }
        int i2 = 5+used[i]++;
        for(int j = 0; j < i; j++) i2 += received[j];
        int val = gc.getTeamArray(Planet.Mars).get(i2);

        int w = (int)gc.startingMap(Planet.Mars).getWidth();
        return new MapLocation(Planet.Mars, val/w, val%w);
    }

    //Earth only
    private static void computeDefault(){
        //TODO
    }
    //Mars only
    private static void compute(int tt, int tf, int ft, int ff){
        //watch for improper arguments
        int total = tt+tf+ft+ff;
        if(total > 40) {
            System.out.println("Number of total requested locations (" + total + ") exceeds 40");
            tt = tf = ft = ff = 10;
        }
        gc.writeTeamArray(0, ++deliveryId);
        gc.writeTeamArray(1, tt);
        gc.writeTeamArray(2, tf);
        gc.writeTeamArray(3, ft);
        gc.writeTeamArray(4, ff);
        //compute best locations
        //TODO
        long c = 5;
        long i = c;
        int w = (int)gc.startingMap(Planet.Mars).getWidth();
        for(c += tt; i < c; i++){
            gc.writeTeamArray(i, 0);
        }
        for(c += tf; i < c; i++){
            gc.writeTeamArray(i, (int)i);
        }
        for(c += ft; i < c; i++){
            gc.writeTeamArray(i, (int)i*w);
        }
        for(c += ff; i < c; i++){
            gc.writeTeamArray(i, (int)(i*w+i));
        }
    }
}
