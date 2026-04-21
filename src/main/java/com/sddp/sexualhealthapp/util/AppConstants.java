package com.sddp.sexualhealthapp.util;

/**
 * Application-wide constants for the Sexual Health App.
 * This class contains all constant values used throughout the application
 * to ensure consistency and ease of maintenance.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public final class AppConstants {

        /**
         * Private constructor to prevent instantiation of this utility class.
         */
        private AppConstants() {
                throw new AssertionError("Cannot instantiate AppConstants class");
        }

        // Application Window Properties
        /**
         * The application window title (disguised as a calculator).
         */
        public static final String APP_TITLE = "Calculator";

        /**
         * Fixed width of the app window in pixels (9:16 mobile aspect ratio).
         */
        public static final int APP_WIDTH = 360;

        /**
         * Fixed height of the app window in pixels (9:16 mobile aspect ratio).
         */
        public static final int APP_HEIGHT = 640;

        // Secure Storage Keys
        /**
         * Key used to store the hashed secret equation in Java Preferences.
         */
        public static final String SECRET_EQUATION_KEY = "secret_equation_hash";

        /**
         * Key used to store the hashed parental controls PIN in Java Preferences.
         */
        public static final String PARENTAL_CONTROLS_PIN_KEY = "parental_controls_pin_hash";

        // Calculator Configuration
        /**
         * Maximum number of equations to keep in history for secret matching.
         */
        public static final int MAX_EQUATION_HISTORY = 10;

        /**
         * Maximum number of digits allowed in calculator display.
         */
        public static final int MAX_DISPLAY_DIGITS = 15;

        /**
         * Delay in milliseconds before transitioning to main app after successful
         * authentication.
         * This prevents the transition from looking too obvious.
         */
        public static final int TRANSITION_DELAY_MS = 120;

        /**
         * Delay before warming the main app root while the calculator is idle.
         */
        public static final int MAIN_APP_PRELOAD_DELAY_MS = 250;

        // FXML Resource Paths
        /**
         * Path to the calculator FXML layout file.
         */
        public static final String CALCULATOR_FXML = "/fxml/calculator.fxml";

        /**
         * Path to the setup wizard FXML layout file.
         */
        public static final String SETUP_FXML = "/fxml/setup.fxml";

        /**
         * Path to the main app FXML layout file.
         */
        public static final String MAIN_APP_FXML = "/fxml/main-app.fxml";

        /**
         * Path to the article reader layout file.
         */
        public static final String ARTICLE_VIEW_FXML = "/fxml/article-view.fxml";

        /**
         * Path to the calendar tab layout file.
         */
        public static final String CALENDAR_VIEW_FXML = "/fxml/calendar-view.fxml";

        /**
         * Path to the event feed layout file.
         */
        public static final String EVENT_FEED_FXML = "/fxml/event-feed-view.fxml";

        /**
         * Path to the create-event layout file.
         */
        public static final String CREATE_EVENT_FXML = "/fxml/create-event-view.fxml";

        /**
         * Path to the event detail layout file.
         */
        public static final String EVENT_DETAIL_FXML = "/fxml/event-detail-view.fxml";

        /**
         * Path to the settings layout file.
         */
        public static final String SETTINGS_VIEW_FXML = "/fxml/settings-view.fxml";

        // CSS Resource Paths
        /**
         * Calculator-specific styles (also used by setup screen).
         */
        public static final String CSS_CALCULATOR = "/css/calculator.css";

        /**
         * Setup & onboarding tutorial styles.
         */
        public static final String CSS_SETUP = "/css/setup.css";

        /**
         * Main app shell & bottom navigation styles.
         */
        public static final String CSS_MAIN_APP = "/css/main-app.css";

        /**
         * Article search, cards, detail, and navigation menu styles.
         */
        public static final String CSS_ARTICLE = "/css/article.css";

        /**
         * Calendar grid, day cells, and event card styles.
         */
        public static final String CSS_CALENDAR = "/css/calendar.css";

        /**
         * Create-event form inputs, controls, and date picker styles.
         */
        public static final String CSS_CREATE_EVENT = "/css/create-event.css";

        /**
         * Event feed section headers and filter chip styles.
         */
        public static final String CSS_EVENT_FEED = "/css/event-feed.css";

        /**
         * Event detail view and confirmation overlay styles.
         */
        public static final String CSS_EVENT_DETAIL = "/css/event-detail.css";

        /**
         * Accessibility overrides (dyslexic-font toggle, etc.).
         * Included on every scene so the toggle works globally.
         */
        public static final String CSS_ACCESSIBILITY = "/css/accessibility.css";

        /**
         * Stylesheets applied to the calculator scene.
         * Accessibility CSS is deliberately excluded — the calculator must always
         * render in the default system font to maintain the disguise and avoid
         * text truncation from OpenDyslexic's wider glyphs.
         */
        public static final String[] CSS_CALCULATOR_SCENE = {
                        CSS_CALCULATOR
        };

        /**
         * Stylesheets applied to the setup/onboarding scene.
         * Accessibility CSS is deliberately excluded for the same reasons as
         * the calculator scene above.
         */
        public static final String[] CSS_SETUP_SCENE = {
                        CSS_CALCULATOR, CSS_SETUP
        };

        /**
         * Stylesheets applied to the main application scene.
         */
        public static final String[] CSS_MAIN_APP_SCENE = {
                        CSS_MAIN_APP, CSS_ARTICLE, CSS_CALENDAR,
                        CSS_CREATE_EVENT, CSS_EVENT_FEED, CSS_EVENT_DETAIL,
                        CSS_ACCESSIBILITY
        };

        // Scene Names (for SceneManager)
        /**
         * Scene identifier for the calculator view.
         */
        public static final String SCENE_CALCULATOR = "CALCULATOR";

        /**
         * Scene identifier for the setup wizard view.
         */
        public static final String SCENE_SETUP = "SETUP";

        /**
         * Scene identifier for the main application view.
         */
        public static final String SCENE_MAIN_APP = "MAIN_APP";

        /**
         * Duration in milliseconds for onboarding slide animations.
         */
        public static final int ONBOARDING_SLIDE_DURATION_MS = 150;

        // Scene transition animation durations
        /**
         * Default crossfade duration for setup↔calculator transitions (ms).
         */
        public static final int SCENE_CROSSFADE_MS = 250;

        /**
         * Fast crossfade duration for the lock button transition (ms).
         */
        public static final int LOCK_CROSSFADE_MS = 250;

        /**
         * Slower, elegant crossfade for calculator→main app reveal (ms).
         */
        public static final int REVEAL_FADE_MS = 250;

        /**
         * Slide duration for search↔article view transitions within the main app (ms).
         */
        public static final int VIEW_SLIDE_MS = 250;

        // Error Messages
        /**
         * Error message displayed when equation validation fails due to incorrect math.
         */
        public static final String ERROR_INVALID_MATH = "The equation doesn't match! Please check your math.";

        // Calculator Display Messages
        /**
         * Error message displayed on calculator when dividing by zero.
         */
        public static final String CALC_ERROR_DIV_ZERO = "Error";

        /**
         * Initial display value when calculator starts.
         */
        public static final String CALC_INITIAL_DISPLAY = "0";

        // ARTICLE CONSTANTS
        public static final String ARTICLE_MARKDOWN_PATH = "article-mds";
        public static final String ARTICLE_STATE_DIR = "src/main/resources/article-state";
        public static final String RECENTLY_READ_FILE = "recently-read.json";

        // Calendar FXML path
        /**
         * Path to the calendar view FXML layout file.
         */
        public static final String CALENDAR_FXML = "/fxml/calendar-view.fxml";

        /**
         * Scene identifier for the calendar view.
         */
        public static final String SCENE_CALENDAR = "CALENDAR";
}
