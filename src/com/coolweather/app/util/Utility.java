package com.coolweather.app.util;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

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
					
					JSONArray cityArray = resultJSON.getJSONArray("result");
					if(cityArray!=null && cityArray.length()>0){
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
							if(parentidInt==0){
								Province province = new Province();
								province.setId(Integer.parseInt(cityId));
								province.setProvinceCode(cityCode);
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
							
							//parentid=0的是省份
							//parentid为1-34的为市
							//parentid大于34的为县
							int parentidInt = Integer.parseInt(parentid);
							if(provinceIdSet.contains(parentid)){
								City city = new City();
								city.setId(Integer.parseInt(cityId));
								city.setCityCode(cityCode);
								city.setCityName(cityName);
								city.setProvinceId(parentidInt);
								
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
	
	
}
