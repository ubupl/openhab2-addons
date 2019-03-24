package org.openhab.binding.jsupla.internal.cloud;

import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.api.generated.ApiClient;

@SuppressWarnings("PackageAccessibility")
public class ApiClientFactory {
    public static final ApiClientFactory FACTORY = new ApiClientFactory();

    public ApiClient newApiClient(String token, Logger logger) {
        final ApiClient apiClient = pl.grzeslowski.jsupla.api.ApiClientFactory.INSTANCE.newApiClient(token);
        if (logger != null) {
            apiClient.setDebugging(logger.isDebugEnabled(), new HttpLoggingInterceptor(logger::trace));
        }
        return apiClient;
    }

    public ApiClient newApiClient(String token) {
        return newApiClient(token, null);
    }
}
