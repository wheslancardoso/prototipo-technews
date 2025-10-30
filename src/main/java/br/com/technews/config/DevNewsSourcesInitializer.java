package br.com.technews.config;

import br.com.technews.entity.NewsSource;
import br.com.technews.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Inicializador de dados para o perfil de desenvolvimento (H2).
 * Quando não há fontes ativas, semeia as principais fontes brasileiras
 * para viabilizar testes da coleta manual pelo painel admin.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevNewsSourcesInitializer implements CommandLineRunner {

    private final NewsSourceRepository newsSourceRepository;

    @Override
    public void run(String... args) {
        try {
            long activeCount = newsSourceRepository.countActiveSources();
            if (activeCount > 0) {
                log.info("Fontes ativas já existem ({}). Inicializador dev não será executado.", activeCount);
                return;
            }

            log.info("Nenhuma fonte ativa encontrada no perfil dev. Semeando fontes brasileiras padrão...");

            List<NewsSource> seeds = new ArrayList<>();

            // Portais principais (intervalo 45 min, até 12 artigos)
            seeds.add(build("TecMundo", "https://www.tecmundo.com.br/rss", 45, 12));
            seeds.add(build("Canaltech", "https://canaltech.com.br/rss/", 45, 12));
            seeds.add(build("Olhar Digital", "https://olhardigital.com.br/feed/", 45, 12));
            seeds.add(build("Tecnoblog", "https://tecnoblog.net/feed/", 45, 12));
            seeds.add(build("Gizmodo Brasil", "https://gizmodo.uol.com.br/feed/", 45, 12));

            // Fontes especializadas (intervalo 120 min, até 8 artigos)
            seeds.add(build("Inovação Tecnológica", "https://www.inovacaotecnologica.com.br/noticias/rss.php", 120, 8));
            seeds.add(build("Showmetech", "https://www.showmetech.com.br/feed/", 120, 8));
            seeds.add(build("Meio Bit", "https://meiobit.com/feed/", 120, 8));
            seeds.add(build("Adrenaline", "https://adrenaline.com.br/rss/", 120, 8));

            // Podcast (intervalo 240 min, até 5 episódios)
            seeds.add(build("Canaltech Podcast", "https://feeds.simplecast.com/7KcWpKc2", 240, 5));

            newsSourceRepository.saveAll(seeds);
            log.info("Semeadas {} fontes brasileiras no perfil dev.", seeds.size());
        } catch (Exception e) {
            log.error("Erro ao semear fontes no perfil dev: {}", e.getMessage(), e);
        }
    }

    private NewsSource build(String name, String url, int intervalMinutes, int maxPerFetch) {
        NewsSource ns = new NewsSource();
        ns.setName(name);
        ns.setUrl(url);
        ns.setType(NewsSource.SourceType.RSS_FEED);
        ns.setActive(true);
        ns.setFetchIntervalMinutes(intervalMinutes);
        ns.setMaxArticlesPerFetch(maxPerFetch);
        return ns;
    }
}