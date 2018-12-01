package com.example.lin.littleweather.db;

import org.litepal.crud.DataSupport;

public class County extends DataSupport {
    private int id;
    private String CountyName;
    private String WeatherId;
    private int CityId;
    private int CountyId;

    public int getId() {
        return id;
    }

    public String getCountyName() {
        return CountyName;
    }

    public String getWeatherId() {
        return WeatherId;
    }

    public int getCityId() {
        return CityId;
    }

    public int getCountyId() {
        return CountyId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCountyName(String countyName) {
        CountyName = countyName;
    }

    public void setWeatherId(String weatherId) {
        WeatherId = weatherId;
    }

    public void setCityId(int cityId) {
        CityId = cityId;
    }

    public void setCountyId(int countyId) {
        CountyId = countyId;
    }
}
