package com.sddp.sexualhealthapp.calculator.controller;

import com.sddp.sexualhealthapp.calculator.model.Calculator;
import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * Controller for the combined onboarding tutorial and setup wizard.
 *
 * <p>The setup scene is a single carousel: pages 0–2 are the onboarding
 * tutorial slides, and page 3 is the calculator interface where the user
 * creates their secret equation. All pages slide within one StackPane,
 * so the transition from tutorial to calculator feels seamless.</p>
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SetupController {

    // --- Onboarding carousel pages ---
    @FXML
    private VBox onboardingPage1;

    @FXML
    private VBox onboardingPage2;

    @FXML
    private VBox onboardingPage3;

    // --- Calculator setup page ---
    @FXML
    private VBox setupPage;

    // Calculator UI elements
    @FXML
    private Label displayLabel;

    @FXML
    private Button clearButton;

    @FXML
    private Button backspaceButton;

    @FXML
    private Button equalsButton;

    // Instruction header elements
    @FXML
    private VBox instructionHeader;

    @FXML
    private Label instructionTitle;

    @FXML
    private Label instructionMessage;

    // Confirmation panel elements
    @FXML
    private VBox confirmationOverlay;

    @FXML
    private VBox confirmationPanel;

    @FXML
    private Label equationPreview;

    @FXML
    private Button confirmButton;

    @FXML
    private Button retryButton;

    // --- Onboarding video containers and fallback placeholders ---
    @FXML
    private StackPane videoContainer1;
    @FXML
    private VBox videoPlaceholder1;
    @FXML
    private StackPane videoContainer2;
    @FXML
    private VBox videoPlaceholder2;
    @FXML
    private StackPane videoContainer3;
    @FXML
    private VBox videoPlaceholder3;

    /**
     * Per-page media players, indexed by page number (0-2 for onboarding pages).
     * A slot is null if that page has no video file available.
     */
    private final MediaPlayer[] pageMediaPlayers = new MediaPlayer[3];

    // --- Carousel state ---
    private VBox[] pages;
    private int currentPage;

    // --- Calculator state ---
    private final Calculator calculator;
    private final SecretAuthService authService;
    private String capturedEquation;
    private String capturedResult;
    private SetupState currentState;

    private enum SetupState {
        ENTERING_EQUATION,
        AWAITING_CONFIRMATION,
        CONFIRMED
    }

    public SetupController() {
        this.calculator = new Calculator();
        this.authService = new SecretAuthService();
        this.currentState = SetupState.ENTERING_EQUATION;
    }

    /**
     * Initializes the controller after FXML injection.
     * Positions all carousel pages (onboarding + calculator setup) side by side.
     */
    @FXML
    private void initialize() {
        pages = new VBox[]{onboardingPage1, onboardingPage2, onboardingPage3, setupPage};
        currentPage = 0;

        for (int i = 0; i < pages.length; i++) {
            pages[i].setTranslateX(i * AppConstants.APP_WIDTH);
        }

        loadOnboardingVideos();
        updateDisplay();
        showEnteringState();
    }

    /**
     * Attempts to load looping MP4 videos for each onboarding page.
     * If a video file exists at the expected resource path, a {@link MediaView}
     * is created and inserted into the container, and the placeholder is hidden.
     * Videos are paused by default; only the current page's video is started.
     * When the user navigates between pages, {@link #slideTo(int)} handles
     * stopping the outgoing video and restarting the incoming one from the beginning.
     */
    private void loadOnboardingVideos() {
        String[] videoFiles = {
            "/images/onboarding/calculator-disguise.mp4",
            "/images/onboarding/equation-unlock.mp4",
            "/images/onboarding/reset-code.mp4"
        };
        StackPane[] containers = {videoContainer1, videoContainer2, videoContainer3};
        VBox[] placeholders = {videoPlaceholder1, videoPlaceholder2, videoPlaceholder3};

        for (int i = 0; i < videoFiles.length; i++) {
            try {
                var resource = getClass().getResource(videoFiles[i]);
                if (resource != null) {
                    Media media = new Media(resource.toExternalForm());
                    MediaPlayer player = new MediaPlayer(media);
                    player.setCycleCount(MediaPlayer.INDEFINITE);
                    player.setMute(true);
                    player.setAutoPlay(false);

                    MediaView mediaView = new MediaView(player);
                    mediaView.setFitWidth(312);
                    mediaView.setFitHeight(360);
                    mediaView.setPreserveRatio(true);

                    // Insert video behind the placeholder, then hide placeholder
                    containers[i].getChildren().add(0, mediaView);
                    placeholders[i].setVisible(false);
                    placeholders[i].setManaged(false);

                    pageMediaPlayers[i] = player;
                }
            } catch (Exception e) {
                // Video not available yet — placeholder stays visible
            }
        }

        // Start only the first page's video
        if (pageMediaPlayers[0] != null) {
            pageMediaPlayers[0].seek(Duration.ZERO);
            pageMediaPlayers[0].play();
        }
    }

    /**
     * Stops and disposes all active media players.
     * Should be called when the setup scene is torn down.
     */
    public void dispose() {
        for (int i = 0; i < pageMediaPlayers.length; i++) {
            if (pageMediaPlayers[i] != null) {
                pageMediaPlayers[i].stop();
                pageMediaPlayers[i].dispose();
                pageMediaPlayers[i] = null;
            }
        }
    }

    // =========================================================
    // Onboarding carousel handlers
    // =========================================================

    @FXML
    public void handleOnboardingNext() {
        slideTo(currentPage + 1);
    }

    @FXML
    public void handleOnboardingContinue() {
        slideTo(currentPage + 1);
    }

    private void slideTo(int targetPage) {
        if (targetPage < 0 || targetPage >= pages.length || targetPage == currentPage) {
            return;
        }

        // Stop the outgoing page's video (if it has one)
        if (currentPage < pageMediaPlayers.length && pageMediaPlayers[currentPage] != null) {
            pageMediaPlayers[currentPage].stop();
        }

        VBox outgoing = pages[currentPage];
        VBox incoming = pages[targetPage];
        Duration duration = Duration.millis(AppConstants.ONBOARDING_SLIDE_DURATION_MS);
        int direction = targetPage > currentPage ? 1 : -1;

        TranslateTransition slideOut = new TranslateTransition(duration, outgoing);
        slideOut.setFromX(0);
        slideOut.setToX(-direction * AppConstants.APP_WIDTH);

        TranslateTransition slideIn = new TranslateTransition(duration, incoming);
        slideIn.setFromX(direction * AppConstants.APP_WIDTH);
        slideIn.setToX(0);

        slideOut.play();
        slideIn.play();

        // Start the incoming page's video from the beginning (if it has one)
        if (targetPage < pageMediaPlayers.length && pageMediaPlayers[targetPage] != null) {
            pageMediaPlayers[targetPage].seek(Duration.ZERO);
            pageMediaPlayers[targetPage].play();
        }

        currentPage = targetPage;
    }

    // =========================================================
    // Calculator setup handlers (unchanged logic, page 4)
    // =========================================================

    @FXML
    private void handleNumber(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

        Button button = (Button) event.getSource();
        calculator.appendDigit(button.getText());
        updateDisplay();
    }

    @FXML
    private void handleOperation(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

        Button button = (Button) event.getSource();
        String operatorText = button.getText();

        Calculator.Operation operation;
        switch (operatorText) {
            case "+":
                operation = Calculator.Operation.ADD;
                break;
            case "-":
                operation = Calculator.Operation.SUBTRACT;
                break;
            case "\u00D7":
                operation = Calculator.Operation.MULTIPLY;
                break;
            case "\u00F7":
                operation = Calculator.Operation.DIVIDE;
                break;
            default:
                return;
        }

        calculator.setOperation(operation);
        updateDisplay();
    }

    @FXML
    private void handleEquals(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            return;
        }

        capturedEquation = calculator.getCurrentEquation();
        calculator.calculateResult();
        capturedResult = calculator.getCurrentDisplay();
        updateDisplay();

        if (capturedEquation != null && !capturedEquation.isEmpty()
                && !capturedResult.equals(AppConstants.CALC_ERROR_DIV_ZERO)) {
            showConfirmationState();
        } else {
            showErrorMessage("Please enter a valid equation first");
        }
    }

    @FXML
    private void handleDecimal(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

        calculator.appendDecimal();
        updateDisplay();
    }

    @FXML
    private void handleClear(ActionEvent event) {
        calculator.clear();
        updateDisplay();

        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }
    }

    @FXML
    private void handleBackspace(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

        calculator.backspace();
        updateDisplay();
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        currentState = SetupState.CONFIRMED;

        SecretEquation equation = parseEquation(capturedEquation, capturedResult);

        if (equation == null) {
            showErrorMessage("Invalid equation. Please try again.");
            resetToEnteringState();
            return;
        }

        if (!equation.isValid()) {
            showErrorMessage(AppConstants.ERROR_INVALID_MATH);
            resetToEnteringState();
            return;
        }

        boolean success = authService.setupSecretEquation(equation);

        if (success) {
            showSuccessMessage();
            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(e -> SceneManager.getInstance().transitionToCalculator());
            pause.play();
        } else {
            showErrorMessage("Failed to save. Please try again.");
            resetToEnteringState();
        }
    }

    @FXML
    private void handleRetry(ActionEvent event) {
        calculator.clear();
        updateDisplay();
        resetToEnteringState();
    }

    // =========================================================
    // Calculator UI state helpers
    // =========================================================

    private void updateDisplay() {
        displayLabel.setText(calculator.getFullDisplay());
    }

    private void showEnteringState() {
        currentState = SetupState.ENTERING_EQUATION;

        instructionTitle.setText("Create Your Secret Equation");
        instructionMessage.setText("Enter an equation you'll remember");
        instructionHeader.getStyleClass().removeAll("instruction-error", "instruction-success");

        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);
        confirmationOverlay.setMouseTransparent(true);
    }

    private void showConfirmationState() {
        currentState = SetupState.AWAITING_CONFIRMATION;

        String fullEquation = capturedEquation + "=" + capturedResult;
        equationPreview.setText("Your secret: " + fullEquation);

        instructionTitle.setText("Confirm Your Choice");
        instructionMessage.setText("Is this the equation you want?");

        confirmationOverlay.setVisible(true);
        confirmationOverlay.setManaged(true);
        confirmationOverlay.setMouseTransparent(false);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), confirmationPanel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void resetToEnteringState() {
        capturedEquation = null;
        capturedResult = null;
        showEnteringState();
    }

    private void showErrorMessage(String message) {
        instructionTitle.setText("Oops!");
        instructionMessage.setText(message);
        instructionHeader.getStyleClass().add("instruction-error");

        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            instructionHeader.getStyleClass().remove("instruction-error");
            showEnteringState();
        });
        pause.play();
    }

    private void showSuccessMessage() {
        instructionTitle.setText("Success!");
        instructionMessage.setText("Your secret equation has been saved.");
        instructionHeader.getStyleClass().add("instruction-success");

        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);
    }

    private SecretEquation parseEquation(String equation, String result) {
        if (equation == null || equation.isEmpty() || result == null) {
            return null;
        }

        try {
            String operator = null;
            int operatorIndex = -1;
            String[] operators = {"+", "-", "\u00D7", "\u00F7"};

            for (String op : operators) {
                int idx = equation.lastIndexOf(op);
                if (idx > 0) {
                    operatorIndex = idx;
                    operator = op;
                    break;
                }
            }

            if (operator == null || operatorIndex <= 0) {
                return null;
            }

            String leftOperand = equation.substring(0, operatorIndex);
            String rightOperand = equation.substring(operatorIndex + 1);

            Double.parseDouble(leftOperand);
            Double.parseDouble(rightOperand);
            Double.parseDouble(result);

            return new SecretEquation(leftOperand, operator, rightOperand, result);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the calculator model (for testing purposes).
     *
     * @return the calculator model
     */
    public Calculator getCalculator() {
        return calculator;
    }
}
