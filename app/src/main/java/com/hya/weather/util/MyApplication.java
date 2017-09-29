package com.hya.weather.util;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePal;

/**
 * Created by 洪裕安 on 2017/9/16.
 */

public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        context = getApplicationContext();
        LitePal.initialize(context);
    }

    public static Context getContext(){
        return context;
    }
}
