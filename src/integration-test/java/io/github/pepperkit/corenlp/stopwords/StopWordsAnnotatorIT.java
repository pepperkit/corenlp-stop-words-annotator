package io.github.pepperkit.corenlp.stopwords;

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopWordsAnnotatorIT {
    /**
     * Scenario: The common words are filtered out from the text based on StopWordsAnnotator work
     *     Given I have the text
     *     And the stop words are defined in the resources file (containing the most common english words)
     *     When I launch text processing using StanfordCoreNLP pipeline with StopWordsAnnotator
     *     And set it to mark words as stopped if it is shorter than 3 letters (to remove all the punctuation and simple words like be, so etc.)
     *     And is of POS category I am not interested in
     *     And is in the list of stop words I provided in the file
     *     Then I should be able to filter out the common words from the text
     */
    @Test
    public void annotatorWorksCorrectly() {
        // I have the text:
        final String text = "Once upon a time there was a dear little girl who was loved by everyone who looked at " +
                "her, but most of all by her grandmother, and there was nothing that she would not have given to the " +
                "child. Once she gave her a little riding hood of red velvet, which suited her so well that she would" +
                " never wear anything else; so she was always called 'Little Red Riding Hood.'";

        // I want to get the list of lemmas created from the text, excluding words from the provided list and all the
        // common or simple words (like propositions, conjunctions etc.), since I want to extract only the words
        // I could be interested to learn
        String[] expectedWords = {"dear", "look", "have", "give", "riding", "hood", "velvet", "suit", "wear", "call"};

        // And the stop words in resources (containing the most known english words)
        final String stopWordsResourcePath = "common-words-list-it.txt";

        final Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, stopwords");
        props.setProperty("customAnnotatorClass.stopwords", "io.github.pepperkit.corenlp.stopwords.StopWordsAnnotator");
        props.setProperty("ssplit.isOneSentence", "true");

        // to filter out all the punctuation and simple words like be, so etc.
        props.setProperty("stopwords.withLemmasShorterThan", "3");

        // to filter out all the common and simple words
        // Description of the available POS categories can be found here:
        // - https://nlp.stanford.edu/software/pos-tagger-faq.html
        // - https://catalog.ldc.upenn.edu/docs/LDC99T42/tagguid1.pdf
        props.setProperty("stopwords.withPosCategories",
                "NNP,NNPS," + // proper noun singular and plural
                        "PDT," + // predeterminer
                        "IN,CC," + // conjunction and coordinating conjunction (but, and etc.)
                        "DT," + // determiner - the, a, etc.
                        "UH," + // interjection - my, his, oh, uh etc.
                        "FW," + // foreign word
                        "MD," + // modal verb
                        "RP," + // particle
                        "PRP,PRP$," + // personal pronoun
                        "EX," + // existential there
                        "POS," + // possessive ending: 's
                        "SYM," + // symbol
                        "WDT,WP,WP$," + // wh-determiner (who), wh-pronoun (who, what, whom) and possessive wh-pronoun (whose)
                        "WRB" // wh-adverb
        );

        // provide the file with stop words list
        props.setProperty("stopwords.customListResourcesFilePath", stopWordsResourcePath);

        // Annotate the text using StanfordCoreNLP pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        // Process returned tokens
        Set<String> result = new HashSet<>();
        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);

        // Return only lemmas of words
        for (CoreLabel token : tokens) {
            if (!token.get(StopWordsAnnotator.class)) {
                result.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }

        assertThat(result).containsExactlyInAnyOrder(expectedWords);
    }
}
