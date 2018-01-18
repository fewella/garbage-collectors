package Utils;

import java.util.ArrayList;

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
    public static short[][] cloneMat(short[][] mat){
        short[][] out = new short[mat.length][];
        for(int i = 0; i < mat.length; i++) {
            short[] row = mat[i];
            int len = row.length;
            out[i] = new short[len];
            System.arraycopy(row, 0, out[i], 0, len);
        }
        return out;
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
