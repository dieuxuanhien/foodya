package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.infrastructure.integration.GoongMapsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class GoongRouteDistanceAdapterIntegrationTests {

    @Autowired
    private GoongRouteDistanceAdapter goongRouteDistanceAdapter;

    @MockitoBean
    private GoongMapsClient goongMapsClient;

    @Test
    void routeDistanceKm_parsesMetersToKm() {
        given(goongMapsClient.routeDistanceRaw("10.1,106.1", "10.2,106.2"))
                .willReturn("{\"routes\":[{\"legs\":[{\"distance\":{\"value\":3456}}]}]}");

        BigDecimal km = goongRouteDistanceAdapter.routeDistanceKm(10.1, 106.1, 10.2, 106.2);

        assertEquals(new BigDecimal("3.456"), km);
    }

    @Test
    void routeDistanceKm_throwsWhenGoongPayloadMissingDistance() {
        given(goongMapsClient.routeDistanceRaw("10.1,106.1", "10.2,106.2"))
                .willReturn("{\"routes\":[]}");

        assertThrows(IllegalStateException.class,
                () -> goongRouteDistanceAdapter.routeDistanceKm(10.1, 106.1, 10.2, 106.2));
    }
}
