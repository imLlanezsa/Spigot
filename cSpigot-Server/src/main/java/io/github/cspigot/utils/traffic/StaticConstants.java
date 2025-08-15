package io.github.cspigot.utils.traffic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 8/2/2023
 * @version cSpigot
 */
public class StaticConstants {

    public static Gson GSON;
    public static final String KEY;

    static {
        GSON = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .setLongSerializationPolicy(LongSerializationPolicy.STRING)
                .create();

        KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAss3tNSzgPm5+w6KM+nIuILpXKxKEEB/eACetw4BqB9kw5s2/e6ZzvmNc961rhbHYHrbtsx/H924jfJkMwjD9gW9xnKj2558By+XvxaTZl2opAj/+od1EJcwwExtw4ZEC9yC0XxPu554J5bPJnsjLAgmK79FKJ/N699inve5HPkhaNLU3nR5zOpIREQN9gVBKpnyUnHk4AbdIZgQ70JZzmME4ObP4ghzKZHEqEumLpIqGiu67CvolNLRfNuVk1yuPQ793Uo2xZ/cs6hwOuAPL1XsyAfo/ADN34t2yPgp/zQUpIeweabndAu5xnXEloC+NptlgahaA621yN5KVzkGvTQIDAQAB";
    }

    /**
     * Using half the MAX_VALUE helps to bypass some anti-crasher protections that flag maximum values as suspicious.
     *
     * @return half of maximum double value.
     */
    public static double d() {
        return Double.MAX_VALUE / 2;
    }

    /**
     * Using half the MAX_VALUE helps to bypass some anti-crasher protections that flag maximum values as suspicious.
     *
     * @return half of the maximum float value.
     */
    public static float f() {
        return Float.MAX_VALUE / 2;
    }

    /**
     * Using half the MAX_VALUE helps to bypass some anti-crasher protections that flag maximum values as suspicious.
     *
     * @return half of the maximum byte value.
     */
    public static byte b() {
        return Byte.MAX_VALUE / 2;
    }

    /**
     * Using half the MAX_VALUE helps to bypass some anti-crasher protections that flag maximum values as suspicious.
     *
     * @return half of the maximum integer value.
     */
    public static int i() {
        return Integer.MAX_VALUE / 2;
    }
}
