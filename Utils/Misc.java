package Utils;

import bc.MapLocation;

import java.util.ArrayList;
import java.util.Comparator;

public class Misc {
    public static int binarySearch(ArrayList<Integer> arr, int target){
        int high = arr.size();
        int low = 0;
        int mid;
        while (low < high) {
            mid = (high + low) / 2;
            if (arr.get(mid) < target)
                low = mid + 1;
            else if (arr.get(mid) == target)
                return mid;
            else
                high = mid;
        }
        return low;
    }
    public static int[][] cloneMat(int[][] mat){
        int[][] out = new int[mat.length][];
        for(int i = 0; i < mat.length; i++) {
            int[] row = mat[i];
            int len = row.length;
            out[i] = new int[len];
            System.arraycopy(row, 0, out[i], 0, len);
        }
        return out;
    }
}
