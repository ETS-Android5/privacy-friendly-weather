package org.secuso.privacyfriendlyweather.weather_api.open_weather_map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.secuso.privacyfriendlyweather.R;
import org.secuso.privacyfriendlyweather.database.CityToWatch;
import org.secuso.privacyfriendlyweather.database.CurrentWeatherData;
import org.secuso.privacyfriendlyweather.database.Forecast;
import org.secuso.privacyfriendlyweather.database.WeekForecast;
import org.secuso.privacyfriendlyweather.database.PFASQLiteHelper;
import org.secuso.privacyfriendlyweather.ui.updater.ViewUpdater;
import org.secuso.privacyfriendlyweather.weather_api.IDataExtractor;
import org.secuso.privacyfriendlyweather.weather_api.IProcessHttpRequest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class processes the HTTP requests that are made to the OpenWeatherMap API requesting the
 * current weather for all stored cities.
 */
public class ProcessOwmForecastOneCallAPIRequest implements IProcessHttpRequest {

    /**
     * Constants
     */
    private final String DEBUG_TAG = "process_forecast";

    /**
     * Member variables
     */
    private Context context;
    private PFASQLiteHelper dbHelper;

    /**
     * Constructor.
     *
     * @param context The context of the HTTP request.
     */
    public ProcessOwmForecastOneCallAPIRequest(Context context) {
        this.context = context;
        this.dbHelper = PFASQLiteHelper.getInstance(context);
    }

    /**
     * Converts the response to JSON and updates the database. Note that for this method no
     * UI-related operations are performed.
     *
     * @param response The response of the HTTP request.
     */
    @Override
    public void processSuccessScenario(String response) {
        IDataExtractor extractor = new OwmDataExtractor();
        try {
            JSONObject json = new JSONObject(response);
            float lat = (float)json.getDouble("lat");
            float lon = (float)json.getDouble("lon");
 //           Log.d("URL JSON",Float.toString(lat));
 //           Log.d("URL JSON",Float.toString(lon));
            int cityId=0;
            //get CityID from lat/lon
            //Maybe a risk of rounding problems. Alternative: search closest citytowatch
            List<CityToWatch> citiesToWatch = dbHelper.getAllCitiesToWatch();
            for (int i = 0; i < citiesToWatch.size(); i++) {
                CityToWatch city = citiesToWatch.get(i);
                //if lat/lon of json response very close to lat/lon in citytowatch
                if ((Math.abs(city.getLatitude() - lat)<0.01) && (Math.abs(city.getLongitude() - lon)<0.01)) {
                    cityId=city.getCityId();
 //                   Log.d("URL CITYID", Integer.toString(cityId));
                    break;
                }
            }

            String rain60min="\u2614 60\u200amin:    no data";
            if (json.has("minutely")) {
                rain60min="\u2614 60\u200amin:    ";
                JSONArray listrain = json.getJSONArray("minutely");
                for (int i = 0; i < listrain.length()/5; i++) {   //evaluate in 5min intervals
                    String currentItem0 = listrain.get(i*5).toString();
                    String currentItem1 = listrain.get(i*5+1).toString();
                    String currentItem2 = listrain.get(i*5+2).toString();
                    String currentItem3 = listrain.get(i*5+3).toString();
                    String currentItem4 = listrain.get(i*5+4).toString();
                    rain60min += extractor.extractRain60min(currentItem0,currentItem1,currentItem2,currentItem3,currentItem4);
                }
            }

            CurrentWeatherData weatherData = extractor.extractCurrentWeatherDataOneCall(json.getString("current"));

            if (weatherData == null) {
                final String ERROR_MSG = context.getResources().getString(R.string.convert_to_json_error);
                Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
            } else {
                weatherData.setCity_id(cityId);
                weatherData.setRain60min(rain60min);
                weatherData.setTimeZoneSeconds(json.getInt("timezone_offset"));
                CurrentWeatherData current = dbHelper.getCurrentWeatherByCityId(cityId);
                if (current != null && current.getCity_id() == cityId) {
                    dbHelper.updateCurrentWeather(weatherData);
                } else {
                    dbHelper.addCurrentWeather(weatherData);
                }

                ViewUpdater.updateCurrentWeatherData(weatherData);
            }




            JSONArray listdaily = json.getJSONArray("daily");

            dbHelper.deleteWeekForecastsByCityId(cityId);
            List<WeekForecast> weekforecasts = new ArrayList<>();

            for (int i = 0; i < listdaily.length(); i++) {
                String currentItem = listdaily.get(i).toString();
                WeekForecast forecast = extractor.extractWeekForecast(currentItem);
                // Data were not well-formed, abort
                if (forecast == null) {
                    final String ERROR_MSG = context.getResources().getString(R.string.convert_to_json_error);
                    Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
                    return;
                }
                // Could retrieve all data, so proceed
                else {
                    forecast.setCity_id(cityId);
                    // add it to the database
                    dbHelper.addWeekForecast(forecast);
                    weekforecasts.add(forecast);
                }
            }

            ViewUpdater.updateWeekForecasts(weekforecasts);

            //Use hourly data only if forecastChoice 2 (1h) is active
            SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            int choice = Integer.parseInt(prefManager.getString("forecastChoice","1"));
            if (choice==2) {
                JSONArray listhourly = json.getJSONArray("hourly");

                dbHelper.deleteForecastsByCityId(cityId);
                List<Forecast> hourlyforecasts = new ArrayList<>();

                for (int i = 0; i < listhourly.length(); i++) {
                    String currentItem = listhourly.get(i).toString();
                    Forecast forecast = extractor.extractHourlyForecast(currentItem);
                    // Data were not well-formed, abort
                    if (forecast == null) {
                        final String ERROR_MSG = context.getResources().getString(R.string.convert_to_json_error);
                        Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Could retrieve all data, so proceed
                    else {
                        forecast.setCity_id(cityId);
                        // add it to the database
                        dbHelper.addForecast(forecast);
                        hourlyforecasts.add(forecast);
                    }
                }

                ViewUpdater.updateForecasts(hourlyforecasts);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows an error that the data could not be retrieved.
     *
     * @param error The error that occurred while executing the HTTP request.
     */
    @Override
    public void processFailScenario(final VolleyError error) {
        Handler h = new Handler(this.context.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, context.getResources().getString(R.string.error_fetch_forecast), Toast.LENGTH_LONG).show();
            }
        });
    }

}
