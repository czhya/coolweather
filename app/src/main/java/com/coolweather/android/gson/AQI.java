package com.coolweather.android.gson;

/**
 * Created by 洪裕安 on 2017/9/16.
 */

public class AQI {
    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
