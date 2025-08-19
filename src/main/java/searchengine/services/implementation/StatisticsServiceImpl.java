package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.intarfaces.StatisticsService;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        if (siteRepository.findAll().isEmpty()) {
            return getEmptyStatistic();
        }

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.findAll().size());
        total.setPages(pageRepository.findAll().size());
        total.setLemmas(lemmaRepository.findAll().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<searchengine.model.Site> sitesList = siteRepository.findAll();
            for (Site site : sitesList) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());
                item.setStatus(site.getStatus().toString());
                item.setError(site.getLastError());
                item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.ofHours(2)));
                item.setPages(pageRepository.findAllBySiteId(site.getId()).size());
                item.setLemmas(lemmaRepository.findAllBySiteId(site.getId()).size());
                detailed.add(item);
            }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    private StatisticsResponse getEmptyStatistic() {
        TotalStatistics total = new TotalStatistics();
        total.setIndexing(false);
        total.setLemmas(0);
        total.setPages(0);
        total.setSites(0);

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(new ArrayList<>());

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);

        return response;
    }
}
