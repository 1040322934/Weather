package com.example.lin.littleweather;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.lin.littleweather.gson.Forecast;
import com.example.lin.littleweather.gson.Weather;
import com.example.lin.littleweather.util.HttpUtil;
import com.example.lin.littleweather.util.Utility;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView city;
    private TextView update_time;
    private TextView degree;
    private TextView weather_info;
    private LinearLayout forecastLayout;
    private TextView aqi_text;
    private TextView pm25;
    private TextView comfort;;
    private TextView car_wash;
    private TextView sport;
    private ImageView bing_pic;
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button nav_btn;
    public  String  bingpic;
    private ChooseFragment chooseFragment;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        weatherLayout=findViewById(R.id.weatherLayout_weather);
        city=findViewById(R.id.city_title);
        update_time=findViewById(R.id.update_time_title);
        degree=findViewById(R.id.degree_now);
        weather_info=findViewById(R.id.weather_info_now);
        forecastLayout=findViewById(R.id.layout_forecast);
        aqi_text=findViewById(R.id.aqi_text_aqi);
        pm25=findViewById(R.id.pm25_aqi);
        comfort=findViewById(R.id.comfort_suggestion);
        car_wash=findViewById(R.id.car_wash_suggestion);
        sport=findViewById(R.id.sport_suggestion);
        bing_pic=findViewById(R.id.bing_pic_weather);
        swipeRefresh=findViewById(R.id.swipe_refresh_weather);
        drawerLayout=findViewById(R.id.drawrlayout_weather);
        nav_btn=findViewById(R.id.nav_btn_title);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        chooseFragment= (ChooseFragment) getFragmentManager().findFragmentById(R.id.choose_fragment);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        if(weatherString!=null){
            //有缓存的时候直接读取缓存中的天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            mWeatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            mWeatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        //加载必应的每日图片
        String bingpic=prefs.getString("bing_pic",null);
        if(bingpic!=null){
            Glide.with(this).load(bingpic).into(bing_pic);
        }else {
            showProgressDialog();
            loadBingPic();
        }
        swipeRefresh.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public boolean canChildScrollUp(@NonNull SwipeRefreshLayout parent, @Nullable View child) {
                return weatherLayout.getScrollY()>0;
            }
        });
        //下拉刷新的处理逻辑
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
                chooseFragment.loadBingPic();
            }
        });
        nav_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        //setDrawerLeftEdgeSize(this,drawerLayout,1);
    }

    //根据天气Id请求天气数据
    public void requestWeather(String weatherId){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+"&key=6e275ebdd3b54c0188dec5a27972a5dd";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor =PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId=weather.basic.weatherId;
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"请求天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    //处理并展示Weather实体类中的内容
    public void showWeatherInfo(Weather weather){
       city.setText(weather.basic.cityname);
       update_time.setText(weather.basic.update.updatetime.split(" ")[1]);
       degree.setText(weather.now.temperature+"°C");
       weather_info.setText(weather.now.more.info);
       forecastLayout.removeAllViews();
       for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView date_forecast=view.findViewById(R.id.date_forecast_item);
            TextView info_forecast=view.findViewById(R.id.info_forecast_item);
            TextView max_forecast=view.findViewById(R.id.max_forecast_item);
            TextView min_forecast=view.findViewById(R.id.min_forecast_item);
            date_forecast.setText(forecast.date);
            info_forecast.setText(forecast.more.info);
            max_forecast.setText(forecast.temperature.max);
            min_forecast.setText(forecast.temperature.min);
            forecastLayout.addView(view);
       }
        if(weather.aqi!=null){
           aqi_text.setText(weather.aqi.city.aqi);
           pm25.setText(weather.aqi.city.pm25);
        }
        comfort.setText("舒适度:"+weather.suggestion.comfort.info);
        car_wash.setText("洗车指数:"+weather.suggestion.carwash.info);
        sport.setText("运动建议:"+weather.suggestion.sport.info);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    //加载必应每日一图
    private void loadBingPic(){
        String requestBingpic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingpic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                closeProgressDialog();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                bingpic=response.body().string();
                SharedPreferences.Editor editor=  PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingpic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingpic).into(bing_pic);
                        closeProgressDialog();
                    }
                });
            }
        });
    }

    //显示进度对话框
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

   /* 全屏滑动(注意:有点灵敏emmm,稍微蹭一下都会划出来)
    private void setDrawerLeftEdgeSize (Activity activity, DrawerLayout drawerLayout, float displayWidthPercentage) {
        if (activity == null || drawerLayout == null) return;
        try {
            // 找到 ViewDragHelper 并设置 Accessible 为true
            Field leftDraggerField =
                    drawerLayout.getClass().getDeclaredField("mLeftDragger");//Right
            leftDraggerField.setAccessible(true);
            ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);

            // 找到 edgeSizeField 并设置 Accessible 为true
            Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edgeSize = edgeSizeField.getInt(leftDragger);

            // 设置新的边缘大小
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x *
                    displayWidthPercentage)));
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
    }*/

}
