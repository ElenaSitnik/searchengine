package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class PageIndexing {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public String indexingPage(Site site, Page page, String html) {
        try{
            LemmasMaker lemmasMaker = LemmasMaker.getInstance();
            Map<String, Integer> lemmasMap = lemmasMaker.collectLemmas(getTextWithoutTags(html));
            for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                Lemma lemma = saveLemma(site, key);
                saveIndex(page, lemma, value);
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";

    }

    private String getTextWithoutTags(String text) {
        String cleanText = "";
        int start;
        int end;
        for (int i = 0; i < text.length(); ) {
            start = text.indexOf(">", i);
            end = text.indexOf("<", start);
            if (start < 0 || end < 0) {
                return cleanText;
            }
            if (end - start >= 2) {
                cleanText = cleanText.concat(text.substring(start + 1, end));
            }
            i = end + 1;
        }
        return cleanText;
    }

    private Lemma saveLemma(Site site, String key) {
        Optional<Lemma> lemmaOptional = lemmaRepository.findByLemma(key);
        Lemma lemma;
        if (lemmaOptional.isPresent()) {
            lemma = lemmaOptional.get();
            Integer frequency = lemma.getFrequency() + 1;
            lemma.setFrequency(frequency);
            lemmaRepository.save(lemma);
        } else {
            lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(key);
            lemma.setFrequency(1);
            lemmaRepository.save(lemma);
        }
        return lemma;
    }

    private void saveIndex(Page page, Lemma lemma, Integer count) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        Float rank = (float) count / lemma.getFrequency();
        index.setRank(rank);
        indexRepository.save(index);
    }
}
