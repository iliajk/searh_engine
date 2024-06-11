package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.Morphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.WebSite;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MorphologyService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static LuceneMorphology rusMorphology, engMorphology;
    private static SaveLemmaClass saveLemmaClass;

    {
        try {
            saveLemmaClass = new SaveLemmaClass();
            engMorphology = new EnglishLuceneMorphology();
            rusMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error("LuceneMorphology problem:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static final String RUSSIAN = "[а-яА-Я-]{2,}";
    private static final String ENGLISH = "[a-zA-Z-]{2,}";
    private static final String UNNECESSARYSYMBOLS = "[^А-Яа-яa-zA-Z’\\- ]";

    public static void processPage(String text, WebSite webSite, String url) {
        Map<String, Integer> countLemma = countLemma(cleanWebPage(text));
        List<Lemma> pageLemmas = new ArrayList<>();
        for (String s : countLemma.keySet()) {
            pageLemmas.add(Lemma.builder()
                    .frequency(1)
                    .lemma(s)
                    .webSite(webSite)
                    .build());
        }
        saveLemmaClass.saveLemmas(webSite, pageLemmas, url);
    }

    private class SaveLemmaClass {
        @Transactional
        private void saveLemmas(WebSite webSite, List<Lemma> pageLemmas, String url) {
            long duration = System.currentTimeMillis();
            List<Lemma> repoLemmas = lemmaRepository.findByWebSite(webSite);
            for (Lemma pageLemma : pageLemmas) {
                if (repoLemmas.contains(pageLemma)) {
                    int idxLemma = repoLemmas.indexOf(pageLemma);
                    Lemma extLemma = repoLemmas.get(idxLemma);
                    extLemma.setFrequency(extLemma.getFrequency() + 1);
                } else {
                    repoLemmas.add(pageLemma);
                }
            }
            lemmaRepository.saveAll(repoLemmas);
            duration = System.currentTimeMillis() - duration;
            log.debug("Lemmas saving finished for link " + url + ". Duration : " + duration);
        }
    }

    private static Map<String, Integer> countLemma(String text) {
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
                response.put(s, 1);
            }
        });

        return response;
    }


    public static String cleanWebPage(String page) {
        Document document = Jsoup.parse(page);
        return document.text();
    }

    private static List<String> processingToBaseForm(List<String> arrayWords) {
        List<String> wordBaseForms = new ArrayList<>();
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

        return wordBaseForms;
    }

    private static boolean wordIsApproved(String s, Morphology morphology) {
        String[] particlesNames;

        if (morphology instanceof RussianLuceneMorphology) {
            particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МС", "МС-П", "ПРИТЯЖ"};
        } else if (morphology instanceof EnglishLuceneMorphology) {
            particlesNames = new String[]{"ARTICLE", "CONJ", "PART", "PREP", "PRON", "ADV", "VBE", "MOD"};
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
