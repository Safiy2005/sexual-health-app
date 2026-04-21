package com.sddp.sexualhealthapp.settings.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.article.service.ArticleServiceRegistry;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.model.DisguisePreferences;
import com.sddp.sexualhealthapp.settings.model.DisplayMode;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences.VisibilityMode;
import com.sddp.sexualhealthapp.settings.model.TextSizeLevel;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.DisguisePreferencesService;
import com.sddp.sexualhealthapp.settings.service.DisplaySettingsService;
import com.sddp.sexualhealthapp.settings.service.ParentalControlsPinService;
import com.sddp.sexualhealthapp.settings.service.ReminderPreferencesService;
import com.sddp.sexualhealthapp.settings.service.TextSizeSettingsService;
import com.sddp.sexualhealthapp.settings.ui.ParentalControlsPinPrompt;
import com.sddp.sexualhealthapp.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Controller for the settings hub and detail pages.
 */
public class SettingsController {

    private static final int DEFAULT_SUGGESTED_TAGS = 8;
    private static final int MAX_SEARCH_RESULTS = 12;

    private final DisplaySettingsService displaySettingsService = DisplaySettingsService.getInstance();
    private Consumer<DisplayMode> onDisplayModeChanged;

    private final TextSizeSettingsService textSizeSettingsService = TextSizeSettingsService.getInstance();
    private Consumer<TextSizeLevel> onTextSizeChanged;
    private final ParentalControlsPinService parentalControlsPinService;

    private record SettingsPageDefinition(String id, String title, String subtitle, boolean locked,
            PageBuilder builder) {
    }

    @FunctionalInterface
    private interface PageBuilder {
        Node build();
    }

    private enum TagSection {
        BLOCKED,
        PREFERRED
    }

    private record TagPickerRefs(
            FlowPane selectedTagsPane,
            FlowPane availableTagsPane,
            Label availableLabel,
            TextField searchField) {
    }

    @FXML
    private VBox settingsHomeView;
    @FXML
    private VBox settingsDetailView;
    @FXML
    private VBox settingsCardContainer;
    @FXML
    private Label settingsDetailTitle;
    @FXML
    private VBox settingsDetailContent;
    @FXML
    private ScrollPane settingsHomeScrollPane;
    @FXML
    private ScrollPane settingsDetailScrollPane;

    private final ContentPreferencesService preferencesService;
    private final Supplier<List<String>> curatedTagsSupplier;
    private final List<SettingsPageDefinition> pageDefinitions = new ArrayList<>();
    private TagPickerRefs blockedTagPickerRefs;
    private TagPickerRefs preferredTagPickerRefs;
    private String currentPageId;
    private Runnable onPreferencesChanged;

    public SettingsController() {
        this(
                ContentPreferencesService.getInstance(),
                () -> ArticlePersonalizationService.buildCuratedTagList(
                        ArticleServiceRegistry.getArticleCollection().getArticles()));
    }

    SettingsController(ContentPreferencesService preferencesService) {
        this(
                preferencesService,
                () -> ArticlePersonalizationService.buildCuratedTagList(
                        ArticleServiceRegistry.getArticleCollection().getArticles()));
    }

    SettingsController(ContentPreferencesService preferencesService, Supplier<List<String>> curatedTagsSupplier) {
        this(preferencesService, curatedTagsSupplier, ParentalControlsPinService.getInstance());
    }

    SettingsController(ContentPreferencesService preferencesService,
            Supplier<List<String>> curatedTagsSupplier,
            ParentalControlsPinService parentalControlsPinService) {
        this.preferencesService = preferencesService;
        this.curatedTagsSupplier = curatedTagsSupplier;
        this.parentalControlsPinService = parentalControlsPinService;
    }

    @FXML
    private void initialize() {
        settingsHomeScrollPane.getStyleClass().add("search-scroll-pane");
        settingsDetailScrollPane.getStyleClass().add("search-scroll-pane");

        pageDefinitions.add(new SettingsPageDefinition(
                "content-preferences",
                "Content preferences",
                "Block topics and prioritise the tags most relevant to you.",
                true,
                this::buildContentPreferencesPage));

        pageDefinitions.add(new SettingsPageDefinition(
                "display",
                "Display",
                "Switch between standard, dark, and high-contrast views.",
                false,
                this::buildDisplayPage));

        pageDefinitions.add(new SettingsPageDefinition(
                "text-size",
                "Text size",
                "Adjust the global text size across the app",
                false,
                this::buildTextSizePage));

        pageDefinitions.add(new SettingsPageDefinition(
                "reminder-preferences",
                "Reminders & Privacy",
                "Manage how and when you receive event notifications.",
                true,
                this::buildReminderPreferencesPage));

        pageDefinitions.add(new SettingsPageDefinition(
                "parental-controls",
                "Parental controls",
                "Set, change, or remove a PIN that protects sensitive settings pages.",
                true,
                this::buildParentalControlsPage));

        // keep this at the bottom if youre doing a merge for more settings. just makes
        // sense
        pageDefinitions.add(new SettingsPageDefinition(
                "privacy-policy",
                "Privacy Policy",
                "How we keep your data, storage, and notifications secure.",
                true,
                this::buildPrivacyPolicyPage));

        pageDefinitions.add(new SettingsPageDefinition(
                "disguise-settings",
                "App disguise",
                "Choose if the app starts as a calculator or goes straight to the home screen.",
                true,
                this::buildDisguiseSettingsPage));

        renderSettingsCards();
        showHome();
    }

