import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

/**
 * WeatherApp is the main application window of the Weather Information App.
 * It provides a Swing-based GUI that lets users search any city, view current
 * temperature, humidity, wind speed, and conditions, browse a 5-day forecast,
 * toggle between Celsius and Fahrenheit, track search history, and see a
 * dynamic background that changes with the time of day.
 *
 * <p>To run: set YOUR_API_KEY_HERE to a valid OpenWeatherMap API key
 * (free registration at https://openweathermap.org/api), then compile and
 * run as described in README.md.
 *
 * @author Lerato Maseko
 */
public class WeatherApp extends JFrame
{
    // ── Replace with your own free OpenWeatherMap API key ──────────────────
    private static final String API_KEY = "YOUR_API_KEY_HERE";

    // ── UI component references needed for updates ─────────────────────────
    private JTextField         cityField;
    private JButton            searchButton;
    private JComboBox<String>  unitSelector;
    private JLabel             weatherIconLabel;
    private JLabel             temperatureLabel;
    private JLabel             cityCountryLabel;
    private JLabel             conditionLabel;
    private JLabel             humidityLabel;
    private JLabel             windLabel;
    private JLabel             feelsLikeLabel;
    private JLabel             pressureLabel;
    private JLabel             visibilityLabel;
    private JPanel             forecastPanel;
    private JPanel             mainPanel;
    private JLabel             statusLabel;
    private JLabel             clockLabel;
    private DefaultListModel<String> historyModel;

    // ── Application state ──────────────────────────────────────────────────
    private final WeatherService         weatherService;
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();
    private String currentUnit = "metric";  // "metric" or "imperial"

    /**
     * Constructs the application window, initialises all panels and starts
     * the live clock timer.
     */
    public WeatherApp()
    {
        super("Weather Information App");
        weatherService = new WeatherService(API_KEY);
        buildUI();
        startClock();
        applyDynamicBackground();
    }

    // ── UI construction ────────────────────────────────────────────────────

