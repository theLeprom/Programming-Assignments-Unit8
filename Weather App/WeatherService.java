import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * WeatherService encapsulates all communication with the OpenWeatherMap API.
 * It fetches current weather data and a 5-day forecast for a given city, then
 * parses the JSON response into WeatherData objects without any external
 * library dependencies. Java 11's built-in HttpClient is used for HTTP calls.
 *
 * @author Lerato Maseko
 */
public class WeatherService
{
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final DateTimeFormatter DT_IN  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DT_OUT =
            DateTimeFormatter.ofPattern("EEE d MMM");

    private final String     apiKey;
    private final HttpClient httpClient;

    /**
     * Constructs a new WeatherService using the supplied API key.
     *
     * @param apiKey a valid OpenWeatherMap API key; cannot be null or empty
     */
    public WeatherService(String apiKey)
    {
        this.apiKey     = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Fetches current weather conditions for the specified city.
     *
     * @param city  the city name or "lat,lon" coordinate string
     * @param units "metric" for Celsius/km/h or "imperial" for Fahrenheit/mph
     * @return a WeatherData object populated from the API response
     * @throws Exception if the network call fails, the city is not found,
     *                   or the API key is invalid
     */
    public WeatherData fetchCurrentWeather(String city, String units) throws Exception
    {
        String url = BASE_URL + "/weather?q=" + encode(city)
                + "&appid=" + apiKey
                + "&units=" + units;
        return parseCurrentWeather(fetch(url));
    }

    /**
     * Fetches a 5-day forecast for the specified city, returning one entry
     * per day at the midday (12:00) time slot.
     *
     * @param city  the city name or "lat,lon" coordinate string
     * @param units "metric" or "imperial"
     * @return a list of up to five WeatherData forecast entries
     * @throws Exception if the network call fails or the city is not found
     */
    public List<WeatherData> fetchForecast(String city, String units) throws Exception
    {
        String url = BASE_URL + "/forecast?q=" + encode(city)
                + "&appid=" + apiKey
                + "&units=" + units
                + "&cnt=40";
        return parseForecast(fetch(url));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Executes an HTTP GET request and returns the response body as a string.
     * Translates HTTP error codes into meaningful exceptions so callers can
     * display specific error messages to the user.
     *
     * @param url the fully formed API URL
     * @return the response body JSON string
     * @throws Exception on any network or HTTP error
     */
    private String fetch(String url) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode())
        {
            case 200 -> { return response.body(); }
            case 401 -> throw new Exception("Invalid API key — check your key in WeatherApp.java.");
            case 404 -> throw new Exception("City not found: \"" + city(url) + "\". Check spelling.");
            case 429 -> throw new Exception("API rate limit reached. Wait a moment and try again.");
            default  -> throw new Exception("API returned HTTP " + response.statusCode() + ".");
        }
    }

    /**
     * Parses a current-weather JSON response into a WeatherData object.
     *
     * @param json the raw JSON string from the API
     * @return populated WeatherData
     */
    private WeatherData parseCurrentWeather(String json)
    {
        WeatherData data = new WeatherData();
        data.cityName    = parseStr(json, "name");
        data.country     = parseStr(json, "country");
        data.temperature = parseDbl(json, "temp");
        data.feelsLike   = parseDbl(json, "feels_like");
        data.humidity    = (int) parseDbl(json, "humidity");
        data.pressure    = (int) parseDbl(json, "pressure");
        data.windSpeed   = parseDbl(json, "speed");
        data.visibility  = (int) parseDbl(json, "visibility");
        data.description = parseStr(json, "description");

        // "main" inside the "weather" array is the condition category.
        // "main" as a top-level key is the temperature section — different context.
        int weatherArrayIdx = json.indexOf("\"weather\":");
        if (weatherArrayIdx != -1)
        {
            data.weatherMain = parseStr(json.substring(weatherArrayIdx), "main");
        }
        return data;
    }

    /**
     * Parses a forecast JSON response and returns one WeatherData entry for
     * each day at the 12:00:00 time slot (up to five entries).
     *
     * @param json the raw forecast JSON string
     * @return list of daily forecast entries
     */
    private List<WeatherData> parseForecast(String json)
    {
        List<WeatherData> forecasts = new ArrayList<>();
        String[] items = json.split("\"dt_txt\":");

        for (int i = 1; i < items.length && forecasts.size() < 5; i++)
        {
            try
            {
                String item  = items[i];
                String dtTxt = parseStr(item, "dt_txt");

                // Only use midday entries so each card represents one full day.
                if (!dtTxt.contains("12:00:00"))
                {
                    continue;
                }

                WeatherData fc = new WeatherData();
                fc.temperature  = parseDbl(item, "temp");
                fc.description  = parseStr(item, "description");
                fc.weatherMain  = parseStr(item, "main");

                LocalDateTime dt = LocalDateTime.parse(dtTxt.trim(), DT_IN);
                fc.forecastDay = dt.format(DT_OUT);
                forecasts.add(fc);
            }
            catch (Exception e)
            {
                // Skip malformed or incomplete forecast time slots silently.
            }
        }
        return forecasts;
    }

    /**
     * Extracts a string value for the given key from a JSON fragment.
     * Handles both quoted string values and bare numeric values.
     *
     * @param json the JSON text to search
     * @param key  the key name (without quotes)
     * @return the extracted value as a string, or "" if not found
     */
    private String parseStr(String json, String key)
    {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        idx += pattern.length();

        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx >= json.length()) return "";

        if (json.charAt(idx) == '"')
        {
            int start = idx + 1;
            int end   = json.indexOf('"', start);
            return (end == -1) ? "" : json.substring(start, end);
        }
        else
        {
            int start = idx;
            int end   = start;
            while (end < json.length()
                    && "0123456789.-".indexOf(json.charAt(end)) >= 0) end++;
            return (end > start) ? json.substring(start, end) : "";
        }
    }

    /**
     * Convenience wrapper that calls parseStr and converts the result to
     * a double, returning 0.0 on any parse failure.
     *
     * @param json the JSON text
     * @param key  the numeric field key
     * @return the parsed double value, or 0.0 if absent or malformed
     */
    private double parseDbl(String json, String key)
    {
        try { return Double.parseDouble(parseStr(json, key)); }
        catch (Exception e) { return 0.0; }
    }

    /** URL-encodes spaces in a city name for use in the query string. */
    private String encode(String s) { return s.replace(" ", "%20"); }

    /** Extracts the city name from a query URL for use in error messages. */
    private String city(String url)
    {
        int q = url.indexOf("q=");
        int a = url.indexOf("&", q);
        return (q == -1) ? "" : url.substring(q + 2, (a == -1) ? url.length() : a)
                .replace("%20", " ");
    }
}