    public void setOnPreferencesChanged(Runnable onPreferencesChanged) {
        this.onPreferencesChanged = onPreferencesChanged;
    }

    public void refresh() {
        preferencesService.reloadFromDisk();
        // Always return to the settings hub when Settings is re-entered so
        // locked pages need the PIN again after leaving and coming back.
        showHome();
    }

    private void refreshCurrentPageContent() {
        SettingsPageDefinition current = getCurrentPage();
        if (current == null) {
            return;
        }

        blockedTagPickerRefs = null;
        preferredTagPickerRefs = null;
        settingsDetailContent.getChildren().setAll(current.builder().build());
    }

    @FXML
    private void handleBackToSettingsHome() {
        showHome();
    }

    private void renderSettingsCards() {
        settingsCardContainer.getChildren().clear();

        boolean pinActive = parentalControlsPinService.hasPin();

        for (SettingsPageDefinition page : pageDefinitions) {
            VBox card = new VBox(6);
            card.getStyleClass().add("settings-card");
            card.setMaxWidth(Double.MAX_VALUE);

            Label title = new Label(page.title());
            title.getStyleClass().add("settings-card-title");
            title.setWrapText(true);
            title.setMaxWidth(Double.MAX_VALUE);

            Label subtitle = new Label(page.subtitle());
            subtitle.getStyleClass().add("settings-card-subtitle");
            subtitle.setWrapText(true);
            subtitle.setMaxWidth(Double.MAX_VALUE);

            VBox textBox = new VBox(4, title, subtitle);
            textBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            HBox trailing = new HBox(8);
            trailing.setAlignment(Pos.CENTER_RIGHT);
            trailing.setMinWidth(Region.USE_PREF_SIZE);

            if (page.locked() && pinActive) {
                Node lockIcon = SvgIcon.load("/icons/lock.svg", "settings-card-lock-icon", 14);
                trailing.getChildren().add(lockIcon);
                card.getStyleClass().add("settings-card-locked");
            }

            Label chevron = new Label(">");
            chevron.getStyleClass().add("settings-card-chevron");
            chevron.setMinWidth(Region.USE_PREF_SIZE);
            trailing.getChildren().add(chevron);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(textBox, trailing);

            card.getChildren().add(row);
            card.setOnMouseClicked(event -> openPage(page));
            settingsCardContainer.getChildren().add(card);
        }
    }

    private void openPage(SettingsPageDefinition page) {
        if (page.locked() && parentalControlsPinService.hasPin()) {
            boolean unlocked = ParentalControlsPinPrompt.requestPin(
                    "Enter your PIN to open \"" + page.title() + "\".",
                    settingsCardContainer);
            if (!unlocked) {
                return;
            }
        }

        currentPageId = page.id();
        settingsDetailTitle.setText(page.title());
        blockedTagPickerRefs = null;
        preferredTagPickerRefs = null;
        settingsDetailContent.getChildren().setAll(page.builder().build());

        settingsHomeView.setVisible(false);
        settingsHomeView.setManaged(false);
        settingsDetailView.setVisible(true);
        settingsDetailView.setManaged(true);
        settingsDetailScrollPane.setVvalue(0);
    }

    private void showHome() {
        currentPageId = null;
        blockedTagPickerRefs = null;
        preferredTagPickerRefs = null;
        renderSettingsCards();
        settingsHomeView.setVisible(true);
        settingsHomeView.setManaged(true);
        settingsDetailView.setVisible(false);
        settingsDetailView.setManaged(false);
        settingsHomeScrollPane.setVvalue(0);
    }

    private Node buildContentPreferencesPage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("settings-page-content");

        Label intro = new Label(
                "Use blocked tags to hide articles across browse, search, and recommendations. "
                        + "Use preferred tags to gently lift relevant results in search.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        Label blockedTitle = new Label("Blocked tags");
        blockedTitle.getStyleClass().add("settings-section-title");
        Label blockedBody = new Label(
                "Articles with these tags will stay out of article lists, search results, and recommendations.");
        blockedBody.getStyleClass().add("settings-section-body");
        blockedBody.setWrapText(true);

        Label preferredTitle = new Label("Prioritise these tags");
        preferredTitle.getStyleClass().add("settings-section-title");
        Label preferredBody = new Label(
                "These tags add a small ranking boost in search and receive a stronger result highlight.");
        preferredBody.getStyleClass().add("settings-section-body");
        preferredBody.setWrapText(true);

        page.getChildren().addAll(
                intro,
                blockedTitle, blockedBody, buildTagPicker(TagSection.BLOCKED),
                preferredTitle, preferredBody, buildTagPicker(TagSection.PREFERRED));
        return page;
    }

