package org.openhab.binding.librenms.internal.connection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.librenms.internal.*;
import org.openhab.binding.librenms.internal.dto.LibrenmsDevice;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpStatus.*;
import org.openhab.core.cache.ByteArrayFileCache;
import org.openhab.core.cache.ExpiringCacheMap;
import org.openhab.core.i18n.CommunicationException;
import org.openhab.core.i18n.ConfigurationException;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.RawType;

public class librenmsConnection {
   
    private final Logger logger = LoggerFactory.getLogger(librenmsConnection.class);

    private static final String PROPERTY_MESSAGE = "message";
    private static final String API_KEY_HEADERNAME = "X-Auth-Token";

    private static final String API_PATH_DEVICE = "device";

    private final librenmsHandler handler;
    private final HttpClient httpClient;
    private final librenmsConfiguration config;

    private final Gson gson = new Gson();

    public librenmsConnection(librenmsHandler handler, HttpClient httpClient) {
        this.handler = handler;
        this.httpClient = httpClient;
        this.config = handler.getlibrenmsConfiguration();
    }

    private String buildBaseUrl() {
        return "https://" + this.config.hostname + "api" + this.config.apiVersion;
    }

    private LibrenmsDevice getDevice(int id)
    {
        var url = buildBaseUrl() + "/" + API_PATH_DEVICE + "/" + id; 
        var resp = getResponse(url);
        return gson.fromJson(resp, LibrenmsDevice.class);
    }


    private String getResponse(String url) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("OpenWeatherMap request: URL = '{}'", url);
            }
            ContentResponse contentResponse = httpClient.newRequest(url).method(GET).timeout(10, TimeUnit.SECONDS)
                    .send();
            int httpStatus = contentResponse.getStatus();
            String content = contentResponse.getContentAsString();
            String errorMessage = "";
            logger.trace("OpenWeatherMap response: status = {}, content = '{}'", httpStatus, content);
            switch (httpStatus) {
                case OK_200:
                    return content;
                case BAD_REQUEST_400:
                case UNAUTHORIZED_401:
                case NOT_FOUND_404:
                    errorMessage = getErrorMessage(content);
                    logger.debug("OpenWeatherMap server responded with status code {}: {}", httpStatus, errorMessage);
                    throw new ConfigurationException(errorMessage);
                case TOO_MANY_REQUESTS_429:
                    // TODO disable refresh job temporarily (see https://openweathermap.org/appid#Accesslimitation)
                default:
                    errorMessage = getErrorMessage(content);
                    logger.debug("OpenWeatherMap server responded with status code {}: {}", httpStatus, errorMessage);
                    throw new CommunicationException(errorMessage);
            }
        } catch (ExecutionException e) {
            String errorMessage = e.getMessage();
            logger.debug("ExecutionException occurred during execution: {}", errorMessage, e);
            if (e.getCause() instanceof HttpResponseException) {
                logger.debug("OpenWeatherMap server responded with status code {}: Invalid API key.", UNAUTHORIZED_401);
                throw new ConfigurationException("@text/offline.conf-error-invalid-apikey", e.getCause());
            } else {
                throw new CommunicationException(
                        errorMessage == null ? "@text/offline.communication-error" : errorMessage, e.getCause());
            }
        } catch (TimeoutException e) {
            String errorMessage = e.getMessage();
            logger.debug("TimeoutException occurred during execution: {}", errorMessage, e);
            throw new CommunicationException(errorMessage == null ? "@text/offline.communication-error" : errorMessage,
                    e.getCause());
        } catch (InterruptedException e) {
            String errorMessage = e.getMessage();
            logger.debug("InterruptedException occurred during execution: {}", errorMessage, e);
            Thread.currentThread().interrupt();
            throw new CommunicationException(errorMessage == null ? "@text/offline.communication-error" : errorMessage,
                    e.getCause());
        }
    }


    private String getErrorMessage(String response) {
        JsonElement jsonResponse = JsonParser.parseString(response);
        if (jsonResponse.isJsonObject()) {
            JsonObject json = jsonResponse.getAsJsonObject();
            if (json.has(PROPERTY_MESSAGE)) {
                return json.get(PROPERTY_MESSAGE).getAsString();
            }
        }
        return response;
    }
}
