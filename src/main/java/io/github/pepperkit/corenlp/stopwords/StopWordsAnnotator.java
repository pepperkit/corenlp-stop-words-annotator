package io.github.pepperkit.corenlp.stopwords;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;

/**
 * Stop words CoreNLP Annotator.
 * Uses several settings and chosen or specified list of stop words in text processing.
 */
public class StopWordsAnnotator implements Annotator, CoreAnnotation<Boolean> {

    // private static final Logger logger = LoggerFactory.getLogger(StopWordsAnnotator.class);

    /**
     *
     */
    public static final String ANNOTATOR_NAME = "stopwords";

    private static final String CHECK_ONLY_LEMMAS_PROPERTY = "checkOnlyLemmas";
    private static final String STOP_POS_CATEGORIES = "stopPosCategories";
    private static final String STOP_ALL_WORDS_SHORTER_THAN = "stopAllWordsShorterThan";
    private static final String STOP_ALL_LEMMAS_SHORTER_THAN = "stopAllLemmasShorterThan";
    private static final String STOP_WORDS_FILE_PATH = "customListFilePath";
    private static final String STOP_WORDS_RESOURCES_FILE_PATH = "customListResourcesFilePath";
    private static final String STOP_WORDS_LIST = "customList";

    Set<String> stopwords;
    final boolean checkOnlyLemmas;
    final Set<String> stopPosCategories;
    int maximumWordLength = 0;
    int maximumLemmaLength = 0;

    public StopWordsAnnotator(String name, Properties props) throws IOException {
        this(props);
    }

    /**
     *
     * @param props
     * @throws IOException
     */
    public StopWordsAnnotator(Properties props) throws IOException {
        this.checkOnlyLemmas = Boolean.parseBoolean(
                props.getProperty(globalPropertyName(CHECK_ONLY_LEMMAS_PROPERTY), "true"));

        if (props.containsKey(globalPropertyName(STOP_POS_CATEGORIES))) {
            final String[] posCategories = props.getProperty(globalPropertyName(STOP_POS_CATEGORIES)).split(",");
            this.stopPosCategories = Arrays.stream(posCategories)
                    .collect(Collectors.toSet());
        } else {
            this.stopPosCategories = new HashSet<>(Arrays.asList(
                    "IN", "RP", "UH", "MD", "PDT", "POS", "PRP", "SYM", "WDT", "WP", "WRB", "NNP"));
        }

        if (props.containsKey(globalPropertyName(STOP_ALL_WORDS_SHORTER_THAN))) {
            this.maximumWordLength = Integer.parseInt(
                    props.getProperty(globalPropertyName(STOP_ALL_WORDS_SHORTER_THAN)));
        }

        if (props.containsKey(globalPropertyName(STOP_ALL_LEMMAS_SHORTER_THAN))) {
            this.maximumLemmaLength = Integer.parseInt(
                    props.getProperty(globalPropertyName(STOP_ALL_LEMMAS_SHORTER_THAN)));
        }

        if (props.containsKey(globalPropertyName(STOP_WORDS_LIST))) {
            final String stopWordsListString = props.getProperty(globalPropertyName(STOP_WORDS_LIST));
            this.stopwords = Arrays.stream(stopWordsListString
                    .split(","))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

        } else if (props.containsKey(globalPropertyName(STOP_WORDS_FILE_PATH))) {
            final String filePath = props.getProperty(globalPropertyName(STOP_WORDS_FILE_PATH));
            this.stopwords = loadStopWordsFromFile(filePath);

        } else if (props.containsKey(globalPropertyName(STOP_WORDS_RESOURCES_FILE_PATH))) {
            final String filePath = props.getProperty(globalPropertyName(STOP_WORDS_RESOURCES_FILE_PATH));
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
                if (inputStream != null) {
                    this.stopwords = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.toSet());
                } else {
                    throw new IOException("Cannot read stop words resources file: " + filePath);
                }
            }
        }

        // this.stopwords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
    }

    private static String globalPropertyName(String privatePropertyName) {
        return ANNOTATOR_NAME + "." + privatePropertyName;
    }

    private static Set<String> loadStopWordsFromFile(String filePathStr) throws IOException {
        Path filePath = Paths.get(filePathStr);
        return Files.lines(filePath)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public void annotate(Annotation annotation) {
        if (stopwords != null && stopwords.size() > 0) {
            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                token.set(StopWordsAnnotator.class,
                        token.word().length() < maximumWordLength ||
                                token.lemma().length() < maximumLemmaLength ||
                                stopPosCategories.contains(token.tag()) ||
                                checkWordAndOrLemma(token));
            }
        }
    }

    private boolean checkWordAndOrLemma(CoreLabel token) {
        if (checkOnlyLemmas) {
            return stopwords.contains(token.lemma().toLowerCase());
        } else {
            return stopwords.contains(token.word().toLowerCase()) ||
                    stopwords.contains(token.lemma().toLowerCase());
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class,
                CoreAnnotations.LemmaAnnotation.class
        )));
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(StopWordsAnnotator.class);
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }
}
