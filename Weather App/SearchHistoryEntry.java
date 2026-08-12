import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SearchHistoryEntry records one weather lookup â€” the city name and the
 * exact local date-time at which the search was performed. The toString()
 * representation is used directly in the history JList.
 *
 * @author Lerato Maseko
 */
public class SearchHistoryEntry
{
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");

    /** The city name as the user typed it. */
    public final String cityName;

    /** The moment at which this search was executed. */
    public final LocalDateTime timestamp;

    /**
     * Constructs a new SearchHistoryEntry.
     *
     * @param cityName  the city name that was searched
     * @param timestamp the local date-time of the search
     */
    public SearchHistoryEntry(String cityName, LocalDateTime timestamp)
    {
        this.cityName  = cityName;
        this.timestamp = timestamp;
    }

    /**
     * Returns a display string suitable for the history JList, e.g.
     * "Cape Town  14:32 04/08".
     *
     * @return formatted history entry
     */
    @Override
    public String toString()
    {
        return cityName + "  " + timestamp.format(DISPLAY_FORMAT);
    }
}