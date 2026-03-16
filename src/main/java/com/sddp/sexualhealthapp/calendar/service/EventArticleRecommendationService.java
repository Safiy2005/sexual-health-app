package com.sddp.sexualhealthapp.calendar.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.article.service.TextPreprocessor;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Recommends articles for a calendar event.
 *
 * <p>
 * Ranking strategy:
 * </p>
 * <ul>
 * <li>Primary signal: event title intent (75%)</li>
 * <li>Secondary signal: event description context (25%)</li>
 * <li>Small metadata boost from overlap with article tags</li>
 * </ul>
 *
 * <p>
 * The service always attempts to return at least one recommendation when
 * there are any searchable matches.
 * </p>
 */
public class EventArticleRecommendationService {

    private static final double TITLE_WEIGHT = 0.75;
    private static final double DESCRIPTION_WEIGHT = 0.25;
    private static final double RELEVANCE_THRESHOLD = 0.50;
    private static final double ADDITIONAL_RESULT_MIN_SCORE = 0.55;
    private static final double ADDITIONAL_RESULT_TOP_RATIO = 0.85;
    private static final double TAG_BOOST_CAP = 0.12;

    @FunctionalInterface
    interface QuerySearch {
        List<SearchResult> search(String query, double minScore);
    }

    public record Recommendation(Article article, double score) {
    }

    private final QuerySearch querySearch;
    private final Supplier<ContentPreferences> preferencesSupplier;

    /** Production constructor. */
    public EventArticleRecommendationService() {
        this(new HybridSearchService()::search,
                ContentPreferencesService.getInstance()::getPreferences);
    }

    /** Testing constructor. */
    EventArticleRecommendationService(QuerySearch querySearch) {
        this(querySearch, ContentPreferences::empty);
    }

    EventArticleRecommendationService(QuerySearch querySearch, Supplier<ContentPreferences> preferencesSupplier) {
        this.querySearch = querySearch;
        this.preferencesSupplier = preferencesSupplier;
    }

    /**
     * Returns up to {@code maxResults} recommendations for an event.
     * Returns at least one result when matching candidates exist.
     */
    public List<Recommendation> recommendForEvent(CalendarEvent event, int maxResults) {
        if (event == null || maxResults <= 0) {
            return Collections.emptyList();
        }

        int limit = Math.min(maxResults, 3);

        String rawTitle = safe(event.getName());
        String rawDescription = safe(event.getDescription());
        ContentPreferences preferences = preferencesSupplier.get();

        String titleQuery = expandEventTerms(rawTitle);
        String descriptionQuery = expandEventTerms(rawDescription);

        if (titleQuery.isBlank() && descriptionQuery.isBlank()) {
            return Collections.emptyList();
        }

        List<SearchResult> titleResults = titleQuery.isBlank()
                ? List.of()
                : ArticlePersonalizationService.personalizeResults(
                        querySearch.search(titleQuery, 0.0),
                        titleQuery,
                        preferences);
        List<SearchResult> descriptionResults = descriptionQuery.isBlank()
                ? List.of()
                : ArticlePersonalizationService.personalizeResults(
                        querySearch.search(descriptionQuery, 0.0),
                        descriptionQuery,
                        preferences);

        if (titleResults.isEmpty() && descriptionResults.isEmpty()) {
            return Collections.emptyList();
        }

        double titleTop = topScore(titleResults);
        double descriptionTop = topScore(descriptionResults);

        String eventText = (rawTitle + " " + rawDescription).trim();
        Set<String> eventTokens = new HashSet<>(
                TextPreprocessor.tokenize(TextPreprocessor.normalize(eventText)));

        Set<Article> allArticles = new LinkedHashSet<>();
        titleResults.forEach(result -> allArticles.add(result.article()));
        descriptionResults.forEach(result -> allArticles.add(result.article()));

        Map<Article, Double> titleScoreMap = toScoreMap(titleResults);
        Map<Article, Double> descriptionScoreMap = toScoreMap(descriptionResults);

        List<Recommendation> ranked = new ArrayList<>();
        for (Article article : allArticles) {
            double titleScore = normalizeToTop(titleScoreMap.getOrDefault(article, 0.0), titleTop);
            double descriptionScore = normalizeToTop(descriptionScoreMap.getOrDefault(article, 0.0), descriptionTop);
            double weightedScore = TITLE_WEIGHT * titleScore + DESCRIPTION_WEIGHT * descriptionScore;
            double tagBoost = computeTagBoost(eventTokens, article.getTags());
            ranked.add(new Recommendation(article, weightedScore + tagBoost));
        }

        ranked.sort(Comparator.comparingDouble(Recommendation::score).reversed());

        if (!ranked.isEmpty()) {
            List<Recommendation> strict = new ArrayList<>();
            Recommendation top = ranked.get(0);

            if (top.score() >= RELEVANCE_THRESHOLD) {
                strict.add(top);
                for (int i = 1; i < ranked.size() && strict.size() < limit; i++) {
                    Recommendation candidate = ranked.get(i);
                    boolean strongAbsolute = candidate.score() >= ADDITIONAL_RESULT_MIN_SCORE;
                    boolean closeToTop = candidate.score() >= top.score() * ADDITIONAL_RESULT_TOP_RATIO;
                    if (strongAbsolute && closeToTop) {
                        strict.add(candidate);
                    }
                }
                if (!strict.isEmpty()) {
                    return strict;
                }
            }
        }

        return ranked.stream().limit(1).toList();
    }

