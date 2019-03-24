package org.openhab.binding.supla.internal.cloud;

import org.slf4j.Logger;
import pl.grzeslowski.jsupla.api.OneLineHttpLoggingInterceptor;
import pl.grzeslowski.jsupla.api.generated.ApiClient;

import static com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.BODY;

public class ApiClientFactory {
    public static final ApiClientFactory FACTORY = new ApiClientFactory();

    public ApiClient newApiClient(String token, Logger logger) {
        final ApiClient apiClient = pl.grzeslowski.jsupla.api.ApiClientFactory.INSTANCE.newApiClient(token);
        if (logger != null) {
            apiClient.setDebugging(logger.isDebugEnabled(), new OneLineHttpLoggingInterceptor(logger::trace, BODY));
        }
        return apiClient;
    }
}
