package com.coolweather.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;

public class Utility {
	
	/**
	 * 解析和处理服务器返回的省市县数据
	 */
	public synchronized static boolean handleProvinceCityCountyResponse(CoolWeatherDB coolWeatherDB, String response){
		if(!TextUtils.isEmpty(response)){
			try{
				JSONObject resultJSON = new JSONObject(response);
				String status = resultJSON.getString("status");
				if("0".equalsIgnoreCase(status)){
					Set<String> provinceIdSet = new HashSet<String>();
					Set<String> cityIdSet = new HashSet<String>();
					Set<String> cityProvinceIdSet = new HashSet<String>();//直辖市特殊处理
					
					JSONArray cityArray = resultJSON.getJSONArray("result");
					if(cityArray!=null && cityArray.length()>0){
						for(int i=0;i<cityArray.length();i++){
							JSONObject cityJson = cityArray.getJSONObject(i);
							
							String cityId = cityJson.getString("cityid");
							String parentid = cityJson.getString("parentid");
							String cityCode = cityJson.getString("citycode");
							String cityName = cityJson.getString("city");
							
							//parentid=0的是省份
							int parentidInt = Integer.parseInt(parentid);
							if(parentidInt==0){
								Province province = new Province();
								province.setId(Integer.parseInt(cityId));
								
								if(!TextUtils.isEmpty(cityCode)){
									cityProvinceIdSet.add(cityId);
								}
								//province.setProvinceCode(cityCode);
								province.setProvinceName(cityName);
								
								provinceIdSet.add(cityId);
								coolWeatherDB.saveProvince(province);
							}
						}
						
						
						for(int i=0;i<cityArray.length();i++){
							JSONObject cityJson = cityArray.getJSONObject(i);
							
							String cityId = cityJson.getString("cityid");
							String parentid = cityJson.getString("parentid");
							String cityCode = cityJson.getString("citycode");
							String cityName = cityJson.getString("city");
							
							int parentidInt = Integer.parseInt(parentid);
							if(provinceIdSet.contains(parentid) && (!cityProvinceIdSet.contains(parentid))){
								City city = new City();
								city.setId(Integer.parseInt(cityId));
								city.setCityCode(cityCode);
								city.setCityName(cityName);
								city.setProvinceId(parentidInt);
								
								cityIdSet.add(cityId);
								coolWeatherDB.saveCity(city);
							}else if(cityProvinceIdSet.contains(cityId)){
								City city = new City();
								city.setId(Integer.parseInt(cityId));
								city.setCityCode(cityCode);
								city.setCityName(cityName);
								city.setProvinceId(Integer.parseInt(cityId));
								
								cityIdSet.add(cityId);
								coolWeatherDB.saveCity(city);
							}
						}
						
						for(int i=0;i<cityArray.length();i++){
							JSONObject cityJson = cityArray.getJSONObject(i);
							
							String cityId = cityJson.getString("cityid");
							String parentid = cityJson.getString("parentid");
							String cityCode = cityJson.getString("citycode");
							String cityName = cityJson.getString("city");
							
							//parentid=0的是省份
							//parentid为1-34的为市
							//parentid大于34的为县
							int parentidInt = Integer.parseInt(parentid);
							if(cityIdSet.contains(parentid)){
								County county = new County();
								county.setId(Integer.parseInt(cityId));
								county.setCountyCode(cityCode);
								county.setCountyName(cityName);
								county.setCityId(parentidInt);
								
								coolWeatherDB.saveCounty(county);
							}
						}
						
					}
					return true;
				}else{
					String msg = resultJSON.getString("msg");
					throw new Exception(msg);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	
	/**
	 * 解析天气数据返回
	 * @param context
	 * @param response
	 */
	public static void handleWeatherResponse(Context context, String response){
		try{
			JSONObject jsonObject = new JSONObject(response);
			String status = jsonObject.getString("status");
			if("0".equalsIgnoreCase(status)){
				JSONObject resultJson = jsonObject.getJSONObject("result");
				
				String cityName = resultJson.getString("city");
				String weatherCode = resultJson.getString("citycode");
				String templow =  resultJson.getString("templow");
				String temphigh =  resultJson.getString("temphigh");
				String weatherDesp =  resultJson.getString("weather");
				String publishTime =  resultJson.getString("updatetime");
				
				saveWeatherInfo(context, cityName, weatherCode, templow, temphigh, weatherDesp, publishTime);
			}else{
				String msg = jsonObject.getString("msg");
				throw new Exception(msg);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 将服务器返回的所有天气信息存储到SharedPreferences文件中
	 * @param context
	 * @param cityName
	 * @param weatherCode
	 * @param templow
	 * @param temphigh
	 * @param weatherDesp
	 * @param publishTime
	 */
	public static void saveWeatherInfo(Context context, String cityName, String weatherCode, 
			String templow, String temphigh, String weatherDesp, String publishTime){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityName);
		editor.putString("weather_code", weatherCode);
		editor.putString("templow", templow);
		editor.putString("temphigh", temphigh);
		editor.putString("weather_desp", weatherDesp);
		editor.putString("publish_time", publishTime);
		editor.putString("current_date", sdf.format(new Date()));
		
		editor.commit();
	}
	
}
