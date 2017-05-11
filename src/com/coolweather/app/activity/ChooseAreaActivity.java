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
	 * ʡ���б�
	 */
	private List<Province> provinceList;
	/**
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ʡ���б�
	 */
	private List<County> countyList;
	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;
	/**
	 * ѡ�еĵ���
	 */
	private City selectedCity;
	/**
	 * ѡ�е�����
	 */
	private County selectedCounty;
	/**
	 * ѡ�еļ���
	 */
	private int currentLevel;
	/**
	 *�Ƿ�� WeatherActivity��ת����
	 */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//�Ѿ�ѡ���˳��� �Ҳ��Ǵ�WeatherActivity��ת��������ֱ�Ӵ�weatheractivity
		if(prefs.getBoolean("city_selected", false) && !isFromWeatherActivity){
			//��������Ѿ���������Ϣ��ֱ���ȼ��ر�����Ϣ
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
	 * ����ȫ��ʡ�����ݣ����ȴ����ݿ��ѯ�����û�в�ѯ����ӷ�������ȥ��ѯ
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
			titleText.setText("�й�");
			
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromServer("province");
		}
	}
	
	/**
	 * ��ѯѡ��ʡ����������е���
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
	 * ��ѯѡ�����������������
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
	 * �ӷ�������ȡʡ��������
	 */
	private void queryFromServer(final String type){
		String address = "http://api.jisuapi.com/weather/city?appkey=3f0797ef7a27ca4e";
		
		showProgressDialog();
		
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = Utility.handleProvinceCityCountyResponse(coolWeatherDB, response);
				if(result){
					//ͨ��runOnUiThread�ص����̴߳����߼�
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
				//ͨ��runOnUiThread�ص����̴߳����߼�
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		
		
	}
	
	/**
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	
	
	/**
	 * ����back���������ݵ�ǰ�ļ����������жϣ���ʱӦ�÷������б�ʡ���б�����ֱ���˳�
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
