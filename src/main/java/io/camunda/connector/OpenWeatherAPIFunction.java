package io.camunda.connector;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@OutboundConnector(
        name = "OpenWeatherAPI", inputVariables = {"latitude", "longitude", "units"}, type = "io.camunda:weather-api:1")
public class OpenWeatherAPIFunction implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenWeatherAPIFunction.class);

  @Override
  public Object execute(OutboundConnectorContext context) throws Exception {
    var connectorRequest = context.getVariablesAsType(OpenWeatherAPIRequest.class);
    context.replaceSecrets(connectorRequest);
    return executeConnector(connectorRequest);
  }

  private OpenWeatherAPIResult executeConnector(final OpenWeatherAPIRequest connectorRequest) throws IOException {
    // TODO: implement connector logic
    LOGGER.info("Executing OpenWeather API connector with request {}", connectorRequest);
    String urlString = "\n" +
            "https://api.openweathermap.org/data/2.5/weather?appid=" + connectorRequest.getApiKey() +
            "&lat=" + connectorRequest.getLatitude() + "&lon=" + connectorRequest.getLongitude();
    URL url = new URL(urlString);
    HttpURLConnection http = (HttpURLConnection)url.openConnection();
    http.setRequestProperty("Accept", "application/json");

    http.disconnect();

    String weatherReport;
    if (http.getResponseCode() == 200) {
      weatherReport= convertInputStreamToString(http.getInputStream());
    } else {
      weatherReport = "{}";
    }

    var result = new OpenWeatherAPIResult();
    result.setResult("{\"code\": " + http.getResponseCode() + ", \"forecast\": " + weatherReport + "}");
    result.setCode(http.getResponseCode());
    return result;
  }

  /**
   * Reads the input stream line-by-line and returns its content in <code>String</code> representation.
   *
   * @param inputStream input stream to convert.
   * @return converted <code>InputStream</code> content.
   * @throws IllegalArgumentException in case if input stream is unable to be read.
   */
  private static String convertInputStreamToString(InputStream inputStream) {
    StringBuilder result = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
      }
    } catch (IOException ex) {
      LOGGER.error("Error during response reading: ", ex);
      return "{}";
    }

    return result.toString();
  }
}