# Weather Information App — README

Author : Lerato Maseko | CS 1103 Programming 2 | Unit 8

A Java Swing weather application that fetches real-time weather and 5-day
forecasts from the OpenWeatherMap API. Features dynamic backgrounds,
unit conversion, search history, and emoji condition icons.

## Files
| File | Description |
|------|-------------|
| WeatherApp.java | Main GUI window + all Swing panels |
| WeatherService.java | OpenWeatherMap API calls + JSON parsing |
| WeatherData.java | Data model for weather readings |
| SearchHistoryEntry.java | History entry with city name + timestamp |

## Requirements
- Java 11 or later (uses java.net.http.HttpClient built into JDK 11+)
- No external libraries required
- A free OpenWeatherMap API key (see step 1 below)

## Setup

### Step 1 — Get a free API key
1. Go to https://openweathermap.org/api
2. Click "Sign Up" and create a free account
3. From your account dashboard, copy your default API key

### Step 2 — Add your API key
Open WeatherApp.java and replace line:
```
private static final String API_KEY = "YOUR_API_KEY_HERE";
```
with your actual key:
```
private static final String API_KEY = "abc123yourkeyhere";
```
Note: New API keys take up to 2 hours to activate after registration.

### Step 3 — Compile
Open a terminal in the folder containing all four .java files and run:
```
javac WeatherApp.java WeatherService.java WeatherData.java SearchHistoryEntry.java
```

### Step 4 — Run
```
java WeatherApp
```

## Features
- **Real-time weather**: temperature, humidity, wind speed, pressure, visibility
- **5-day forecast**: daily midday conditions with emoji icons
- **Unit conversion**: toggle between °C/km/h and °F/mph at any time
- **Search history**: last 10 searches; double-click to re-run
- **Dynamic background**: colour changes with time of day (dawn/day/sunset/night)
- **Error handling**: invalid city, API key errors, and network failures all show clear messages
- **Live clock**: updates every second in the header bar

## Usage
1. Type a city name in the search box (e.g. "Cape Town", "Tokyo", "New York")
2. Press Enter or click Search
3. Toggle the unit selector to switch between Celsius and Fahrenheit
4. Double-click any history entry to re-search that city
5. Click "Clear History" to reset the history list

## Error Messages
| Message | Cause |
|---------|-------|
| City not found | Spelling mistake or unsupported location |
| Invalid API key | Key not entered or not yet activated (wait 2 hrs) |
| API rate limit reached | Too many requests on the free tier |
| Network error | No internet connection |
