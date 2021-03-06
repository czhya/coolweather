package com.hya.weather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 洪裕安 on 2017/9/16.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;
    @SerializedName("cond")
    public More more;

    public class More {
        @SerializedName("txt")
        public String info;
    }
}