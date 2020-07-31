package helpers;

import java.util.concurrent.ThreadLocalRandom;

public class GenerationHelper {
    public static int getRandomValue(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    public static double getRandomValue(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
    
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
