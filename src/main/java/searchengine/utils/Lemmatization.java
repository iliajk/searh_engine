package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.Morphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

@Slf4j
public class Lemmatization {

    private static final String RUSSIAN = "[а-яА-Я-]{2,}";
    private static final String ENGLISH = "[a-zA-Z-]{2,}";
    private static final String UNNECESSARYSYMBOLS = "[^А-Яа-яa-zA-Z’\\- ]";
    public static Map<String, Integer> countLemma(String text) {
        HashMap<String, Integer> response = new HashMap<>();
        if (text.isEmpty())
            return response;
        text = text.trim();
        text = text.replaceAll(UNNECESSARYSYMBOLS, "");
        List<String> arrayWords = Arrays.stream(text.split("\\s+")).toList();
        List<String> wordBaseForms = processingToBaseForm(arrayWords);
        wordBaseForms.forEach(s -> {
            if (response.containsKey(s)) {
                Integer count = response.get(s);
                response.put(s, count + 1);
            } else {
                response.put(s,1);
            }
        });

        return response;
    }

    public static String cleanWebPage(String page){
        Document document = Jsoup.parse(page);
        return document.text();
    }

    private static List<String> processingToBaseForm(List<String> arrayWords) {
        List<String> wordBaseForms = new ArrayList<>();

        try {
            LuceneMorphology rusMorphology = new RussianLuceneMorphology();
            LuceneMorphology engMorphology = new EnglishLuceneMorphology();
            arrayWords.forEach(s -> {
                String result = checkLanguage(s);
                if (result.equals("Rus") && wordIsApproved(s.toLowerCase(), rusMorphology)) {
                    wordBaseForms.add(rusMorphology.getNormalForms(s.toLowerCase()).get(0));
                } else if (result.equals("Eng") && wordIsApproved(s.toLowerCase(), engMorphology)) {
                    wordBaseForms.add(engMorphology.getNormalForms(s.toLowerCase()).get(0));
                } else {
                    log.debug("Work is not recognized. Word: " + s + " Message: " + result);
                }
            });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return wordBaseForms;
    }

    private static boolean wordIsApproved(String s, Morphology morphology) {
        String[] particlesNames;

        if (morphology instanceof RussianLuceneMorphology) {
            particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        } else if (morphology instanceof EnglishLuceneMorphology) {
            particlesNames = new String[]{"PART", "CONJ", "ARTICLE", "PREP", "ADJECTIVE", "PN", "VBE", "MOD", "ADVERB"};
        } else {
            log.error("Wrong morphology type. Please check it for work " + s);
            return false;
        }
        List<String> wordInfo = morphology.getMorphInfo(s);
        for (String morphInfo : wordInfo) {
            for (String property : particlesNames) {
                if (morphInfo.toUpperCase().contains(property)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String checkLanguage(String s) {
        return s.matches(RUSSIAN) ? "Rus" :
                s.matches(ENGLISH) ? "Eng" :
                        s.length() == 0 ? "Empty String" : "Different language";
    }
}
