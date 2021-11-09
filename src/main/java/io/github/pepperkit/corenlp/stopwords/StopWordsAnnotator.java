/*
 * Copyright (C) 2021 PepperKit
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
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
import java.util.stream.Stream;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;

/**
 * Annotator for CoreNLP library, allows to add the set of rules or/and the word themselves, which should be filtered
 * out in the CoreNLP pipeline processing.
 */
public class StopWordsAnnotator implements Annotator, CoreAnnotation<Boolean> {

    /** This name is used to identify the annotator. */
    public static final String ANNOTATOR_NAME = "stopwords";

    private static final String STOP_POS_CATEGORIES = "withPosCategories";
    private static final String STOP_ALL_WORDS_SHORTER_THAN = "shorterThan";
    private static final String STOP_ALL_LEMMAS_SHORTER_THAN = "withLemmasShorterThan";

    private static final String STOP_WORDS_FILE_PATH = "customListFilePath";
    private static final String STOP_WORDS_RESOURCES_FILE_PATH = "customListResourcesFilePath";
    private static final String STOP_WORDS_LIST = "customList";

    Set<String> stopwords;
    Set<String> stopPosCategories = new HashSet<>();
    int minimumWordLength = 0;
    int minimumLemmaLength = 0;

    /**
     * Constructs a new StopWordsAnnotator with the specified annotator's name and properties
     * ({@link StopWordsAnnotator#StopWordsAnnotator(Properties)}).
     * @param name annotator's name
     * @param props properties for the annotator
     * @throws IOException if the list of stop words is specified in a file and the file cannot be read
     */
    public StopWordsAnnotator(String name, Properties props) throws IOException {
        this(props);
    }

    /**
     * Constructs a new StopWordsAnnotator with the specified properties.
     * Accepted properties are:
     * <ul>
     *     <li>provided list of particular words (and/or its lemmas) using a string containing comma-separated words, or
     *     a file with newline-separated words (from any place in the file system or from a bundled resource) -
     *     `stopwords.customList`, `stopwords.customListFilePath`, and `stopwords.customListResourcesFilePath`
     *     properties (if all of the properties are provided, only one list of words will be initialized from a provided
     *     property, the order of precedence: string with words, from a file, from a bundled resource);</li>
     *     <li>POS (part-of-speech) categories as a string containing a comma-separated list of the categories -
     *     `stopwords.withPosCategories` property;</li>
     *     <li>the length of a word or its lemma - `stopwords.shorterThan` and `stopwords.withLemmasShorterThan`
     *     properties.</li>
     * </ul>
     * @param props properties for the annotator
     * @throws IOException if the list of stop words is specified in a file and the file cannot be read
     */
    public StopWordsAnnotator(Properties props) throws IOException {
        if (props.containsKey(globalPropertyName(STOP_POS_CATEGORIES))) {
            final String[] posCategories = props.getProperty(globalPropertyName(STOP_POS_CATEGORIES)).split(",");
            this.stopPosCategories = Arrays.stream(posCategories)
                    .map(pc -> pc.trim().toUpperCase())
                    .collect(Collectors.toSet());
        }

        if (props.containsKey(globalPropertyName(STOP_ALL_WORDS_SHORTER_THAN))) {
            this.minimumWordLength = Integer.parseInt(
                    props.getProperty(globalPropertyName(STOP_ALL_WORDS_SHORTER_THAN)));
        }

        if (props.containsKey(globalPropertyName(STOP_ALL_LEMMAS_SHORTER_THAN))) {
            this.minimumLemmaLength = Integer.parseInt(
                    props.getProperty(globalPropertyName(STOP_ALL_LEMMAS_SHORTER_THAN)));
        }

        this.initializeStopWordsList(props);
    }

    private static String globalPropertyName(String privatePropertyName) {
        return ANNOTATOR_NAME + "." + privatePropertyName;
    }

    private void initializeStopWordsList(Properties props) throws IOException {
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
            final String resourcePath = props.getProperty(globalPropertyName(STOP_WORDS_RESOURCES_FILE_PATH));
            this.stopwords = loadStopWordsFromResource(resourcePath);
        }
    }

    private Set<String> loadStopWordsFromFile(String filePathStr) throws IOException {
        Path filePath = Paths.get(filePathStr);
        try (Stream<String> s = Files.lines(filePath)) {
            return s.map(String::toLowerCase).collect(Collectors.toSet());
        }
    }

    private Set<String> loadStopWordsFromResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.toSet());
            } else {
                throw new IOException("Cannot read stop words resources file: " + resourcePath);
            }
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        if (stopwords != null && stopwords.size() > 0) {
            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                token.set(StopWordsAnnotator.class,
                        token.word().length() < minimumWordLength ||
                                token.lemma().length() < minimumLemmaLength ||
                                stopPosCategories.contains(token.tag()) ||
                                checkWordAndOrLemma(token));
            }
        }
    }

    private boolean checkWordAndOrLemma(CoreLabel token) {
            return stopwords.contains(token.lemma().toLowerCase()) || stopwords.contains(token.word().toLowerCase());
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
