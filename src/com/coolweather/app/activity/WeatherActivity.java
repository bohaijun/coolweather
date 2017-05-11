package com.coolweather.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolweather.app.R;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class WeatherActivity extends Activity implements OnClickListener {
	
	
	private LinearLayout weatherInfoLayout;
	
	private TextView cityNameText;
	
	private TextView publishText;
	
	private TextView weatherDespText;
	
	private TextView templowText;
	
	private TextView temphighText;
	
	private TextView currentDateText;
	
	private Button switchCity;
	
	private Button refreshWeather;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		
		//初始化控件
		weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
		
		cityNameText = (TextView)findViewById(R.id.city_name);
		
		publishText = (TextView)findViewById(R.id.publish_text);
		
		weatherDespText = (TextView)findViewById(R.id.weather_desp);
		
		templowText = (TextView)findViewById(R.id.templow);
		
		temphighText = (TextView)findViewById(R.id.temphigh);
		
		currentDateText = (TextView)findViewById(R.id.current_date);
		
		switchCity = (Button)findViewById(R.id.switch_city);
		
		refreshWeather = (Button)findViewById(R.id.refresh_weather);
		
		
		String weatherCode = getIntent().getStringExtra("weather_code");
		if(!TextUtils.isEmpty(weatherCode)){
			//有天气编码的时候就去查询天气
			publishText.setText("同步中...");
			
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherInfo(weatherCode);
		}else{
			//没有天气编码的时候就直接显示本地天气
			showWeather();
		}
		
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
	}
	
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.switch_city:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			
			break;
		case R.id.refresh_weather:
			publishText.setText("同步中...");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if(!TextUtils.isEmpty(weatherCode)){
				queryWeatherInfo(weatherCode);
			}
			break;
		default:
			break;
		}
		
	}
	
	/**
	 * 根据天气代号查询天气
	 * @param weatherCode
	 */
	private void queryWeatherInfo(String weatherCode){
		String address = "http://api.jisuapi.com/weather/query?appkey=3f0797ef7a27ca4e&citycode="+weatherCode;
		queryFromServer(address);
	}
	
	/**
	 * 根据传入的address查询天气信息
	 * @param address
	 */
	private void queryFromServer(String address){
		
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				//处理服务器返回的response
				Utility.handleWeatherResponse(WeatherActivity.this, response);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showWeather();
					}
				});
			}
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						publishText.setText("同步失败！");
					}
				});
			}
		});
		
	}
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示
	 */
	private void showWeather(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		templowText.setText(prefs.getString("templow", ""));
		temphighText.setText(prefs.getString("temphigh", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
	}
	
}
