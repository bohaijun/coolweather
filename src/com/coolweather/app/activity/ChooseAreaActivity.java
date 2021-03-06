package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class ChooseAreaActivity extends Activity {
	
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	
	private List<String> dataList = new ArrayList<String>();
	
	/**
	 * 省份列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 省份列表
	 */
	private List<County> countyList;
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的地市
	 */
	private City selectedCity;
	/**
	 * 选中的区县
	 */
	private County selectedCounty;
	/**
	 * 选中的级别
	 */
	private int currentLevel;
	/**
	 *是否从 WeatherActivity跳转而来
	 */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市 且不是从WeatherActivity跳转而来的则直接打开weatheractivity
		if(prefs.getBoolean("city_selected", false) && !isFromWeatherActivity){
			//如果本地已经有天气信息，直接先加载本地信息
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			
			finish();
			return;
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				String weatherCode = "";
				if(currentLevel==LEVEL_PROVINCE){
					selectedProvince = provinceList.get(index);
					queryCities();
				}else if(currentLevel==LEVEL_CITY){
					selectedCity = cityList.get(index);
					queryCounties();
				}else if(currentLevel==LEVEL_COUNTY){
					selectedCounty = countyList.get(index);
					
					weatherCode = selectedCounty.getCountyCode();
					if(!TextUtils.isEmpty(weatherCode)){
						Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
						intent.putExtra("weather_code", weatherCode);
						startActivity(intent);
						finish();
					}
				}
				
			}
		});
		
		queryProvinces();
	}
	
	
	/**
	 * 加载全国省份数据，优先从数据库查询，如果没有查询到则从服务器上去查询
	 */
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvinces();
		
		if(provinceList!=null && provinceList.size()>0){
			dataList.clear();
			
			for(Province province : provinceList){
				dataList.add(province.getProvinceName());
			}
			
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromServer("province");
		}
	}
	
	/**
	 * 查询选中省份下面的所有地市
	 */
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		
		if(cityList!=null && cityList.size()>0){
			dataList.clear();
			
			for(City city : cityList){
				dataList.add(city.getCityName());
			}
			
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			
			currentLevel = LEVEL_CITY;
		}else{
			queryFromServer("city");
		}
	}
	
	/**
	 * 查询选中市下面的所有区县
	 */
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		
		if(countyList!=null && countyList.size()>0){
			dataList.clear();
			
			for(County county : countyList){
				dataList.add(county.getCountyName());
			}
			
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			
			currentLevel = LEVEL_COUNTY;
		}else{
			queryFromServer("county");
		}
	}
	
	/**
	 * 从服务器获取省市区数据
	 */
	private void queryFromServer(final String type){
		String address = "http://api.jisuapi.com/weather/city?appkey=3f0797ef7a27ca4e";
		
		showProgressDialog();
		
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = Utility.handleProvinceCityCountyResponse(coolWeatherDB, response);
				if(result){
					//通过runOnUiThread回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if("province".equalsIgnoreCase(type)){
								queryProvinces();
							}else if("city".equalsIgnoreCase(type)){
								queryCities();
							}else if("county".equalsIgnoreCase(type)){
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				//通过runOnUiThread回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		
		
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	
	
	/**
	 * 捕获back按键，根据当前的级别来进行判断，此时应该返回市列表，省份列表，还是直接退出
	 */
	@Override
	public void onBackPressed() {
		if(currentLevel==LEVEL_COUNTY){
			queryCities();
		}else if (currentLevel==LEVEL_CITY){
			queryProvinces();
		}else{
			if(isFromWeatherActivity){
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
}
