package com.coolweather.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.LogUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherAcyivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private LinearLayout forecastLayout;

    private TextView titleCity, titleUpdateTime, degreeText, weatherInfoText, aqiText, pm25Text, comfortText, sportText, carWashText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        init();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);

        if (weatherString != null) {
            LogUtil.e("ghj","ttttt");
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weatherOfCity = getIntent().getStringExtra("weatherOfCity");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherOfCity);
        }
    }


    /*
    初始化控件
     */
    private void init() {
        weatherLayout = (ScrollView) findViewById(R.id.scrollView_weather);
        forecastLayout = (LinearLayout) findViewById(R.id.layout_forecast);

        titleCity = (TextView) findViewById(R.id.tv_title_city);
        titleUpdateTime = (TextView) findViewById(R.id.tv_title_update_time);
        degreeText = (TextView) findViewById(R.id.tv_degree);
        weatherInfoText = (TextView) findViewById(R.id.tv_weather_info);
        aqiText = (TextView) findViewById(R.id.tv_aqi);
        pm25Text = (TextView) findViewById(R.id.tv_pm2_5);
        comfortText = (TextView) findViewById(R.id.tv_comfort);
        sportText = (TextView) findViewById(R.id.tv_sport);
        carWashText = (TextView) findViewById(R.id.tv_car_wash);
    }


    /*
    根据城市id请求天气信息
     */
    public void requestWeather(final String weatherId) {
        Log.e("hya",weatherId);
        String weatherUrl = "https://free-api.heweather.com/v5/weather?city="+weatherId+"&key=4a6a5dd530cf4ea1bbde66c0ee0d5eec";
//        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=4a6a5dd530cf4ea1bbde66c0ee0d5eec";
        LogUtil.e("hya",weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(WeatherAcyivity.this, "获取天气信息失败Failed to obtain weather information", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().toString();
                LogUtil.e("WeatherActivity22",response.toString());
                LogUtil.e("WeatherActivity22",response.body().toString());
                Weather weather = Utility.handleWeatherResponse(responseText);

//                LogUtil.e("ok", weather.toString());
                runOnUiThread(() -> {
//                    if (weather != null && "OK".equals(weather.status.toUpperCase())) {
                    if (true) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherAcyivity.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                        showWeatherInfo(weather);
                    } else {
                        Toast.makeText(WeatherAcyivity.this, "Failed to obtain weather information", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();

        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dataText = (TextView) view.findViewById(R.id.tv_data);
            TextView infoText = (TextView) view.findViewById(R.id.tv_info);
            TextView maxText = (TextView) view.findViewById(R.id.tv_max);
            TextView minText = (TextView) view.findViewById(R.id.tv_min);

            dataText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            minText.setText(forecast.temperature.min);
            maxText.setText(forecast.temperature.max);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度" + weather.suggestion.comfort.info;
        String carWash = "洗车指数" + weather.suggestion.carWash.info;
        String sport = "运动指数" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
