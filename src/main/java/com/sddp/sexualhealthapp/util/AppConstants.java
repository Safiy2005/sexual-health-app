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
     * Key used to store when the secret equation was created.
     */
    public static final String SECRET_CREATION_DATE_KEY = "secret_creation_date";

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
     * Delay in milliseconds before transitioning to main app after successful authentication.
     * This prevents the transition from looking too obvious.
     */
    public static final int TRANSITION_DELAY_MS = 500;

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

    // CSS Resource Paths
    /**
     * Path to the application stylesheet.
     */
    public static final String CSS_STYLESHEET = "/css/styles.css";

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
     * Number of onboarding pages in the setup carousel (before the calculator).
     */
    public static final int ONBOARDING_PAGE_COUNT = 4;

    /**
     * Duration in milliseconds for onboarding slide animations.
     */
    public static final int ONBOARDING_SLIDE_DURATION_MS = 150;

    // Validation Constants
    /**
     * List of trivial equations that should be rejected during setup.
     * These are considered too easy to guess.
     */
    public static final String[] TRIVIAL_EQUATIONS = {
        "1+1=2",
        "0+0=0",
        "1-1=0",
        "0-0=0",
        "1*1=1",
        "0*0=0",
        "1/1=1",
        "2+2=4"
    };

    /**
     * Maximum number of authentication attempts to track (for future rate limiting).
     */
    public static final int MAX_AUTH_ATTEMPTS = 5;

    // Error Messages
    /**
     * Error message displayed when equation validation fails due to incorrect math.
     */
    public static final String ERROR_INVALID_MATH = "The equation doesn't match! Please check your math.";

    /**
     * Error message displayed when equation is too trivial.
     */
    public static final String ERROR_TRIVIAL_EQUATION = "Please choose a more complex equation for better security.";

    /**
     * Error message displayed when required fields are empty.
     */
    public static final String ERROR_EMPTY_FIELDS = "Please fill in all fields.";

    /**
     * Error message displayed when input is not a valid number.
     */
    public static final String ERROR_INVALID_NUMBER = "Please enter valid numbers.";

    // Calculator Display Messages
    /**
     * Error message displayed on calculator when dividing by zero.
     */
    public static final String CALC_ERROR_DIV_ZERO = "Error";

    /**
     * Initial display value when calculator starts.
     */
    public static final String CALC_INITIAL_DISPLAY = "0";
}
