package org.secuso.privacyfriendlyweather.database;


/**
 * Created by yonjuni on 04.01.17.
 * data object for city
 * <p>
 * Structure taken from the old orm package from previous versions of this app.
 */

public class City {

    private static final String UNKNOWN_POSTAL_CODE_VALUE = "-";

    private int cityId;
    private String cityName;
    private String countryCode;
    private float lon;
    private float lat;

    public City() {
    }

    public City(int cityId, String cityName, String countryCode, float lon, float lat) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.countryCode = countryCode;
        this.lon = lon;
        this.lat = lat;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public String toString() {
        return String.format("%s, %s (%f - %f)", cityName, countryCode, lon, lat);
    }

    public void setLatitude(float latitude) {
        lat = latitude;
    }

    public float getLatitude() {
        return lat;
    }

    public float getLongitude() {
        return lon;
    }

    public void setLongitude(float lon) {
        this.lon = lon;
    }
}
