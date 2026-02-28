package com.sddp.sexualhealthapp.calendar.controller;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Unit tests for EventDetailController.
 *
 * - Defect tests: null event => missing state, null type => fallback, trimming missing ID
 * - Boundary/partition tests: dosage visible rules, recurrence visible rules, missing ID blank vs present
 * - Regression tests: style string contains dot color, view state toggles correctly, callbacks work correctly
**/

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class EventDetailControllerTest {

    private EventDetailController controller;

    @BeforeAll
    static void initJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFx Platfrom failed to start");
            }
        } catch (IllegalStateException alreadyStarted) {
            // JavaFx already started
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        controller = new EventDetailController();

        // Inject @FXML fields 
        inject(controller, "nameLabel", new Label());
        inject(controller, "typeBadge", new Label());
        inject(controller, "dateTimeLabel", new Label());

        inject(controller, "contentRoot", new VBox());
        inject(controller, "missingStateRoot", new VBox());
        inject(controller, "missingStateBodyLabel", new Label());
        inject(controller, "missingEventIdLabel", new Label());

        inject(controller, "descriptionLabel", new Label());

        inject(controller, "dosageBox", new VBox());
        inject(controller, "dosageLabel", new Label());

        inject(controller, "recurrenceBox", new VBox());
        inject(controller, "recurrenceLabel", new Label());

        Locale.setDefault(Locale.UK);
    }

    // Defect tests

    @Test
    void defect_nullEvent_showsMissingStateAndHidesContent() throws Exception {
        runOnFxAndWait(() -> controller.setEvent(null, null));

        VBox contentRoot = get(controller, "contentRoot", VBox.class);
        VBox missingRoot = get(controller, "missingStateRoot", VBox.class);
        Label missingBody = get(controller, "missingStateBodyLabel", Label.class);

        assertFalse(contentRoot.isVisible() || contentRoot.isManaged());
        assertTrue(missingRoot.isVisible() && missingRoot.isManaged());
        assertFalse(missingBody.getText().isBlank(), "Missing body text should be set");
    }

    @Test
    void defect_nullType_fallsBackToGenericStyleAndText() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "Untyped Event",
                LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0),
                null,
                null,
                null
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        Label typeBadge = get(controller, "typeBadge", Label.class);
        assertEquals("Event", typeBadge.getText(), "Null type should fall back to 'Event'");
        assertNotNull(typeBadge.getStyle());
        assertFalse(typeBadge.getStyle().isBlank(), "Fallback style should still be applied");
    }

    // Boundary / partition tests
    
    @Test
    void boundary_missingEventId_blankOrWhitespace_hidesIdLabel() throws Exception {
        runOnFxAndWait(() -> controller.showMissingEventState("   "));

        Label idLabel = get(controller, "missingEventIdLabel", Label.class);
        assertFalse(idLabel.isVisible() || idLabel.isManaged(),
                "Whitespace ID should be treated as null and hidden");
        assertEquals("", idLabel.getText());
    }

    @Test
    void boundary_missingEventId_present_showsIdLabelWithPrefix() throws Exception {
        runOnFxAndWait(() -> controller.showMissingEventState("abc-123"));

        Label idLabel = get(controller, "missingEventIdLabel", Label.class);
        assertTrue(idLabel.isVisible() && idLabel.isManaged());
        assertTrue(idLabel.getText().contains("Event ID: abc-123"));
    }

    @Test
    void partition_medicationWithDosage_showsDosageBox() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "PrEP Dose",
                LocalDate.of(2026, 3, 15),
                LocalTime.of(8, 0),
                EventType.MEDICATION,
                null,
                "200mg"
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        VBox dosageBox = get(controller, "dosageBox", VBox.class);
        Label dosageLabel = get(controller, "dosageLabel", Label.class);

        assertTrue(dosageBox.isVisible() && dosageBox.isManaged());
        assertFalse(dosageLabel.getText().isBlank());
        assertTrue(dosageLabel.getText().toLowerCase().contains("200"));
    }

    @Test
    void partition_nonMedication_hidesDosageBoxEvenIfDosageProvided() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "Consultation",
                LocalDate.of(2026, 3, 20),
                LocalTime.of(9, 0),
                EventType.APPOINTMENT,
                null,
                "200mg"
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        VBox dosageBox = get(controller, "dosageBox", VBox.class);
        Label dosageLabel = get(controller, "dosageLabel", Label.class);

        assertFalse(dosageBox.isVisible() || dosageBox.isManaged());
        assertEquals("", dosageLabel.getText(), "Dosage label should be cleared when not shown");
    }

    @Test
    void partition_recurringEvent_showsRecurrenceBox() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "Weekly Counselling",
                LocalDate.of(2026, 3, 2),
                LocalTime.of(16, 0),
                EventType.APPOINTMENT,
                null,
                null
        );
        event.setRecurrenceRule(RecurrenceRule.weekly(1, DayOfWeek.MONDAY));

        runOnFxAndWait(() -> controller.setEvent(event, null));

        VBox recurrenceBox = get(controller, "recurrenceBox", VBox.class);
        Label recurrenceLabel = get(controller, "recurrenceLabel", Label.class);

        assertTrue(recurrenceBox.isVisible() && recurrenceBox.isManaged());
        assertFalse(recurrenceLabel.getText().isBlank());
    }

    @Test
    void partition_nonRecurringEvent_hidesRecurrenceBox() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "One-off Test",
                LocalDate.of(2026, 4, 1),
                LocalTime.of(14, 0),
                EventType.TEST,
                null,
                null
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        VBox recurrenceBox = get(controller, "recurrenceBox", VBox.class);
        Label recurrenceLabel = get(controller, "recurrenceLabel", Label.class);

        assertFalse(recurrenceBox.isVisible() || recurrenceBox.isManaged());
        assertEquals("", recurrenceLabel.getText());
    }

    // Regression tests
    @Test
    void regression_typeStyleUsesDotColorForKnownType() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "Styled",
                LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0),
                EventType.TEST,
                null,
                null
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        Label typeBadge = get(controller, "typeBadge", Label.class);
        assertTrue(typeBadge.getStyle().contains(EventType.TEST.getDotColor()),
                "Type badge style should include dot color (UI regression guard)");
    }

    @Test
    void regression_stateTogglesBetweenContentAndMissing() throws Exception {
        CalendarEvent event = new CalendarEvent(
                "Event",
                LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0),
                EventType.APPOINTMENT,
                null,
                null
        );

        runOnFxAndWait(() -> controller.setEvent(event, null));

        VBox contentRoot = get(controller, "contentRoot", VBox.class);
        VBox missingRoot = get(controller, "missingStateRoot", VBox.class);

        assertTrue(contentRoot.isVisible() && contentRoot.isManaged());
        assertFalse(missingRoot.isVisible() || missingRoot.isManaged());

        runOnFxAndWait(() -> controller.showMissingEventState("x"));

        assertFalse(contentRoot.isVisible() || contentRoot.isManaged());
        assertTrue(missingRoot.isVisible() && missingRoot.isManaged());
    }

    // Wiring tests
    @Test
    void handleBack_invokesCallback() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        controller.setOnBack(() -> called.set(true));

        runOnFxAndWait(() ->
                invokePrivate(controller, "handleBackToCalendar", javafx.event.ActionEvent.class, null)
        );

        assertTrue(called.get(), "Back callback should run when handler is invoked");
    }

    @Test
    void handleEdit_invokesCallbackOnlyWhenEventPresent_partitionTest() throws Exception {
        AtomicBoolean editCalled = new AtomicBoolean(false);
        controller.setOnEdit(ev -> editCalled.set(true));

        // Partition A: missing state => edit should not fire
        runOnFxAndWait(() -> controller.showMissingEventState("x"));
        runOnFxAndWait(() -> invokePrivate(controller, "handleEditEvent", javafx.event.ActionEvent.class, null));
        assertFalse(editCalled.get());

        // Partition B: event set => edit should fire
        CalendarEvent event = new CalendarEvent(
                "Editable",
                LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0),
                EventType.APPOINTMENT,
                null,
                null
        );
        runOnFxAndWait(() -> controller.setEvent(event, null));
        runOnFxAndWait(() -> invokePrivate(controller, "handleEditEvent", javafx.event.ActionEvent.class, null));
        assertTrue(editCalled.get());
    }
    // Helpers
    
    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static <T> T get(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    private static void invokePrivate(Object target, String method, Class<?> paramType, Object arg) {
        try {
            var m = target.getClass().getDeclaredMethod(method, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void runOnFxAndWait(Runnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for FX thread");
    }
}