    private Node buildReminderPreferencesPage() {
        VBox page = new VBox(20);
        page.getStyleClass().add("settings-page-content");
        page.setPadding(new Insets(0, 0, 80, 0));

        Label intro = new Label("Control your privacy by choosing how event reminders appear on your screen.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        Label modeTitle = new Label("Reminder Visibility");
        modeTitle.getStyleClass().add("settings-section-title");

        ToggleGroup modeGroup = new ToggleGroup();
        TextField customTitleField = new TextField();
        TextField customBodyField = new TextField();

        // --- Custom Disguise Box ---
        // Increased gap to 12px between the two input fields
        VBox customDisguiseBox = new VBox(12,
                createLabeledInput("Disguise Title:", customTitleField, ReminderPreferences.DEFAULT_TITLE),
                createLabeledInput("Disguise Body Text (Time will be appended):", customBodyField,
                        ReminderPreferences.DEFAULT_BODY));
        // Indent it nicely under the "Disguised" description
        customDisguiseBox.setPadding(new Insets(12, 0, 8, 24));
        customDisguiseBox.setVisible(false);
        customDisguiseBox.setManaged(false);

        // --- Build Radio Options ---
        RadioButton offBtn = new RadioButton("Off");
        RadioButton disguisedBtn = new RadioButton("Disguised (Maximum Privacy)");
        RadioButton discreetBtn = new RadioButton("Discreet");
        RadioButton explicitBtn = new RadioButton("Detailed");

        // Increased gap between the radio groups to 20px
        VBox radioBox = new VBox(20);
        radioBox.getStyleClass().add("settings-tag-picker");
        // Force a bit of extra padding inside the white card
        radioBox.setPadding(new Insets(18));

        radioBox.getChildren().addAll(
                createRadioOption(offBtn, "No pop-ups. Events will only stay in your feed.", modeGroup,
                        VisibilityMode.OFF),
                createRadioOption(disguisedBtn, "Mimics system alerts or calculator tasks.", modeGroup,
                        VisibilityMode.DISGUISED, customDisguiseBox),
                createRadioOption(discreetBtn, "Shows times only (e.g., \"Upcoming Event\").", modeGroup,
                        VisibilityMode.DISCREET),
                createRadioOption(explicitBtn, "Shows full event names and any notes you've written.", modeGroup,
                        VisibilityMode.EXPLICIT));

        // --- Load Saved State ---
        ReminderPreferences currentPrefs = ReminderPreferencesService.getInstance().getPreferences();
        customTitleField.setText(currentPrefs.customDisguisedTitle());
        customBodyField.setText(currentPrefs.customDisguisedBody());

        switch (currentPrefs.visibilityMode()) {
            case OFF -> offBtn.setSelected(true);
            case DISGUISED -> {
                disguisedBtn.setSelected(true);
                customDisguiseBox.setVisible(true);
                customDisguiseBox.setManaged(true);
            }
            case DISCREET -> discreetBtn.setSelected(true);
            case EXPLICIT -> explicitBtn.setSelected(true);
        }

        // --- Event Listeners ---
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDisguised = (newVal == disguisedBtn);
            customDisguiseBox.setVisible(isDisguised);
            customDisguiseBox.setManaged(isDisguised);
            saveReminderSettings(modeGroup, customTitleField.getText(), customBodyField.getText());
        });

