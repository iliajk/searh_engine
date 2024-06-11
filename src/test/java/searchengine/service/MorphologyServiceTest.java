package searchengine.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.services.MorphologyService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MorphologyServiceTest {
    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private IndexRepository indexRepository;

    @Mock
    private LuceneMorphology rusMorphology;

    @Mock
    private LuceneMorphology engMorphology;

    @InjectMocks
    private MorphologyService morphologyService;
    @Test
    public void countLemmaRu() throws Exception {
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард " +
                "постоянно обитает в некоторых районах Северного Кавказа.";
        Map<String, Integer> preferredOutput = new HashMap<>();
        preferredOutput.put("повторный", 1);
        preferredOutput.put("появление", 1);
        preferredOutput.put("постоянно", 1);
        preferredOutput.put("позволять", 1);
        preferredOutput.put("предположить", 1);
        preferredOutput.put("северный", 1);
        preferredOutput.put("район", 1);
        preferredOutput.put("кавказ", 1);
        preferredOutput.put("осетия", 1);
        preferredOutput.put("леопард", 2);
        preferredOutput.put("обитать", 1);
        Class<?> reflection = MorphologyService.class;
        Method method = reflection.getDeclaredMethod("countLemma", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(morphologyService, text);
        assertEquals(preferredOutput, result);
    }
    @Test
    public void countLemmaEng() throws Exception {
        String text = "The quick brown fox jumps over the lazy dog. The quick fox is very quick.";
        Map<String, Integer> preferredOutput = new HashMap<>();
        preferredOutput.put("very", 1);
        preferredOutput.put("over", 1);
        preferredOutput.put("quick", 3);
        preferredOutput.put("brown", 1);
        preferredOutput.put("fox", 2);
        preferredOutput.put("jump", 1);
        preferredOutput.put("lazy", 1);
        preferredOutput.put("dog", 1);
        Class<?> reflection = MorphologyService.class;
        Method method = reflection.getDeclaredMethod("countLemma", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(morphologyService, text);
        assertEquals(preferredOutput, result);
    }
}
