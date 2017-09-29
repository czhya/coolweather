package com.hya.weather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 洪裕安 on 2017/9/16.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weatherId;
    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }

}
