package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by 洪裕安 on 2017/9/16.
 */

public class Weather {

    public String status;
    public Basic basic;
    public AQI aqi;
    public Suggestion suggestion;
    public Now now;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
