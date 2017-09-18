package com.coolweather.android;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.LogUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    final static String SUNNY100 = "晴";
    final static String CLOUDY101 = "多云";
    final static String FEWCLOUDS102 = "少云";
    final static String PARTLYCLOUDY103 = "晴间多云";
    final static String OVERCAST104 = "阴";
    final static String SHOWERRAIN300 = "阵雨";
    final static String HEAVYSHOWER_RAIN301 = "强阵雨";
    final static String THUNDERSHOWER_RAIN302 = "雷阵雨";
    final static String LIGHT_RAIN305 = "小雨";
    final static String MODERATE_RAIN306 = "中雨";
    final static String HEAVY_RAIN307 = "大雨";
    final static String STORM_RAIN310 = "暴雨";
    final static String HEAVYSTORM_RAIN311 = "大暴雨";
    final static String SEVERESTORM_RAIN312 = "特大暴雨";
    final static String LIGHT_SNOW400 = "小雪";
    final static String MODERATE_SNOW401 = "中雪";
    final static String HEAVY_SNOW402 = "大雪";
    final static String STORM_SNOW403 = "暴雪";
    final static String SLEET404 = "雨夹雪";
    final static String RAINANDSNOW405 = "雨雪天气";


    public DrawerLayout drawerLayout;

    public SwipeRefreshLayout swipeRefresh;

    private ScrollView weatherLayout;

    private Button navButton;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    private static String mWeatherId;

    private LocationClient client;
    private static String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        client = new LocationClient(getApplicationContext());
        client.registerLocationListener(new LocationUtil());
        setContentView(R.layout.activity_weather);
        if (permission()) {
            requestLocation();
        }
        init();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        mWeatherId = address;
        Weather weather;

        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            weather = Utility.handleWeatherResponse(weatherString);
            LogUtil.e("Weather", "" + weather.basic.cityName.equals(mWeatherId) + "  " + mWeatherId + "  " + weather.basic.cityName);
            if (!weather.basic.cityName.equals(mWeatherId) && mWeatherId != null) {
                weatherLayout.setVisibility(View.INVISIBLE);
                requestWeather(mWeatherId);
            } else {
                showWeatherInfo(weather);
            }
        } else {
            // 无缓存时去服务器查询天气
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }


        swipeRefresh.setOnRefreshListener(() -> {
            SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(this);
            String weatherString2 = prefs2.getString("weather", null);
            mWeatherId = Utility.handleWeatherResponse(weatherString2).basic.weatherId;
            requestWeather(mWeatherId);
        });
        navButton.setOnClickListener((View v) -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (permission()) {
            requestLocation();
        }
    }

    private boolean permission() {
        boolean permission = false;
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(WeatherActivity.this, permissions, 1);
        } else {
            LogUtil.e("Weather", "permission");
            permission = true;
//            requestLocation();
        }
        return permission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogUtil.e("MainActivity", "onRequestPermissionsResult");
        boolean state = true;
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            state = false;
                            Toast.makeText(this, "必须同意所有权限才能获得更好的服务", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_LONG).show();
                    finish();
                }
                if (state) {
                    requestLocation();
                }
                break;
        }
    }

    private void requestLocation() {
        LogUtil.e("Weather", "requestLocation");
        initLocation();
        client.start();
    }

    private void initLocation() {
        LogUtil.e("Weather", "initLocation");
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(50);
        option.setIsNeedAddress(true);
        client.setLocOption(option);
    }


    class LocationUtil implements BDLocationListener {
        @Override
        public void onReceiveLocation(final BDLocation bdLocation) {
//            runOnUiThread(() -> {
            String weatherId = bdLocation.getDistrict();
            address = weatherId;
//            });
        }
    }


    // 初始化各控件
    void init() {
        bingPicImg = (ImageView) findViewById(R.id.image_bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.scrollView_weather);
        titleCity = (TextView) findViewById(R.id.tv_title_city);
        titleUpdateTime = (TextView) findViewById(R.id.tv_title_update_time);
        degreeText = (TextView) findViewById(R.id.tv_degree);
        weatherInfoText = (TextView) findViewById(R.id.tv_weather_info);
        forecastLayout = (LinearLayout) findViewById(R.id.layout_forecast);
        aqiText = (TextView) findViewById(R.id.tv_aqi);
        pm25Text = (TextView) findViewById(R.id.tv_pm2_5);
        comfortText = (TextView) findViewById(R.id.tv_comfort);
        carWashText = (TextView) findViewById(R.id.tv_car_wash);
        sportText = (TextView) findViewById(R.id.tv_sport);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.layout_swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout) findViewById(R.id.layout_drawer);
        navButton = (Button) findViewById(R.id.but_nav);
    }

    /**
     * 根据天气id请求城市天气信息。
     */
    public void requestWeather(final String weatherId) {
        LogUtil.e("requestWeather", weatherId);
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=4a6a5dd530cf4ea1bbde66c0ee0d5eec";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(() -> {
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                        mWeatherId = weather.basic.weatherId;
                        showWeatherInfo(weather);
                    } else {

                        Toast.makeText(WeatherActivity.this, "Failed to obtain weather information", Toast.LENGTH_SHORT).show();
                    }
                    swipeRefresh.setRefreshing(false);

                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(WeatherActivity.this, "Failed to obtain weather information", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);

                });
            }
        });
        loadBingPic();
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(() -> {
                    Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);

                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据。
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + degreeText.getText().toString().substring(degreeText.getText().toString().length() - 1);
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.tv_data);
            ImageView infoImage = (ImageView) view.findViewById(R.id.image_info);
            TextView infoText = (TextView) view.findViewById(R.id.tv_info);
            TextView maxText = (TextView) view.findViewById(R.id.tv_max);
            TextView minText = (TextView) view.findViewById(R.id.tv_min);
            dateText.setText(forecast.date);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            infoText.setText(forecast.more.info);
            LogUtil.e("infotext", forecast.more.info.toString() + " " + forecast.more.info.toString().equals("晴") + " " + SUNNY100);
            switch (forecast.more.info) {
                case SUNNY100:
                    infoImage.setImageResource(R.drawable.s100);
                    break;
                case CLOUDY101:
                    infoImage.setImageResource(R.drawable.s101);
                    break;
                case FEWCLOUDS102:
                    infoImage.setImageResource(R.drawable.s102);
                    break;
                case PARTLYCLOUDY103:
                    infoImage.setImageResource(R.drawable.s103);
                    break;
                case OVERCAST104:
                    infoImage.setImageResource(R.drawable.s104);
                    break;
                case SHOWERRAIN300:
                    infoImage.setImageResource(R.drawable.s300);
                    break;
                case HEAVYSHOWER_RAIN301:
                    infoImage.setImageResource(R.drawable.s301);
                    break;
                case THUNDERSHOWER_RAIN302:
                    infoImage.setImageResource(R.drawable.s302);
                    break;
                case LIGHT_RAIN305:
                    infoImage.setImageResource(R.drawable.s305);
                    break;
                case MODERATE_RAIN306:
                    infoImage.setImageResource(R.drawable.s306);
                    break;
                case HEAVY_RAIN307:
                    infoImage.setImageResource(R.drawable.s307);
                    break;
                case STORM_RAIN310:
                    infoImage.setImageResource(R.drawable.s310);
                    break;
                case HEAVYSTORM_RAIN311:
                    infoImage.setImageResource(R.drawable.s311);
                    break;
                case SEVERESTORM_RAIN312:
                    infoImage.setImageResource(R.drawable.s312);
                    break;
                case LIGHT_SNOW400:
                    infoImage.setImageResource(R.drawable.s400);
                    break;
                case MODERATE_SNOW401:
                    infoImage.setImageResource(R.drawable.s401);
                    break;
                case HEAVY_SNOW402:
                    infoImage.setImageResource(R.drawable.s402);
                    break;
                case STORM_SNOW403:
                    infoImage.setImageResource(R.drawable.s403);
                    break;
                case SLEET404:
                    infoImage.setImageResource(R.drawable.s404);
                    break;
                case RAINANDSNOW405:
                    infoImage.setImageResource(R.drawable.s405);
                    break;
                default:
                    break;
            }


            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = comfortText.getText().toString().substring(0, 4) + weather.suggestion.comfort.info;
        String carWash = carWashText.getText().toString().substring(0, 5) + weather.suggestion.carWash.info;
        String sport = sportText.getText().toString().substring(0, 5) + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

}
