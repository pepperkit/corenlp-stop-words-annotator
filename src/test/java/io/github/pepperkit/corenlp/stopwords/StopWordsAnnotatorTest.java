package io.github.pepperkit.corenlp.stopwords;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StopWordsAnnotatorTest {
    @Test
    public void createCheckOnlyLemmasPropIsSetUpIfProvided() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.checkOnlyLemmas", "false");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertFalse(annotator.checkOnlyLemmas);
    }

    @Test
    public void createCheckOnlyLemmasPropIsDefaultTrueIfNotProvided() throws IOException {
        final Properties props = new Properties();
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertTrue(annotator.checkOnlyLemmas);
    }

    @Test
    public void stopPosCategoriesPropIsSetUpIfProvided() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.withPosCategories", "IN,WDT");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopPosCategories).containsExactlyInAnyOrder("IN", "WDT");
    }

    @Test
    public void stopPosCategoriesPropIsSetUpIfProvidedWithSpaces() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.withPosCategories", " IN, WDT");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopPosCategories).containsExactlyInAnyOrder("IN", "WDT");
    }

    @Test
    public void stopPosCategoriesPropIsSetUpIfProvidedInLowerCase() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.withPosCategories", "in,Wdt");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopPosCategories).containsExactlyInAnyOrder("IN", "WDT");
    }

    @Test
    public void stopPosCategoriesPropIsCorrectDefaultValueIfNotProvided() throws IOException {
        final Properties props = new Properties();
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopPosCategories).isEmpty();
    }

    @Test
    public void stopAllWordsShorterThanPropIsSetUpIfProvided() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.shorterThan", "3");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertEquals(3, annotator.minimumWordLength);
    }

    @Test
    public void stopAllWordsShorterThanPropIsDefaultZeroIfNotProvided() throws IOException {
        final Properties props = new Properties();
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertEquals(0, annotator.minimumWordLength);
    }

    @Test
    public void stopAllLemmasShorterThanPropIsSetUpIfProvided() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.withLemmasShorterThan", "3");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertEquals(3, annotator.minimumLemmaLength);
    }

    @Test
    public void stopAllLemmasShorterThanPropIsDefaultZeroIfNotProvided() throws IOException {
        final Properties props = new Properties();
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertEquals(0, annotator.minimumLemmaLength);
    }

    @Test
    public void stopWordsAreSetIfWordsListProvided() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.customList", "stop,words,list");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopwords).containsExactlyInAnyOrder("stop", "words", "list");
    }

    @Test
    public void stopWordsAreSetIfWordsListProvidedAsResourceFile() throws IOException {
        final Properties props = new Properties();
        props.put("stopwords.customListResourcesFilePath", "stop-words-list-test.txt");
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopwords).containsExactlyInAnyOrder("stop", "words", "list", "in", "file");
    }

    @Test
    public void stopWordsAreSetIfWordsListProvidedAsFilePath() throws IOException, URISyntaxException {
        URL res = getClass().getClassLoader().getResource("stop-words-list-test.txt");
        File file = Paths.get(res.toURI()).toFile();
        final String stopWordsFilePath = file.getAbsolutePath();

        final Properties props = new Properties();
        props.put("stopwords.customListFilePath", stopWordsFilePath);
        StopWordsAnnotator annotator = new StopWordsAnnotator(props);

        assertThat(annotator.stopwords).containsExactlyInAnyOrder("stop", "words", "list", "in", "file");
    }

    @Test
    public void wordIsStoppedIfPresentInStopList() throws IOException {
        StopWordsAnnotator annotator = new StopWordsAnnotator(new Properties());
        annotator.checkOnlyLemmas = false;
        annotator.stopwords = new HashSet<>(Collections.singletonList("words"));

        Annotation annotation = mock(Annotation.class);
        CoreLabel wordToStop = mockWordToStopToken();
        CoreLabel regularWord = mockRegularWordToken();
        when(annotation.get(any())).thenReturn(Arrays.asList(wordToStop, regularWord));

        annotator.annotate(annotation);
        verify(wordToStop, times(1)).set(StopWordsAnnotator.class, true);
        verify(regularWord, times(1)).set(StopWordsAnnotator.class, false);
    }

    @Test
    public void wordIsNotStoppedIfPresentInStopListAndOnlyLemmasAreChecked() throws IOException {
        StopWordsAnnotator annotator = new StopWordsAnnotator(new Properties());
        annotator.checkOnlyLemmas = true;
        annotator.stopwords = new HashSet<>(Collections.singletonList("words"));

        Annotation annotation = mock(Annotation.class);
        CoreLabel wordToStop = mockWordToStopToken();
        CoreLabel regularWord = mockRegularWordToken();
        when(annotation.get(any())).thenReturn(Arrays.asList(wordToStop, regularWord));

        annotator.annotate(annotation);
        verify(wordToStop, times(1)).set(StopWordsAnnotator.class, false);
        verify(regularWord, times(1)).set(StopWordsAnnotator.class, false);
    }

    @Test
    public void wordIsStoppedIfItHasLenghtLessThenAllowed() throws IOException {
        StopWordsAnnotator annotator = new StopWordsAnnotator(new Properties());
        annotator.stopwords = new HashSet<>(Collections.singletonList("something"));
        annotator.minimumWordLength = 6;

        Annotation annotation = mock(Annotation.class);
        CoreLabel wordToStop = mockWordToStopToken();
        CoreLabel regularWord = mockRegularWordToken();
        when(annotation.get(any())).thenReturn(Arrays.asList(wordToStop, regularWord));

        annotator.annotate(annotation);
        verify(wordToStop, times(1)).set(StopWordsAnnotator.class, true);
        verify(regularWord, times(1)).set(StopWordsAnnotator.class, false);
    }

    @Test
    public void wordIsStoppedIfItHasLemmaLenghtLessThenAllowed() throws IOException {
        StopWordsAnnotator annotator = new StopWordsAnnotator(new Properties());
        annotator.stopwords = new HashSet<>(Collections.singletonList("something"));
        annotator.minimumLemmaLength = 5;

        Annotation annotation = mock(Annotation.class);
        CoreLabel wordToStop = mockWordToStopToken();
        CoreLabel regularWord = mockRegularWordToken();
        when(annotation.get(any())).thenReturn(Arrays.asList(wordToStop, regularWord));

        annotator.annotate(annotation);
        verify(wordToStop, times(1)).set(StopWordsAnnotator.class, true);
        verify(regularWord, times(1)).set(StopWordsAnnotator.class, false);
    }

    @Test
    public void wordIsStoppedIfItHasStoppedPosCategory() throws IOException {
        StopWordsAnnotator annotator = new StopWordsAnnotator(new Properties());
        annotator.stopwords = new HashSet<>(Collections.singletonList("something"));
        annotator.stopPosCategories = new HashSet<>(Collections.singletonList("NONE"));

        Annotation annotation = mock(Annotation.class);
        CoreLabel wordToStop = mockWordToStopToken();
        CoreLabel regularWord = mockRegularWordToken();
        when(annotation.get(any())).thenReturn(Arrays.asList(wordToStop, regularWord));

        annotator.annotate(annotation);
        verify(wordToStop, times(1)).set(StopWordsAnnotator.class, true);
        verify(regularWord, times(1)).set(StopWordsAnnotator.class, false);
    }

    private CoreLabel mockWordToStopToken() {
        CoreLabel wordToStop = mock(CoreLabel.class);
        when(wordToStop.word()).thenReturn("words");
        when(wordToStop.lemma()).thenReturn("word");
        when(wordToStop.tag()).thenReturn("NONE");
        return wordToStop;
    }

    private CoreLabel mockRegularWordToken() {
        CoreLabel word = mock(CoreLabel.class);
        when(word.word()).thenReturn("justaword");
        when(word.lemma()).thenReturn("justaword");
        when(word.tag()).thenReturn("NONE2");
        return word;
    }
}
