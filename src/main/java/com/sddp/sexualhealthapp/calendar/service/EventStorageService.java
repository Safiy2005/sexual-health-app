package com.sddp.sexualhealthapp.calendar.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import com.sddp.sexualhealthapp.calendar.model.EventType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for persisting and retrieving calendar events using JSON file
 * storage.
 * Events are stored in {@code src/main/resources/calendarevents/events.json}
 * relative to the project root, bundled with the application resources.
 *
 * <p>
 * This service is the shared data layer for stories 47 (calendar view),
 * 48 (event feed), 22 (event creation), 40 (medication reminders),
 * and 49 (event detail pages).
 * </p>
 */
public class EventStorageService {

    private static final String DATA_DIR = "src/main/resources/calendarevents";
    private static final String EVENTS_FILE = "events.json";

    private final Path storageFilePath;
    private final Gson gson;
    private List<CalendarEvent> events;

    /**
     * Production constructor. Reads/writes events from
     * {@code src/main/resources/calendarevents/events.json}.
     */
    public EventStorageService() {
        this(Paths.get(DATA_DIR, EVENTS_FILE));
    }

    /**
     * Testing constructor. Accepts a custom file path for isolated testing.
     *
     * @param storageFilePath the path to the JSON events file
     */
    public EventStorageService(Path storageFilePath) {
        this.storageFilePath = storageFilePath;
        this.gson = createGson();
        this.events = loadFromFile();
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, type, ctx) -> LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, type, ctx) -> LocalTime.parse(json.getAsString()))
                .registerTypeAdapter(DayOfWeek.class,
                        (JsonSerializer<DayOfWeek>) (src, type, ctx) -> new JsonPrimitive(src.name()))
                .registerTypeAdapter(DayOfWeek.class,
                        (JsonDeserializer<DayOfWeek>) (json, type, ctx) -> DayOfWeek.valueOf(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    /**
     * Returns an unmodifiable list of all events.
     */
    public List<CalendarEvent> getAllEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns events for a specific date, sorted by time (nulls last).
     * Includes both one-off events on this date and recurring events
     * whose recurrence pattern matches this date.
     */
    public List<CalendarEvent> getEventsForDate(LocalDate date) {
        return events.stream()
                .filter(e -> e.occursOn(date))
                .sorted(Comparator.comparing(
                        CalendarEvent::getTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all events that have at least one occurrence in the given month.
     * For recurring events, the event is included if any day in the month
     * matches the recurrence pattern.
     */
    public List<CalendarEvent> getEventsForMonth(YearMonth yearMonth) {
        Set<Integer> activeDays = getDaysWithEvents(yearMonth);
        if (activeDays.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .filter(e -> {
                    for (int day : activeDays) {
                        if (e.occursOn(yearMonth.atDay(day)))
                            return true;
                    }
                    return false;
                })
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns the set of day-of-month values that have events in the
     * given month. Delegates to {@link #getEventTypesPerDay(YearMonth)}.
     */
    public Set<Integer> getDaysWithEvents(YearMonth yearMonth) {
        return getEventTypesPerDay(yearMonth).keySet();
    }

    /**
     * Returns a map from day-of-month to the set of {@link EventType}s
     * present on that day. Used by the calendar grid to render coloured
     * per-type indicator dots beneath each day number.
     */
    public Map<Integer, Set<EventType>> getEventTypesPerDay(YearMonth yearMonth) {
        int daysInMonth = yearMonth.lengthOfMonth();
        Map<Integer, Set<EventType>> result = new HashMap<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            for (CalendarEvent event : events) {
                if (event.occursOn(date)) {
                    result.computeIfAbsent(day, k -> EnumSet.noneOf(EventType.class))
                            .add(event.getType());
                }
            }
        }

        return result;
    }

    /**
     * Expands all events (including recurring) into concrete
     * {@link EventOccurrence}s within the date window
     * [{@code from}, {@code until}] (both inclusive), sorted
     * chronologically by occurrence date then time.
     *
     * <p>
     * Because recurring events can repeat indefinitely, callers
     * should request bounded windows and paginate as needed (the event
     * feed loads successive windows on scroll).
     * </p>
     *
     * @param from  the inclusive start date of the window
     * @param until the inclusive end date of the window
     * @return a chronologically sorted list of occurrences in the window
     */
    public List<EventOccurrence> getUpcomingOccurrences(LocalDate from, LocalDate until) {
        List<EventOccurrence> occurrences = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(until); date = date.plusDays(1)) {
            for (CalendarEvent event : events) {
                if (event.occursOn(date)) {
                    occurrences.add(new EventOccurrence(event, date));
                }
            }
        }

        occurrences.sort(Comparator
                .comparing(EventOccurrence::occurrenceDate)
                .thenComparing(o -> o.event().getTime(),
                        Comparator.nullsLast(Comparator.naturalOrder())));

        return occurrences;
    }

    /**
     * Finds an event by its ID.
     */
    public Optional<CalendarEvent> getEventById(String id) {
        return events.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    /**
     * Adds a new event and persists to file.
     */
    public boolean addEvent(CalendarEvent event) {
        if (event == null)
            return false;
        events.add(event);
        return saveToFile();
    }

    /**
     * Updates an existing event (matched by ID) and persists.
     */
    public boolean updateEvent(CalendarEvent updated) {
        if (updated == null)
            return false;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(updated.getId())) {
                events.set(i, updated);
                return saveToFile();
            }
        }
        return false;
    }

    /**
     * Deletes an event by ID and persists.
     */
    public boolean deleteEvent(String id) {
        boolean removed = events.removeIf(e -> e.getId().equals(id));
        if (removed) {
            return saveToFile();
        }
        return false;
    }

    private List<CalendarEvent> loadFromFile() {
        if (!Files.exists(storageFilePath)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(storageFilePath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<CalendarEvent>>() {
            }.getType();
            List<CalendarEvent> loaded = gson.fromJson(reader, listType);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Failed to load events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean saveToFile() {
        try {
            Files.createDirectories(storageFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storageFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(events, writer);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save events: " + e.getMessage());
            return false;
        }
    }
}