        customTitleField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused)
                saveReminderSettings(modeGroup, customTitleField.getText(), customBodyField.getText());
        });

        customBodyField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused)
                saveReminderSettings(modeGroup, customTitleField.getText(), customBodyField.getText());
        });

        page.getChildren().addAll(intro, modeTitle, radioBox);
        return page;
    }

    private void saveReminderSettings(ToggleGroup modeGroup, String customTitle, String customBody) {
        VisibilityMode mode = VisibilityMode.OFF;

        if (modeGroup.getSelectedToggle() != null) {
            // Directly cast the user data back to the Enum. No string matching required!
            mode = (VisibilityMode) modeGroup.getSelectedToggle().getUserData();
        }

        ReminderPreferences prefs = new ReminderPreferences(mode, customTitle, customBody);
        ReminderPreferencesService.getInstance().savePreferences(prefs);
    }

    private Node buildPrivacyPolicyPage() {
        VBox page = new VBox(14);
        page.getStyleClass().add("settings-page-content");
        page.setPadding(new Insets(0, 0, 80, 0));
        Label intro = new Label(
                "We believe your health data is yours alone. Here is exactly how we protect your privacy.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);
        page.getChildren().add(intro);

        // --- Standard Policy Cards ---
        page.getChildren().add(createPolicyCard("Local Storage & Data",
                "All your data, including preferences and activity, is stored strictly locally on your device. It will never be uploaded, sold, or shared with external sources."));

        page.getChildren().add(createPolicyCard("Zero Analytics",
                "We do not track your usage, monitor what articles you read, or collect behavioral data. There are no tracking scripts hidden in the background."));

        page.getChildren().add(createPolicyCard("Notifications & Reminders",
                "All reminders are generated directly on your device. You have full control over how they appear, including a disguised mode to protect your privacy."));

        page.getChildren().add(createPolicyCard("Passcode Encryption",
                "Your passcode is securely encrypted. You can reset your passcode by entering 999/0 into the calculator disguise."));

        // --- Danger Zone: Delete All Data (Mobile Inline Style) ---
        VBox dangerZone = createPolicyCard("Reset App",
                "This will permanently erase all your calendar events, app preferences, and your current passcode. The app will be completely reset.");

        // The initial button
        Button deleteBtn = new Button("Delete All Data");
        deleteBtn.setStyle(
                "-fx-background-color: #F6E0E0; -fx-text-fill: #9A5151; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;");
        deleteBtn.setTranslateY(4);

        // The inline confirmation UI (Hidden by default)
        VBox confirmBox = new VBox(10);
        confirmBox.setVisible(false);
        confirmBox.setManaged(false);
        confirmBox.setPadding(new Insets(10, 0, 0, 0));

        Label confirmLabel = new Label("Type 'I want to fully reset the app' to confirm:");
        confirmLabel.setStyle("-fx-text-fill: #9A5151; -fx-font-size: 12px; -fx-font-weight: bold;");
        confirmLabel.setWrapText(true);

        TextField confirmInput = new TextField();
        confirmInput.getStyleClass().addAll("search-field", "settings-tag-search-field");
        confirmInput.setPromptText("I want to fully reset the app");

        Label errorLabel = new Label("Text does not match. Please try again.");
        errorLabel.setStyle("-fx-text-fill: #9A5151; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button confirmWipeBtn = new Button("Confirm Reset");
        confirmWipeBtn.setStyle(
                "-fx-background-color: #9A5151; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #5C6B6B; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox actionBox = new HBox(12, confirmWipeBtn, cancelBtn);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        confirmBox.getChildren().addAll(confirmLabel, confirmInput, errorLabel, actionBox);

        // --- Wiring the Button Actions ---

        // When initial Delete button is tapped -> Hide button, Show confirm UI
        deleteBtn.setOnAction(e -> {
            deleteBtn.setVisible(false);
            deleteBtn.setManaged(false);
            confirmBox.setVisible(true);
            confirmBox.setManaged(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            confirmInput.clear();
        });

        // When Cancel is tapped -> Hide confirm UI, Show initial Delete button
        cancelBtn.setOnAction(e -> {
            confirmBox.setVisible(false);
            confirmBox.setManaged(false);
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
        });

        // When Confirm Reset is tapped -> Validate, Reset UI, and Wipe
        confirmWipeBtn.setOnAction(e -> {
            if ("I want to fully reset the app".equals(confirmInput.getText().trim())) {
                confirmInput.clear();
                confirmBox.setVisible(false);
                confirmBox.setManaged(false);
                deleteBtn.setVisible(true);
                deleteBtn.setManaged(true);
                showHome();
                com.sddp.sexualhealthapp.util.AppResetService.wipeAllDataAndReset();

            } else {
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        dangerZone.getChildren().addAll(deleteBtn, confirmBox);
        page.getChildren().add(dangerZone);

        return page;
    }

    // Creates a card that looks exactly like a settings card but WITHOUT the
    // hover/clickable effects
    private VBox createPolicyCard(String titleText, String bodyText) {
        VBox card = new VBox(6);
        // We use inline styles here to bypass the hover effects of the .settings-card
        // CSS class
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12px; -fx-padding: 14px 16px; -fx-effect: dropshadow(gaussian, rgba(61, 90, 91, 0.08), 6, 0, 0, 2);");

        Label title = new Label(titleText);
        title.getStyleClass().add("settings-section-title");

        Label body = new Label(bodyText);
        body.getStyleClass().add("settings-section-body");
        body.setWrapText(true);

        card.getChildren().addAll(title, body);
        return card;
    }

    private VBox createRadioOption(RadioButton btn, String description, ToggleGroup group, VisibilityMode mode,
            Node... extraContent) {
        btn.getStyleClass().add("settings-subsection-label");
        btn.setToggleGroup(group);
        btn.setUserData(mode);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("settings-section-body");
        descLabel.setWrapText(true);
        // Indent the description slightly so it aligns with the radio button text
        descLabel.setPadding(new Insets(0, 0, 0, 24));

        VBox box = new VBox(4, btn, descLabel);
        box.getChildren().addAll(extraContent);

        // --- NEW UX UPGRADES ---
        // Apply the new CSS class for padding, hover background, and hand cursor
        box.getStyleClass().add("settings-radio-box");

        // Make the entire VBox (the "red box" from your screenshot) clickable!
        box.setOnMouseClicked(event -> {
            btn.setSelected(true);
            btn.requestFocus(); // Shift focus so the UI visually reacts
        });

        return box;
    }

    private VBox createLabeledInput(String labelText, TextField field, String prompt) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-subsection-label");

        field.getStyleClass().addAll("search-field", "settings-tag-search-field");
        field.setPromptText("e.g., " + prompt);

        // 6px spacing between the label and the text box so they don't touch
        return new VBox(6, label, field);
    }

    private Node buildParentalControlsPage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("settings-page-content");
        page.setPadding(new Insets(0, 0, 80, 0));

        Label intro = new Label(
                "Use a PIN to protect sensitive settings pages. Display and Text size stay unlocked; "
                        + "everything else will ask for the PIN before opening.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        VBox panel = new VBox(12);
        panel.getStyleClass().add("settings-tag-picker");

        Label status = new Label(parentalControlsPinService.hasPin() ? "Status: PIN enabled" : "Status: PIN not set");
        status.getStyleClass().add("settings-subsection-label");

        Label resultLabel = new Label();
        resultLabel.getStyleClass().add("settings-section-body");
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);
        resultLabel.setManaged(false);

        panel.getChildren().add(status);

        if (!parentalControlsPinService.hasPin()) {
            panel.getChildren().add(buildSetPinForm(status, resultLabel));
        } else {
            panel.getChildren().addAll(
                    buildChangePinForm(status, resultLabel),
                    buildRemovePinForm(status, resultLabel));
        }

        panel.getChildren().add(resultLabel);
        page.getChildren().addAll(intro, panel);
        return page;
    }

    private Node buildSetPinForm(Label statusLabel, Label resultLabel) {
        VBox section = new VBox(8);

        Label title = new Label("Set a PIN");
        title.getStyleClass().add("settings-section-title");

        Label body = new Label("PIN must contain digits only and cannot be empty.");
        body.getStyleClass().add("settings-section-body");
        body.setWrapText(true);

        PasswordField newPinField = createPinField("New PIN");
        PasswordField confirmPinField = createPinField("Confirm PIN");

        Button setPinButton = new Button("Save PIN");
        setPinButton.getStyleClass().add("calendar-action-button");
        setPinButton.setOnAction(event -> {
            String newPin = newPinField.getText();
            String confirmPin = confirmPinField.getText();

            if (!ParentalControlsPinService.isValidPinFormat(newPin)) {
                showPinResult(resultLabel, "PIN must contain digits only and cannot be empty.", true);
                return;
            }

            if (!newPin.equals(confirmPin)) {
                showPinResult(resultLabel, "PIN confirmation does not match.", true);
                return;
            }

            if (!parentalControlsPinService.setPin(newPin)) {
                showPinResult(resultLabel, "Could not save PIN. Please try again.", true);
                return;
            }

            newPinField.clear();
            confirmPinField.clear();
            statusLabel.setText("Status: PIN enabled");
            showPinResult(resultLabel, "PIN set successfully.", false);
            refreshCurrentPageContent();
        });

        section.getChildren().addAll(title, body, newPinField, confirmPinField, setPinButton);
        return section;
    }

    private Node buildChangePinForm(Label statusLabel, Label resultLabel) {
        VBox section = new VBox(8);

        Label title = new Label("Change PIN");
        title.getStyleClass().add("settings-section-title");

        PasswordField newPinField = createPinField("New PIN (digits only)");
        PasswordField confirmPinField = createPinField("Confirm new PIN");

        Button changePinButton = new Button("Change PIN");
        changePinButton.getStyleClass().add("calendar-action-button");
        changePinButton.setOnAction(event -> {
            String newPin = newPinField.getText();
            String confirmPin = confirmPinField.getText();

            if (!ParentalControlsPinService.isValidPinFormat(newPin)) {
                showPinResult(resultLabel, "New PIN must contain digits only and cannot be empty.", true);
                return;
            }

            if (!newPin.equals(confirmPin)) {
                showPinResult(resultLabel, "PIN confirmation does not match.", true);
                return;
            }

            if (!parentalControlsPinService.setPin(newPin)) {
                showPinResult(resultLabel, "Could not change PIN. Please try again.", true);
                return;
            }

            newPinField.clear();
            confirmPinField.clear();
            statusLabel.setText("Status: PIN enabled");
            showPinResult(resultLabel, "PIN changed successfully.", false);
        });

        section.getChildren().addAll(title, newPinField, confirmPinField, changePinButton);
        return section;
    }

    private Node buildRemovePinForm(Label statusLabel, Label resultLabel) {
        VBox section = new VBox(8);

        Label title = new Label("Remove PIN");
        title.getStyleClass().add("settings-section-title");

        Label body = new Label("Remove the PIN to disable Settings protection.");
        body.getStyleClass().add("settings-section-body");
        body.setWrapText(true);

        Button removePinButton = new Button("Remove PIN");
        removePinButton.setStyle(
                "-fx-background-color: #F6E0E0; -fx-text-fill: #9A5151; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;");
        removePinButton.setOnAction(event -> {
            if (!parentalControlsPinService.removePinIfPresent()) {
                showPinResult(resultLabel, "Could not remove PIN. Please try again.", true);
                return;
            }

            statusLabel.setText("Status: PIN not set");
            showPinResult(resultLabel, "PIN removed.", false);
            refreshCurrentPageContent();
        });

        section.getChildren().addAll(title, body, removePinButton);
        return section;
    }

    private PasswordField createPinField(String promptText) {
        PasswordField field = new PasswordField();
        field.getStyleClass().addAll("search-field", "settings-tag-search-field");
        field.setPromptText(promptText);
        return field;
    }

    private void showPinResult(Label label, String message, boolean isError) {
        label.setText(message);
        label.setStyle(isError
                ? "-fx-text-fill: #9A5151;"
                : "-fx-text-fill: #3D7A75;");
        label.setVisible(true);
        label.setManaged(true);
    }

    private Node buildDisguiseSettingsPage() {
        VBox page = new VBox(20);
        page.getStyleClass().add("settings-page-content");
        page.setPadding(new Insets(0, 0, 80, 0));

        Label intro = new Label(
                "Control your privacy by choosing whether the app hides behind a calculator on startup.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        Label modeTitle = new Label("Startup Behavior");
        modeTitle.getStyleClass().add("settings-section-title");

        ToggleGroup modeGroup = new ToggleGroup();

        RadioButton enabledBtn = new RadioButton("Enabled (Maximum Privacy)");
        RadioButton disabledBtn = new RadioButton("Disabled (Direct Access)");

        VBox radioBox = new VBox(20);
        radioBox.getStyleClass().add("settings-tag-picker");
        radioBox.setPadding(new Insets(18));

        radioBox.getChildren().addAll(
                createDisguiseRadioOption(enabledBtn,
                        "Requires your secret equation on startup. Mimics a standard calculator.", modeGroup, true),
                createDisguiseRadioOption(disabledBtn, "Skips the calculator and opens directly to your articles.",
                        modeGroup, false));

        // Load Saved State
        DisguisePreferencesService service = DisguisePreferencesService.getInstance();
        if (service.getPreferences().calcDisguiseEnabled()) {
            enabledBtn.setSelected(true);
        } else {
            disabledBtn.setSelected(true);
        }

        // Event Listener
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                boolean isEnabled = (boolean) newVal.getUserData();
                service.save(new DisguisePreferences(isEnabled));
            }
        });

        // Use the new helper method to build the exact box from your image
        VBox tipBox = createInfoBox("Tip",
                "If you ever forget your passcode, type 999/0 into the calculator screen to safely reset it.");

        page.getChildren().addAll(intro, modeTitle, radioBox, tipBox);
        return page;
    }
    // --- HELPER METHODS FOR DISGUISE PAGE ---

    private VBox createDisguiseRadioOption(RadioButton btn, String description, ToggleGroup group, boolean isEnabled) {
        btn.getStyleClass().add("settings-subsection-label");
        btn.setToggleGroup(group);
        btn.setUserData(isEnabled);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("settings-section-body");
        descLabel.setWrapText(true);
        descLabel.setPadding(new Insets(0, 0, 0, 24)); // Align text under the radio button label

        VBox box = new VBox(4, btn, descLabel);
        box.getStyleClass().add("settings-radio-box");

        // Make the entire box clickable, exactly like the reminders page
        box.setOnMouseClicked(event -> {
            btn.setSelected(true);
            btn.requestFocus();
        });

        return box;
    }

    private VBox createInfoBox(String titleText, String bodyText) {
        VBox box = new VBox(4);
        box.getStyleClass().add("settings-info-box");

        // Create the bold bullet point title
        Label title = new Label("•  " + titleText);
        title.getStyleClass().add("settings-info-title");

        // Create the regular body text and indent it to align under the word, not the
        // bullet
        Label body = new Label(bodyText);
        body.getStyleClass().add("settings-info-body");
        body.setWrapText(true);
        body.setPadding(new Insets(0, 0, 0, 16));

        box.getChildren().addAll(title, body);
        return box;
    }

    private FlowPane createTagPane() {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);
        pane.setPadding(new Insets(2, 0, 0, 0));
        return pane;
    }

    private VBox buildTagPicker(TagSection section) {
        VBox picker = new VBox(10);
        picker.getStyleClass().add("settings-tag-picker");

        Label selectedLabel = new Label("Selected");
        selectedLabel.getStyleClass().add("settings-subsection-label");
        FlowPane selectedTagsPane = createTagPane();
        selectedTagsPane.getStyleClass().add("settings-selected-tags");

        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.getStyleClass().add("settings-tag-search-field");
        searchField.setPromptText("Search tags...");

        Label availableLabel = new Label("Suggestions");
        availableLabel.getStyleClass().add("settings-subsection-label");
        FlowPane availableTagsPane = createTagPane();

        TagPickerRefs refs = new TagPickerRefs(selectedTagsPane, availableTagsPane, availableLabel, searchField);
        if (section == TagSection.BLOCKED) {
            blockedTagPickerRefs = refs;
        } else {
            preferredTagPickerRefs = refs;
        }

        updateTagPicker(refs, section);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            updateTagPicker(refs, section);
        });

        picker.getChildren().addAll(selectedLabel, selectedTagsPane, searchField, availableLabel, availableTagsPane);
        return picker;
    }

    private void renderSelectedTagChips(FlowPane container, TagSection section) {
        container.getChildren().clear();
        ContentPreferences preferences = preferencesService.getPreferences();
        List<String> selectedTags = section == TagSection.BLOCKED
                ? preferences.blockedTags()
                : preferences.preferredTags();

        if (selectedTags.isEmpty()) {
            Label empty = new Label("No tags selected yet.");
            empty.getStyleClass().add("settings-empty-hint");
            container.getChildren().add(empty);
            return;
        }

        for (String tag : selectedTags) {
            Button chip = createTagChip(section, tag);
            container.getChildren().add(chip);
        }
    }

    private void renderAvailableTagChips(FlowPane container, TagSection section, String query) {
        container.getChildren().clear();
        List<String> tagsToShow = getAvailableTags(section, query);

        if (tagsToShow.isEmpty()) {
            Label empty = new Label("No matching tags.");
            empty.getStyleClass().add("settings-empty-hint");
            container.getChildren().add(empty);
            return;
        }

        for (String tag : tagsToShow) {
            container.getChildren().add(createTagChip(section, tag));
        }
    }

    private Button createTagChip(TagSection section, String tag) {
        ContentPreferences preferences = preferencesService.getPreferences();
        Button chip = new Button(tag);
        chip.getStyleClass().add("settings-tag-chip");
        chip.setWrapText(true);

        boolean isBlocked = containsTag(preferences.blockedTags(), tag);
        boolean isPreferred = containsTag(preferences.preferredTags(), tag);

        if (section == TagSection.BLOCKED && isBlocked) {
            chip.getStyleClass().add("settings-tag-chip-blocked");
        } else if (section == TagSection.PREFERRED && isPreferred) {
            chip.getStyleClass().add("settings-tag-chip-preferred");
        }

        chip.setOnAction(event -> toggleTag(section, tag));
        return chip;
    }

    private List<String> getAvailableTags(TagSection section, String query) {
        ContentPreferences preferences = preferencesService.getPreferences();
        List<String> curatedTags = curatedTagsSupplier.get();
        List<String> selectedTags = section == TagSection.BLOCKED
                ? preferences.blockedTags()
                : preferences.preferredTags();

        return curatedTags.stream()
                .filter(tag -> !containsTag(selectedTags, tag))
                .filter(tag -> query == null || query.isBlank()
                        || tag.toLowerCase().contains(query.toLowerCase().trim()))
                .limit(query == null || query.isBlank() ? DEFAULT_SUGGESTED_TAGS : MAX_SEARCH_RESULTS)
                .toList();
    }

    private void toggleTag(TagSection section, String tag) {
        ContentPreferences preferences = preferencesService.getPreferences();
        List<String> blocked = new ArrayList<>(preferences.blockedTags());
        List<String> preferred = new ArrayList<>(preferences.preferredTags());

        if (section == TagSection.BLOCKED) {
            toggleMembership(blocked, tag);
            removeMatchingTag(preferred, tag);
        } else {
            toggleMembership(preferred, tag);
            removeMatchingTag(blocked, tag);
        }

        preferencesService.savePreferences(new ContentPreferences(blocked, preferred));
        notifyPreferencesChanged();
        updateVisibleTagPickers();
    }

    private void notifyPreferencesChanged() {
        if (onPreferencesChanged != null) {
            onPreferencesChanged.run();
        }
    }

    private boolean containsTag(List<String> tags, String target) {
        return tags.stream().anyMatch(tag -> sameTag(tag, target));
    }

    private void toggleMembership(List<String> tags, String target) {
        int existingIndex = indexOfMatchingTag(tags, target);
        if (existingIndex >= 0) {
            tags.remove(existingIndex);
        } else {
            tags.add(target);
        }
    }

    private void removeMatchingTag(List<String> tags, String target) {
        int existingIndex = indexOfMatchingTag(tags, target);
        if (existingIndex >= 0) {
            tags.remove(existingIndex);
        }
    }

    private int indexOfMatchingTag(List<String> tags, String target) {
        for (int i = 0; i < tags.size(); i++) {
            if (sameTag(tags.get(i), target)) {
                return i;
            }
        }
        return -1;
    }

    private boolean sameTag(String left, String right) {
        return ArticlePersonalizationService.canonicalTagKey(left)
                .equals(ArticlePersonalizationService.canonicalTagKey(right));
    }

    private SettingsPageDefinition getCurrentPage() {
        return pageDefinitions.stream()
                .filter(page -> page.id().equals(currentPageId))
                .findFirst()
                .orElse(null);
    }

    public void setOnDisplayModeChanged(Consumer<DisplayMode> onDisplayModeChanged) {
        this.onDisplayModeChanged = onDisplayModeChanged;
    }

    private Node buildDisplayPage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("settings-page-content");

        Label intro = new Label(
                "Choose the visual style that is most comfortable and readable for you.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        Label modeTitle = new Label("Display mode");
        modeTitle.getStyleClass().add("settings-section-title");

        Label modeBody = new Label(
                "Standard keeps the default appearance, Dark reduces brightness, and High Contrast increases separation between text and controls.");
        modeBody.getStyleClass().add("settings-section-body");
        modeBody.setWrapText(true);

        VBox optionsBox = new VBox(10);
        optionsBox.getStyleClass().add("settings-tag-picker");

        ToggleGroup modeGroup = new ToggleGroup();
        DisplayMode currentMode = displaySettingsService.getDisplayMode();

        for (DisplayMode mode : DisplayMode.values()) {
            RadioButton radio = new RadioButton(mode.getDisplayName());
            radio.setToggleGroup(modeGroup);
            radio.getStyleClass().add("settings-display-radio");
            radio.setWrapText(true);
            radio.setSelected(mode == currentMode);

            radio.setOnAction(event -> {
                displaySettingsService.setDisplayMode(mode);
                if (onDisplayModeChanged != null) {
                    onDisplayModeChanged.accept(mode);
                }
            });

            optionsBox.getChildren().add(radio);
        }

        Button resetButton = new Button("Reset to standard view");
        resetButton.getStyleClass().add("calendar-action-button");
        resetButton.setOnAction(event -> {
            displaySettingsService.resetDisplayMode();
            if (onDisplayModeChanged != null) {
                onDisplayModeChanged.accept(DisplayMode.STANDARD);
            }
            refresh();
        });

        page.getChildren().addAll(intro, modeTitle, modeBody, optionsBox, resetButton);
        return page;
    }

    public void setOnTextSizeChanged(Consumer<TextSizeLevel> onTextSizeChanged) {
        this.onTextSizeChanged = onTextSizeChanged;
    }

    private Node buildTextSizePage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("settings-page-content");

        Label intro = new Label(
                "Choose a bounded text size that makes the app easier to read.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);

        Label title = new Label("Global text size");
        title.getStyleClass().add("settings-section-title");

        Label body = new Label(
                "The selected size is applied consistently across articles, calendar, event screens, and settings.");
        body.getStyleClass().add("settings-section-body");
        body.setWrapText(true);

        VBox optionsBox = new VBox(10);
        optionsBox.getStyleClass().add("settings-tag-picker");

        ToggleGroup textSizeGroup = new ToggleGroup();
        TextSizeLevel currentLevel = textSizeSettingsService.getTextSizeLevel();

        for (TextSizeLevel level : TextSizeLevel.values()) {
            RadioButton radio = new RadioButton(level.getDisplayName());
            radio.setToggleGroup(textSizeGroup);
            radio.getStyleClass().add("settings-display-radio");
            radio.setWrapText(true);
            radio.setSelected(level == currentLevel);

            radio.setOnAction(event -> {
                textSizeSettingsService.setTextSizeLevel(level);
                if (onTextSizeChanged != null) {
                    onTextSizeChanged.accept(level);
                }
            });

            optionsBox.getChildren().add(radio);
        }

        Button resetButton = new Button("Reset to standard size");
        resetButton.getStyleClass().add("calendar-action-button");
        resetButton.setOnAction(event -> {
            textSizeSettingsService.resetTextSizeLevel();
            if (onTextSizeChanged != null) {
                onTextSizeChanged.accept(TextSizeLevel.STANDARD);
            }
            refresh();
        });

        page.getChildren().addAll(intro, title, body, optionsBox, resetButton);
        return page;
    }

    private void updateVisibleTagPickers() {
        if (blockedTagPickerRefs != null) {
            updateTagPicker(blockedTagPickerRefs, TagSection.BLOCKED);
        }
        if (preferredTagPickerRefs != null) {
            updateTagPicker(preferredTagPickerRefs, TagSection.PREFERRED);
        }
    }

    private void updateTagPicker(TagPickerRefs refs, TagSection section) {
        if (refs == null) {
            return;
        }

        String query = refs.searchField().getText();
        refs.availableLabel().setText(query == null || query.isBlank() ? "Suggestions" : "Matches");
        renderSelectedTagChips(refs.selectedTagsPane(), section);
        renderAvailableTagChips(refs.availableTagsPane(), section, query);
    }
}