    /**
     * Assembles the top-level window layout and registers the main panel
     * as the content pane.
     */
    private void buildUI()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 720);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(mainPanel);

        mainPanel.add(buildHeader(),  BorderLayout.NORTH);
        mainPanel.add(buildWeather(), BorderLayout.CENTER);
        mainPanel.add(buildHistory(), BorderLayout.EAST);
        mainPanel.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Builds the header panel containing the app title, city search field,
     * search button, unit selector, and live clock.
     *
     * @return the constructed header JPanel
     */
    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(8, 6));
        header.setOpaque(false);

        JLabel title = new JLabel("🌤  Weather Information App", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        inputRow.setOpaque(false);

        cityField = new JTextField(22);
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cityField.setToolTipText("Enter a city name (e.g. Cape Town) or lat,lon");
        cityField.addActionListener(e -> triggerSearch());

        searchButton = new JButton("Search");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        searchButton.setBackground(new Color(52, 152, 219));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> triggerSearch());

        unitSelector = new JComboBox<>(new String[]{"°C  /  km/h", "°F  /  mph"});
        unitSelector.setFont(new Font("SansSerif", Font.PLAIN, 13));
        unitSelector.setToolTipText("Toggle temperature and wind speed units");
        unitSelector.addActionListener(e ->
        {
            currentUnit = (unitSelector.getSelectedIndex() == 0) ? "metric" : "imperial";
            if (!cityField.getText().isBlank()) triggerSearch();
        });

        clockLabel = new JLabel();
        clockLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        clockLabel.setForeground(new Color(220, 230, 255));

        inputRow.add(new JLabel("📍") {{ setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18)); }});
        inputRow.add(cityField);
        inputRow.add(searchButton);
        inputRow.add(unitSelector);
        inputRow.add(clockLabel);

        header.add(inputRow, BorderLayout.CENTER);
        return header;
    }

    /**
     * Builds the central weather display panel containing the current conditions
     * card and the 5-day forecast strip.
     *
     * @return the constructed weather JPanel
     */
    private JPanel buildWeather()
    {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setOpaque(false);

        // ── Current conditions card ────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 100), 1, true),
                new EmptyBorder(20, 24, 20, 24)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 12, 6, 12);

        // Large weather emoji icon
        weatherIconLabel = new JLabel("🌍", SwingConstants.CENTER);
        weatherIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        g.gridx = 0; g.gridy = 0; g.gridheight = 3; g.anchor = GridBagConstraints.CENTER;
        card.add(weatherIconLabel, g);

        g.gridheight = 1; g.gridx = 1;

        // Temperature (prominent)
        temperatureLabel = new JLabel("--°", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("SansSerif", Font.BOLD, 72));
        temperatureLabel.setForeground(Color.WHITE);
        g.gridy = 0;
        card.add(temperatureLabel, g);

        // City + country
        cityCountryLabel = new JLabel("Enter a city to get started", SwingConstants.CENTER);
        cityCountryLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        cityCountryLabel.setForeground(new Color(210, 225, 255));
        g.gridy = 1;
        card.add(cityCountryLabel, g);

        // Condition description
        conditionLabel = new JLabel("", SwingConstants.CENTER);
        conditionLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
        conditionLabel.setForeground(new Color(190, 215, 255));
        g.gridy = 2;
        card.add(conditionLabel, g);

        // Details grid: 5 metric tiles
        JPanel details = new JPanel(new GridLayout(1, 5, 12, 0));
        details.setOpaque(false);
        details.setBorder(new EmptyBorder(14, 0, 0, 0));

        humidityLabel  = makeTile("💧 Humidity",  "--");
        windLabel      = makeTile("💨 Wind",       "--");
        feelsLikeLabel = makeTile("🌡 Feels Like", "--");
        pressureLabel  = makeTile("📊 Pressure",  "--");
        visibilityLabel= makeTile("👁 Visibility", "--");

        details.add(humidityLabel);
        details.add(windLabel);
        details.add(feelsLikeLabel);
        details.add(pressureLabel);
        details.add(visibilityLabel);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        card.add(details, g);

        panel.add(card, BorderLayout.CENTER);

        // ── Forecast strip ─────────────────────────────────────────────────
        JPanel forecastWrapper = new JPanel(new BorderLayout(0, 6));
        forecastWrapper.setOpaque(false);

        JLabel forecastTitle = new JLabel("5-Day Forecast", SwingConstants.CENTER);
        forecastTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        forecastTitle.setForeground(Color.WHITE);
        forecastWrapper.add(forecastTitle, BorderLayout.NORTH);

        forecastPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        forecastPanel.setOpaque(false);
        forecastWrapper.add(forecastPanel, BorderLayout.CENTER);

        panel.add(forecastWrapper, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Creates a styled label tile for displaying one weather metric.
     *
     * @param title the metric name including an emoji prefix
     * @param value the initial value string
     * @return a JLabel formatted as a bordered metric tile
     */
    private JLabel makeTile(String title, String value)
    {
        JLabel tile = new JLabel(
                "<html><center><b>" + title + "</b><br>" + value + "</center></html>",
                SwingConstants.CENTER);
        tile.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tile.setForeground(Color.WHITE);
        tile.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 80), 1, true),
                new EmptyBorder(10, 6, 10, 6)));
        return tile;
    }

    /**
     * Builds the search history panel shown on the right side of the window.
     * Double-clicking a history entry re-runs that search.
     *
     * @return the constructed history JPanel
     */
    private JPanel buildHistory()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(195, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 70)),
                new EmptyBorder(0, 12, 0, 0)));

        JLabel title = new JLabel("Search History");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        historyModel = new DefaultListModel<>();
        JList<String> histList = new JList<>(historyModel);
        histList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        histList.setForeground(Color.WHITE);
        histList.setBackground(new Color(0, 0, 0, 60));
        histList.setOpaque(true);
        histList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        histList.setToolTipText("Double-click to re-run a search");

        // Double-click on a history entry re-searches that city
        histList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2 && histList.getSelectedIndex() != -1)
                {
                    String entry = histList.getSelectedValue();
                    // Entry format: "CityName  HH:mm dd/MM" — extract city
                    String city = entry.split("  ")[0].trim();
                    cityField.setText(city);
                    triggerSearch();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(histList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        clearBtn.addActionListener(e -> { historyModel.clear(); searchHistory.clear(); });
        panel.add(clearBtn, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the status bar shown at the bottom of the window.
     *
     * @return the constructed status JPanel
     */
    private JPanel buildStatusBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bar.setOpaque(false);
        statusLabel = new JLabel("Ready — type a city name and press Search or Enter");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 220, 255));
        bar.add(statusLabel);
        return bar;
    }

    // ── Search and update logic ───────────────────────────────────────────

    /**
     * Validates the city field and launches a background search. Uses
     * SwingWorker so the network call never blocks the Event Dispatch Thread.
     */
    private void triggerSearch()
    {
        String city = cityField.getText().trim();
        if (city.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                    "Please enter a city name.", "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        statusLabel.setText("Fetching weather data for \"" + city + "\"...");

        SwingWorker<WeatherData, Void> worker = new SwingWorker<>()
        {
            @Override
            protected WeatherData doInBackground() throws Exception
            {
                return weatherService.fetchCurrentWeather(city, currentUnit);
            }

            @Override
            protected void done()
            {
                try
                {
                    updateCurrentDisplay(get());
                    addHistory(city);
                    statusLabel.setText("Last updated: "
                            + LocalDateTime.now().format(
                              DateTimeFormatter.ofPattern("HH:mm:ss")));
                    fetchForecast(city);
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(WeatherApp.this,
                            ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error: " + ex.getMessage());
                }
                finally
                {
                    searchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * Launches a background SwingWorker to fetch and display the 5-day
     * forecast. Forecast failures are non-critical and produce no dialog.
     *
     * @param city the city name to fetch the forecast for
     */
    private void fetchForecast(String city)
    {
        SwingWorker<List<WeatherData>, Void> worker = new SwingWorker<>()
        {
            @Override
            protected List<WeatherData> doInBackground() throws Exception
            {
                return weatherService.fetchForecast(city, currentUnit);
            }

            @Override
            protected void done()
            {
                try { updateForecastDisplay(get()); }
                catch (Exception ignored) { /* forecast failure is non-critical */ }
            }
        };
        worker.execute();
    }

    /**
     * Populates all current-conditions labels with data from the WeatherData object.
     *
     * @param d the current weather data returned by the API
     */
    private void updateCurrentDisplay(WeatherData d)
    {
        boolean metric = currentUnit.equals("metric");
        String tUnit = metric ? "°C" : "°F";
        String sUnit = metric ? " km/h" : " mph";

        weatherIconLabel.setText(emoji(d.weatherMain));
        temperatureLabel.setText(String.format("%.0f°", d.temperature));
        cityCountryLabel.setText(d.cityName + ",  " + d.country);
        conditionLabel.setText(capitalise(d.description));

        humidityLabel.setText(tile("💧 Humidity", d.humidity + "%"));
        windLabel.setText(tile("💨 Wind", String.format("%.1f%s", d.windSpeed, sUnit)));
        feelsLikeLabel.setText(tile("🌡 Feels Like",
                String.format("%.0f%s", d.feelsLike, tUnit)));
        pressureLabel.setText(tile("📊 Pressure", d.pressure + " hPa"));
        visibilityLabel.setText(tile("👁 Visibility",
                (d.visibility >= 1000)
                ? String.format("%.1f km", d.visibility / 1000.0)
                : d.visibility + " m"));

        applyDynamicBackground();
    }

    /**
     * Rebuilds the forecast strip with one card per forecast entry.
     *
     * @param forecasts the list of daily forecast WeatherData objects
     */
    private void updateForecastDisplay(List<WeatherData> forecasts)
    {
        forecastPanel.removeAll();
        String tUnit = currentUnit.equals("metric") ? "°C" : "°F";

        for (WeatherData fc : forecasts)
        {
            JPanel card = new JPanel(new GridLayout(3, 1, 2, 2));
            card.setOpaque(false);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(255, 255, 255, 80), 1, true),
                    new EmptyBorder(8, 5, 8, 5)));

            JLabel day = new JLabel(fc.forecastDay, SwingConstants.CENTER);
            day.setFont(new Font("SansSerif", Font.BOLD, 11));
            day.setForeground(Color.WHITE);

            JLabel icon = new JLabel(emoji(fc.weatherMain), SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

            JLabel temp = new JLabel(
                    String.format("%.0f%s", fc.temperature, tUnit), SwingConstants.CENTER);
            temp.setFont(new Font("SansSerif", Font.PLAIN, 13));
            temp.setForeground(Color.WHITE);

            card.add(day); card.add(icon); card.add(temp);
            forecastPanel.add(card);
        }
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Builds an HTML tile string for a metric label. */
    private String tile(String label, String value)
    {
        return "<html><center><b>" + label + "</b><br>" + value + "</center></html>";
    }

    /** Maps an OpenWeatherMap weather category to a Unicode emoji. */
    private String emoji(String main)
    {
        if (main == null || main.isBlank()) return "🌍";
        return switch (main.toLowerCase())
        {
            case "clear"                               -> "☀️";
            case "clouds"                              -> "☁️";
            case "rain", "drizzle"                     -> "🌧️";
            case "thunderstorm"                        -> "⛈️";
            case "snow"                                -> "❄️";
            case "mist", "fog", "haze", "smoke",
                 "dust", "sand", "ash", "squall"       -> "🌫️";
            case "tornado"                             -> "🌪️";
            default                                    -> "🌤️";
        };
    }

    /** Capitalises the first letter of a string. */
    private String capitalise(String s)
    {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Adds a city search to the history list. Keeps at most 10 entries and
     * moves a repeated city to the top rather than duplicating it.
     *
     * @param city the city name that was searched
     */
    private void addHistory(String city)
    {
        // Remove duplicate if present
        for (int i = 0; i < historyModel.size(); i++)
        {
            if (historyModel.get(i).startsWith(city + "  "))
            {
                historyModel.remove(i);
                break;
            }
        }

        String entry = new SearchHistoryEntry(city, LocalDateTime.now()).toString();
        historyModel.add(0, entry);
        searchHistory.add(0, new SearchHistoryEntry(city, LocalDateTime.now()));

        // Cap history at 10
        while (historyModel.size() > 10) historyModel.remove(historyModel.size() - 1);
        while (searchHistory.size() > 10) searchHistory.remove(searchHistory.size() - 1);
    }

    /**
     * Sets the main panel background colour based on the current hour:
     * dawn (05–07), day (08–16), sunset (17–19), night (20–04).
     */
    private void applyDynamicBackground()
    {
        int hour = LocalDateTime.now().getHour();
        Color bg;
        if      (hour >= 5  && hour < 8)  bg = new Color(220, 110, 55);   // dawn
        else if (hour >= 8  && hour < 17) bg = new Color(25,  85,  175);  // day
        else if (hour >= 17 && hour < 20) bg = new Color(190, 70,  50);   // sunset
        else                               bg = new Color(10,  15,  55);   // night

        mainPanel.setBackground(bg);
        getContentPane().setBackground(bg);
    }

    /**
     * Starts a 1-second Swing Timer that updates the live clock label and
     * re-applies the dynamic background colour.
     */
    private void startClock()
    {
        new Timer(1000, e ->
        {
            clockLabel.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            applyDynamicBackground();
        }).start();
    }

    /**
     * Application entry point. Sets the system look and feel, then opens
     * the WeatherApp window on the Event Dispatch Thread.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args)
    {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { /* use Metal L&F as fallback */ }

        SwingUtilities.invokeLater(() -> new WeatherApp().setVisible(true));
    }
}