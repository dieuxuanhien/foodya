package com.foodya.backend.application.port.out;

public interface WeatherContextPort {

    String getCurrentWeatherRaw(double lat, double lng);
}
