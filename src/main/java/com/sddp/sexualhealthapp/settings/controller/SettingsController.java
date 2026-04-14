package com.sddp.sexualhealthapp.settings.controller;

import com.sddp.sexualhealthapp.article.service.ArticleServiceRegistry;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.ReminderPreferencesService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences.VisibilityMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Controller for the settings hub and detail pages.
 */
public class SettingsController {

    private static final int DEFAULT_SUGGESTED_TAGS = 8;
    private static final int MAX_SEARCH_RESULTS = 12;

    private record SettingsPageDefinition(String id, String title, String subtitle, PageBuilder builder) {
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
        this.preferencesService = preferencesService;
        this.curatedTagsSupplier = curatedTagsSupplier;
    }

    @FXML
    private void initialize() {
        settingsHomeScrollPane.getStyleClass().add("search-scroll-pane");
        settingsDetailScrollPane.getStyleClass().add("search-scroll-pane");

        pageDefinitions.add(new SettingsPageDefinition(
                "content-preferences",
                "Content preferences",
                "Block topics and prioritise the tags most relevant to you.",
                this::buildContentPreferencesPage));

        pageDefinitions.add(new SettingsPageDefinition(
                "reminder-preferences",
                "Reminders & Privacy",
                "Manage how and when you receive event notifications.",
                this::buildReminderPreferencesPage));

        // keep this at the bottom if youre doing a merge for more settings. just makes sense
        pageDefinitions.add(new SettingsPageDefinition(
                "privacy-policy",
                "Privacy Policy",
                "How we keep your data, storage, and notifications secure.",
                this::buildPrivacyPolicyPage));

        renderSettingsCards();
        showHome();
    }

    public void setOnPreferencesChanged(Runnable onPreferencesChanged) {
        this.onPreferencesChanged = onPreferencesChanged;
    }

    public void refresh() {
        preferencesService.reloadFromDisk();
        if (currentPageId == null) {
            renderSettingsCards();
            return;
        }

        SettingsPageDefinition currentPage = getCurrentPage();
        if (currentPage != null) {
            openPage(currentPage);
        }
    }

    @FXML
    private void handleBackToSettingsHome() {
        showHome();
    }

    private void renderSettingsCards() {
        settingsCardContainer.getChildren().clear();

        for (SettingsPageDefinition page : pageDefinitions) {
            VBox card = new VBox(6);
            card.getStyleClass().add("settings-card");

            Label title = new Label(page.title());
            title.getStyleClass().add("settings-card-title");
            title.setWrapText(true);

            Label subtitle = new Label(page.subtitle());
            subtitle.getStyleClass().add("settings-card-subtitle");
            subtitle.setWrapText(true);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            VBox textBox = new VBox(4, title, subtitle);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label chevron = new Label(">");
            chevron.getStyleClass().add("settings-card-chevron");
            row.getChildren().addAll(textBox, spacer, chevron);

            card.getChildren().add(row);
            card.setOnMouseClicked(event -> openPage(page));
            settingsCardContainer.getChildren().add(card);
        }
    }

    private void openPage(SettingsPageDefinition page) {
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
        VBox page = new VBox(18); // Spacing between cards
        page.getStyleClass().add("settings-page-content");

        Label intro = new Label("We believe your health data is yours alone. Here is exactly how we protect your privacy.");
        intro.getStyleClass().add("settings-page-intro");
        intro.setWrapText(true);
        page.getChildren().add(intro);

        // Card 1: Local Storage
        page.getChildren().add(createPolicyCard("Local Storage & Data",
                "All your data, including preferences and activity, is stored strictly locally on your device. It will never be uploaded, sold, or shared with external sources."));

        // Card 2: Zero Analytics
        page.getChildren().add(createPolicyCard("Zero Analytics",
                "We do not track your usage, monitor what articles you read, or collect behavioral data. There are no tracking scripts hidden in the background."));

        // Card 3: Notifications
        page.getChildren().add(createPolicyCard("Notifications & Reminders",
                "All reminders are generated directly on your device. You have full control over how they appear, including a disguised mode to protect your privacy."));

        // Card 4: Passcode & Reset
        page.getChildren().add(createPolicyCard("Passcode Encryption",
                "Your passcode is securely encrypted. If you forget your passcode you can reset it by entering the equation 999/0 into the calculator disguise."));

        return page;
    }

    // Helper method to create consistent cards
    private VBox createPolicyCard(String titleText, String bodyText) {
        VBox card = new VBox(6);
        card.getStyleClass().add("settings-card"); // Matches the main settings cards

        Label title = new Label(titleText);
        title.getStyleClass().add("settings-section-title"); // teal bold

        Label body = new Label(bodyText);
        body.getStyleClass().add("settings-section-body"); // gray text
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
