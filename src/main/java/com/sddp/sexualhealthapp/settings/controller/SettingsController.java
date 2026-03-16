package com.sddp.sexualhealthapp.settings.controller;

import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
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
    private String currentPageId;
    private Runnable onPreferencesChanged;

    public SettingsController() {
        this(
                ContentPreferencesService.getInstance(),
                () -> ArticlePersonalizationService.buildCuratedTagList(
                        ArticleCollection.getInstance().getArticles()));
    }

    SettingsController(ContentPreferencesService preferencesService) {
        this(
                preferencesService,
                () -> ArticlePersonalizationService.buildCuratedTagList(
                        ArticleCollection.getInstance().getArticles()));
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
        settingsDetailContent.getChildren().setAll(page.builder().build());

        settingsHomeView.setVisible(false);
        settingsHomeView.setManaged(false);
        settingsDetailView.setVisible(true);
        settingsDetailView.setManaged(true);
        settingsDetailScrollPane.setVvalue(0);
    }

    private void showHome() {
        currentPageId = null;
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
        Label blockedBody = new Label("Articles with these tags will stay out of article lists, search results, and recommendations.");
        blockedBody.getStyleClass().add("settings-section-body");
        blockedBody.setWrapText(true);

        Label preferredTitle = new Label("Prioritise these tags");
        preferredTitle.getStyleClass().add("settings-section-title");
        Label preferredBody = new Label("These tags add a small ranking boost in search and receive a stronger result highlight.");
        preferredBody.getStyleClass().add("settings-section-body");
        preferredBody.setWrapText(true);

        page.getChildren().addAll(
                intro,
                blockedTitle, blockedBody, buildTagPicker(TagSection.BLOCKED),
                preferredTitle, preferredBody, buildTagPicker(TagSection.PREFERRED));
        return page;
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

        renderSelectedTagChips(selectedTagsPane, section);
        renderAvailableTagChips(availableTagsPane, section, "");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String label = newValue == null || newValue.isBlank() ? "Suggestions" : "Matches";
            availableLabel.setText(label);
            renderAvailableTagChips(availableTagsPane, section, newValue);
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
        refresh();
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
}
