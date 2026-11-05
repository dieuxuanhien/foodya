package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WeatherCacheServiceTest {

    private WeatherContextPort weatherContextPort;
    private GeoPort geoPort;
    private InMemoryCache cachePort;
    private WeatherCacheService service;

    @BeforeEach
    void setUp() {
        weatherContextPort = mock(WeatherContextPort.class);
        geoPort = mock(GeoPort.class);
        when(geoPort.geoToH3Index(anyDouble(), anyDouble(), eq(8))).thenReturn("8829a4825bfffff");
        cachePort = new InMemoryCache();
        service = new WeatherCacheService(weatherContextPort, cachePort, geoPort);
    }

    @Test
    void cachesByH3Res8CellSoSecondLookupSkipsProvider() {
        WeatherData data = WeatherData.of("Rain", "Moderate rain", 28.0);
        when(weatherContextPort.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(Optional.of(data));

        var first = service.getWeatherForLocation(10.75, 106.75);
        var second = service.getWeatherForLocation(10.7501, 106.7501);

        assertThat(first.data()).isEqualTo(data);
        assertThat(second.data()).isEqualTo(data);
        assertThat(second.h3Res8Cell()).isEqualTo("8829a4825bfffff");
        verify(weatherContextPort, times(1)).getCurrentWeather(anyDouble(), anyDouble());
    }

    @Test
    void providerFailureIsCachedAsUnavailableToAvoidHammering() {
        when(weatherContextPort.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertThat(service.getWeatherForLocation(10.75, 106.75).available()).isFalse();
        assertThat(service.getWeatherForLocation(10.75, 106.75).available()).isFalse();

        verify(weatherContextPort, times(1)).getCurrentWeather(anyDouble(), anyDouble());
    }

    @Test
    void providerExceptionIsSwallowedAndReportedUnavailable() {
        when(weatherContextPort.getCurrentWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("boom"));

        var result = service.getWeatherForLocation(10.75, 106.75);

        assertThat(result.available()).isFalse();
        assertThat(result.h3Res8Cell()).isEqualTo("8829a4825bfffff");
    }

    @Test
    void evictForcesNextCallToHitProvider() {
        when(weatherContextPort.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(WeatherData.of("Clear", "Clear sky", 30.0)));
        service.getWeatherForLocation(10.75, 106.75);

        service.evict("8829a4825bfffff");
        service.getWeatherForLocation(10.75, 106.75);

        verify(weatherContextPort, times(2)).getCurrentWeather(anyDouble(), anyDouble());
    }
}
