package com.pmsconnect.mage.casestudy;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TermDetect {
    SimpleTokenizer tokenizer;
    InputStream inputStreamPOSTagger;
    POSModel posModel;
    POSTaggerME posTagger;
    InputStream dictLemmatizer;
    DictionaryLemmatizer lemmatizer;

    public TermDetect() {
        try {
            tokenizer = SimpleTokenizer.INSTANCE;
            inputStreamPOSTagger = Files.newInputStream(Paths.get("/opennlp/models/en-pos-maxent.bin"));
            posModel = new POSModel(inputStreamPOSTagger);
            posTagger = new POSTaggerME(posModel);
            dictLemmatizer = Files.newInputStream(Paths.get("/opennlp/models/en-lemmatizer.dict"));
            lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> detectLemmaEnglish(String sentence) {
        String[] tokens = tokenizer.tokenize(sentence);
        String[] tags = posTagger.tag(tokens);
        String[] lemmas = lemmatizer.lemmatize(tokens, tags);

        Map<String, String> tokenTag = new HashMap<>();
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].contains("NN"))
                tokenTag.put(tokens[i].toLowerCase(), "noun");
            else if (tags[i].contains("VB"))
                tokenTag.put(lemmas[i], "verb");
        }
        return tokenTag;
    }
}
