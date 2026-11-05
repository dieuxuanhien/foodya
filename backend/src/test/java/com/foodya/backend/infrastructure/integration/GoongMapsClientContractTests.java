package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoongMapsClientContractTests {

    private MockRestServiceServer server;
    private GoongMapsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://goong.test");
        server = MockRestServiceServer.bindTo(builder).build();

        ApiSecretsProvider secretsProvider = mock(ApiSecretsProvider.class);
        when(secretsProvider.get(IntegrationKeyCatalog.GOONG_API_KEY))
                .thenReturn(java.util.Optional.of("test-api-key"));
        client = new GoongMapsClient(secretsProvider, builder.build());
    }

    @Test
    void routeDistanceRaw_usesV2DirectionContract() {
        server.expect(method(GET))
            .andExpect(request -> assertEquals("/v2/direction", request.getURI().getPath()))
                .andExpect(queryParam("origin", "10.1,106.1"))
                .andExpect(queryParam("destination", "10.2,106.2"))
                .andExpect(queryParam("vehicle", "bike"))
                .andExpect(queryParam("api_key", "test-api-key"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        assertEquals("{}", client.routeDistanceRaw("10.1,106.1", "10.2,106.2"));
        server.verify();
    }

    @Test
    void reverseGeocodeRaw_usesV2GeocodeContract() {
        server.expect(method(GET))
            .andExpect(request -> assertEquals("/v2/geocode", request.getURI().getPath()))
                .andExpect(queryParam("latlng", "10.1,106.1"))
                .andExpect(queryParam("language", "vi"))
                .andExpect(queryParam("has_deprecated_administrative_unit", "false"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        assertEquals("{}", client.reverseGeocodeRaw("10.1,106.1"));
        server.verify();
    }

    @Test
    void tripRouteRaw_includesWaypointsAndDeliveryDefaults() {
        server.expect(method(GET))
            .andExpect(request -> assertEquals("/v2/trip", request.getURI().getPath()))
                .andExpect(queryParam("origin", "10.1,106.1"))
                .andExpect(queryParam("destination", "10.3,106.3"))
                .andExpect(queryParam("waypoints", "10.2,106.2"))
                .andExpect(queryParam("vehicle", "bike"))
                .andExpect(queryParam("roundtrip", "false"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        assertEquals("{}", client.tripRouteRaw("10.1,106.1", "10.3,106.3", "10.2,106.2", null, false));
        server.verify();
    }
}