    private static Map<Article, Double> toScoreMap(List<SearchResult> results) {
        Map<Article, Double> map = new HashMap<>();
        for (SearchResult result : results) {
            map.merge(result.article(), result.score(), Math::max);
        }
        return map;
    }

    private static double topScore(List<SearchResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        return results.get(0).score();
    }

    private static double normalizeToTop(double value, double top) {
        if (value <= 0.0 || top <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value / top);
    }

    private static double computeTagBoost(Set<String> eventTokens, List<String> tags) {
        if (eventTokens.isEmpty() || tags == null || tags.isEmpty()) {
            return 0.0;
        }

        Set<String> tagTokens = new HashSet<>();
        for (String tag : tags) {
            tagTokens.addAll(TextPreprocessor.tokenize(TextPreprocessor.normalize(safe(tag))));
        }

        if (tagTokens.isEmpty()) {
            return 0.0;
        }

        long overlap = eventTokens.stream().filter(tagTokens::contains).count();
        if (overlap == 0) {
            return 0.0;
        }

        double ratio = (double) overlap / (double) Math.max(1, eventTokens.size());
        return Math.min(TAG_BOOST_CAP, ratio * TAG_BOOST_CAP);
    }

    private static String expandEventTerms(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String base = text.trim();
        String normalized = normalizeForMatching(base);
        Set<String> expansions = new LinkedHashSet<>();

        // HIV / PrEP / PEP corpus vocabulary
        if (containsAnyTerm(normalized,
                "hiv", "prep", "pep", "u=u", "undetectable", "viral load",
                "exposure", "post exposure", "antiretroviral")) {
            addTerms(expansions,
                    "hiv",
                    "prep",
                    "pep",
                    "post exposure prophylaxis",
                    "antiretroviral",
                    "undetectable untransmittable",
                    "viral load",
                    "rapid hiv testing");
        }

        // STI testing and clinic flow
        if (containsAnyTerm(normalized,
                "sti", "std", "test", "testing", "screen", "screening", "swab",
                "urine sample", "blood sample", "home kit", "test kit", "re-test", "retest", "clinic")) {
            addTerms(expansions,
                    "sexual health clinic",
                    "sti testing",
                    "home sti test kit",
                    "chlamydia test",
                    "gonorrhoea test",
                    "syphilis test",
                    "trichomoniasis test");
        }

        // Contraception and emergency contraception
        if (containsAnyTerm(normalized,
                "contraception", "condom", "pill", "mini pill", "combined pill", "patch",
                "ring", "implant", "nexplanon", "coil", "iud", "ius", "missed pill",
                "unprotected", "morning after", "emergency")) {
            addTerms(expansions,
                    "contraception",
                    "emergency contraception",
                    "morning after pill",
                    "combined pill",
                    "progestogen-only pill",
                    "copper coil",
                    "hormonal coil",
                    "iud",
                    "ius",
                    "implant",
                    "condom failure",
                    "free condoms");
        }

        // Cervical screening and HPV
        if (containsAnyTerm(normalized,
                "smear", "cervical", "hpv", "cervix", "speculum", "abnormal cells")) {
            addTerms(expansions,
                    "cervical screening",
                    "smear test",
                    "human papillomavirus",
                    "hpv",
                    "cervix",
                    "speculum",
                    "abnormal cells");
        }

        // Vaginal/urinary symptom pathways
        if (containsAnyTerm(normalized,
                "discharge", "itch", "itching", "fishy", "odour", "odor", "burning",
                "painful urination", "pee", "wee", "thrush", "bv", "vaginitis",
                "urethritis", "cystitis", "sore", "painful sex")) {
            addTerms(expansions,
                    "vaginal discharge",
                    "thrush candidiasis",
                    "bacterial vaginosis bv",
                    "vaginitis",
                    "urethritis",
                    "cystitis",
                    "sti symptoms",
                    "painful urination");
        }

        // Specific STI terms used heavily in article corpus
        if (containsAnyTerm(normalized,
                "chlamydia", "gonorrhoea", "syphilis", "herpes", "trichomoniasis",
                "mycoplasma", "mg", "pubic lice", "crabs")) {
            addTerms(expansions,
                    "chlamydia",
                    "gonorrhoea",
                    "syphilis",
                    "genital herpes",
                    "trichomoniasis",
                    "mycoplasma genitalium",
                    "urethritis",
                    "pubic lice");
        }

        // Consent, boundaries, and relationship safety
        if (containsAnyTerm(normalized,
                "consent", "boundaries", "pressure", "pressured", "assault", "rape",
                "abuse", "abusive", "coercive", "harassment", "gaslighting", "sarc")) {
            addTerms(expansions,
                    "consent",
                    "relationship boundaries",
                    "healthy communication",
                    "sexual assault support",
                    "sarc",
                    "domestic abuse",
                    "coercive control");
        }

        // Mental health and sexual wellbeing links in corpus
        if (containsAnyTerm(normalized,
                "anxiety", "panic", "depression", "mood", "counselling", "therapy",
                "antidepressant", "mental health", "libido", "sex drive", "wellbeing")) {
            addTerms(expansions,
                    "mental health",
                    "anxiety",
                    "depression",
                    "counselling",
                    "antidepressants",
                    "libido",
                    "sexual wellbeing");
        }

        // LGBTQ+ identity and support vocabulary
        if (containsAnyTerm(normalized,
                "lgbt", "lgbtq", "queer", "trans", "non-binary", "non binary",
                "coming out", "sexuality", "orientation", "pronouns")) {
            addTerms(expansions,
                    "lgbtq",
                    "coming out",
                    "gender identity",
                    "sexual orientation",
                    "trans and non-binary support");
        }

        // Cycles and menopause
        if (containsAnyTerm(normalized,
                "period", "heavy bleeding", "pcos", "endometriosis", "menopause",
                "perimenopause", "vaginal dryness", "pmdd", "pms")) {
            addTerms(expansions,
                    "period health",
                    "heavy and painful periods",
                    "pcos",
                    "endometriosis",
                    "menopause",
                    "perimenopause",
                    "vaginal dryness",
                    "hormone replacement therapy");
        }

        if (expansions.isEmpty()) {
            return base;
        }

        return base + " " + String.join(" ", expansions);
    }

    private static String normalizeForMatching(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String normalized = lower.replaceAll("[^a-z0-9+]+", " ").trim();
        return " " + normalized + " ";
    }

    private static boolean containsAnyTerm(String normalized, String... terms) {
        for (String term : terms) {
            if (containsTerm(normalized, term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTerm(String normalized, String term) {
        String normalizedTerm = term.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+]+", " ")
                .trim();
        if (normalizedTerm.isEmpty()) {
            return false;
        }
        return normalized.contains(" " + normalizedTerm + " ");
    }

    private static void addTerms(Set<String> target, String... terms) {
        Collections.addAll(target, terms);
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}
