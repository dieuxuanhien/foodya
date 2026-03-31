package com.foodya.backend.application.ports.out;

public interface WeatherContextPort {

    String getCurrentWeatherRaw(double lat, double lng);
}
