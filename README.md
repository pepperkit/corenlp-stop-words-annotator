# CoreNLP Stop Words Annotator

![StopWordsAnnotator](https://img.shields.io/badge/CoreNLP%20Compatible-v4.3-blue)
[![Java CI with Maven](https://github.com/pepperkit/corenlp-stop-words-annotator/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/pepperkit/corenlp-stop-words-annotator/actions/workflows/maven.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pepperkit_corenlp-stop-words-annotator&metric=coverage)](https://sonarcloud.io/dashboard?id=pepperkit_corenlp-stop-words-annotator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=pepperkit_corenlp-stop-words-annotator&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=pepperkit_corenlp-stop-words-annotator)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=pepperkit_corenlp-stop-words-annotator&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=pepperkit_corenlp-stop-words-annotator)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=pepperkit_corenlp-stop-words-annotator&metric=security_rating)](https://sonarcloud.io/dashboard?id=pepperkit_corenlp-stop-words-annotator)

Annotator for CoreNLP library, allows adding the set of rules or/and the word themselves, which should be filtered out in the
CoreNLP pipeline processing.

## Usage
Just add the annotator and CoreNLP library with models into the dependencies list like this:
```xml
        <dependency>
            <groupId>io.github.pepperkit</groupId>
            <artifactId>corenlp-stop-words-annotator</artifactId>
            <version>1.0.0</version>
        </dependency>

        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>4.2.2</version>
        </dependency>
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>4.2.2</version>
            <classifier>models</classifier>
        </dependency>
```

The annotator is configured with `Properties`, it marks the words as stopped using one of the following rules:
- provided list of particular words (and/or its lemmas) using a string containing comma-separated words, or a file with newline-separated 
  words (from any place in the file system or from a bundled resource) - `stopwords.customList`, `stopwords.customListFilePath`, 
  and `stopwords.customListResourcesFilePath` properties (if all of the properties are provided, only one list of words
  will be initialized from a provided property, the order of precedence: string with words, from a file, from a bundled resource);
- POS (part-of-speech) categories (of words lemmas) as a string containing a comma-separated list of the categories - `stopwords.withPosCategories` property;
- the length of a word or its lemma - `stopwords.shorterThan` and `stopwords.withLemmasShorterThan` properties.

Description of the available POS categories can be found here (also see complex example below):
 - https://nlp.stanford.edu/software/pos-tagger-faq.html
 - https://catalog.ldc.upenn.edu/docs/LDC99T42/tagguid1.pdf

### Requirements
- Java version should be 8 or higher;
- annotator should be added at the project's POM as a dependency;
- CoreNLP library should be present in the classpath;
- *tokenize*, *ssplit*, *pos*, and *lemma* annotators should be present in the pipeline before *stopwords* annotator.

### Simple Example
If we just want to filter out the words from a list of stop words, we can easily do it like following:
```java
class Example {
    public Set<String> getInterestingWords() {
        final String text = "Once upon a time there was a dear little girl who was loved by everyone who looked at her";
        
        final Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, stopwords");
        props.setProperty("customAnnotatorClass.stopwords", "io.github.pepperkit.corenlp.stopwords.StopWordsAnnotator");
        props.setProperty("ssplit.isOneSentence", "true");
        
        // Filter out these words
        props.setProperty("stopwords.customList", "once,upon,a,little,girl");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        Set<String> result = new HashSet<>();
        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);

        for (CoreLabel token : tokens) {
            // token.get(StopWordsAnnotator.class) will be TRUE if the word is stopped
            if (!token.get(StopWordsAnnotator.class)) {
                result.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return result;
    }
}
```

### Complex Example
Let's use *stopwords* annotator for a particular complex scenario when we need to process a text and extract a set of lemmas of only 
"interesting" words, where the word is considered "interesting", if it is not a common word (more detailed definition is further in the text).

**Scenario**:

*Given* I have the text  
  *And* the stop words are defined in the resources file (containing the most common English words)  
*When* I launch text processing using StanfordCoreNLP pipeline with StopWordsAnnotator  
  *And* set it to mark words as stopped if it is shorter than 3 letters (to remove all the punctuation and simple words like be, so, etc.)  
  *And* is of POS category I am not interested in  
  *And* is in the list of stop words I provided in the resources file  
*Then* I should be able to filter out the common words from the text  

```java
class Example {
    public Set<String> getInterestingWords() {
        // I have the text:
        final String text = "Once upon a time there was a dear little girl who was loved by everyone who looked at " +
                "her, but most of all by her grandmother, and there was nothing that she would not have given to the " +
                "child. Once she gave her a little riding hood of red velvet, which suited her so well that she would" +
                " never wear anything else; so she was always called 'Little Red Riding Hood.'";

        // I want to get the list of lemmas created from the text, excluding words from the provided list and all the
        // common or simple words (like propositions, conjunctions, etc.), since I want to extract only the words
        // I could be interested to learn
        String[] expectedWords = {"dear", "look", "have", "give", "riding", "hood", "velvet", "suit", "wear", "call"};

        // And the stop words in resources (containing the most known English words)
        final String stopWordsResourcePath = "common-words-list-it.txt";

        final Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, stopwords");
        props.setProperty("customAnnotatorClass.stopwords", "io.github.pepperkit.corenlp.stopwords.StopWordsAnnotator");
        props.setProperty("ssplit.isOneSentence", "true");

        // to filter out all the punctuation and simple words like be, so, etc.
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

        // Return only lemmas of only interesting words
        for (CoreLabel token : tokens) {
            if (!token.get(StopWordsAnnotator.class)) {
                result.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return result;
    }
}
```

## Project's structure
```
└── src
    ├── main                # code of the annotator
    ├── test                # unit tests
    └── integration-test    # integration tests
```
