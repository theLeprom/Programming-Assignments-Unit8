/**
 * WeatherData is an immutable-style data container holding weather information
 * for a single point in time — either current conditions or one entry from a
 * 5-day forecast. All fields are package-visible for brevity; in a larger
 * system they would be private with getters.
 *
 * @author Lerato Maseko
 */
public class WeatherData
{
    /** City or location name returned by the API. */
    String cityName    = "";

    /** ISO 3166-1 alpha-2 country code (e.g., "ZA", "GB", "US"). */
    String country     = "";

    /** Air temperature in the currently selected unit (°C or °F). */
    double temperature;

    /** "Feels like" temperature accounting for wind chill or humidity. */
    double feelsLike;

    /** Relative humidity as a percentage (0–100). */
    int    humidity;

    /** Atmospheric pressure in hectopascals (hPa). */
    int    pressure;

    /** Wind speed in the unit matching the temperature unit (km/h or mph). */
    double windSpeed;

    /** Primary weather category returned by the API (e.g., "Clear", "Rain"). */
    String weatherMain  = "";

    /** Human-readable weather description (e.g., "overcast clouds"). */
    String description  = "";

    /** Formatted date label used for forecast cards ("Mon 4 Aug"). */
    String forecastDay  = "";

    /** Visibility in metres. */
    int    visibility;
}