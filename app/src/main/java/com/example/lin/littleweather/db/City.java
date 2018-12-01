package com.example.lin.littleweather.db;

import android.provider.ContactsContract;

import org.litepal.crud.DataSupport;

public class City extends DataSupport {
    private int id;
    private String CityName;
    private int CityId;
    private int ProvinceId;
    public int getId() {
        return id;
    }

    public String getCityName() {
        return CityName;
    }

    public int getCityId() {
        return CityId;
    }

    public int getProvinceId() {
        return ProvinceId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCityName(String cityName) {
        CityName = cityName;
    }

    public void setCityId(int cityId) {
        CityId = cityId;
    }

    public void setProvinceId(int provinceId) {
        ProvinceId = provinceId;
    }
}
