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
import com.coolweather.app.service.AutoUpdateService;
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
		
		//��ʼ���ؼ�
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
			//�����������ʱ���ȥ��ѯ����
			publishText.setText("ͬ����...");
			
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherInfo(weatherCode);
		}else{
			//û�����������ʱ���ֱ����ʾ��������
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
			publishText.setText("ͬ����...");
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
	 * �����������Ų�ѯ����
	 * @param weatherCode
	 */
	private void queryWeatherInfo(String weatherCode){
		String address = "http://api.jisuapi.com/weather/query?appkey=3f0797ef7a27ca4e&citycode="+weatherCode;
		queryFromServer(address);
	}
	
	/**
	 * ���ݴ����address��ѯ������Ϣ
	 * @param address
	 */
	private void queryFromServer(String address){
		
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				//������������ص�response
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
						publishText.setText("ͬ��ʧ�ܣ�");
					}
				});
			}
		});
		
	}
	
	/**
	 * ��SharedPreferences�ļ��ж�ȡ�洢��������Ϣ������ʾ
	 */
	private void showWeather(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		templowText.setText(prefs.getString("templow", ""));
		temphighText.setText(prefs.getString("temphigh", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("����" + prefs.getString("publish_time", "") + "����");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}
	
}
