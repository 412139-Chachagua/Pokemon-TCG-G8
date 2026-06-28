package ar.edu.utn.frc.tup.piii.clients;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;

@Component
public class PokemonTcgApiClient {

    private static final Logger logger = LoggerFactory.getLogger(PokemonTcgApiClient.class);
    private static final String BASE_URL = "https://api.pokemontcg.io/v2/cards";
    private static final int PAGE_SIZE = 50;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final RestTemplate restTemplate;

    public PokemonTcgApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<PokemonTcgApiCardDto> fetchAllCards() {
        String query = "(supertype:pokemon OR supertype:trainer OR supertype:energy) set.id:xy1";
        List<PokemonTcgApiCardDto> allCards = new ArrayList<>();
        List<Integer> failedPages = new ArrayList<>();

        logger.info("Consultando API Pokemon TCG - set XY1...");

        String firstUrl = buildUrl(query, 1);
        PokemonTcgApiResponse firstResponse = fetchPageWithRetry(firstUrl, 1, failedPages);

        if (firstResponse == null) {
            throw new RuntimeException("No se pudo obtener la primera pagina de cartas");
        }

        allCards.addAll(firstResponse.getData() != null ? firstResponse.getData() : new ArrayList<>());

        int totalCount = firstResponse.getTotalCount();
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

        logger.info("Primera pagina obtenida - Total de cartas en el set: {} - Paginas a descargar: {}", totalCount, totalPages);

        for (int page = 2; page <= totalPages; page++) {
            String pageUrl = buildUrl(query, page);
            PokemonTcgApiResponse pageResponse = fetchPageWithRetry(pageUrl, page, failedPages);

            if (pageResponse != null && pageResponse.getData() != null) {
                allCards.addAll(pageResponse.getData());
            }
        }

        if (failedPages.isEmpty()) {
            logger.info("Todas las {} paginas descargadas con exito", totalPages);
        } else {
            logger.warn("Paginas con fallo: {} de {}", failedPages.toString(), totalPages);
        }

        logger.info("Descarga completada: {} cartas obtenidas de {} paginas", allCards.size(), totalPages);
        return allCards;
    }

    private String buildUrl(String query, int page) {
        return BASE_URL + "?q=" + query + "&pageSize=" + PAGE_SIZE + "&page=" + page;
    }

    private PokemonTcgApiResponse fetchPageWithRetry(String url, int pageNumber, List<Integer> failedPages) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("[Pagina {}] Descargando... (intento {}/{})", pageNumber, attempt, MAX_RETRIES);

                ResponseEntity<PokemonTcgApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    PokemonTcgApiResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    int cardCount = response.getBody().getData() != null ? response.getBody().getData().size() : 0;
                    logger.info("[Pagina {}] OK - {} cartas obtenidas", pageNumber, cardCount);
                    return response.getBody();
                } else {
                    logger.warn("[Pagina {}] FALLO - HTTP {}", pageNumber, response.getStatusCode());
                }
            } catch (Exception ex) {
                logger.warn("[Pagina {}] FALLO - {}", pageNumber, ex.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                logger.info("[Pagina {}] Reintentando en {}ms...", pageNumber, backoffMs);
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }

        logger.error("[Pagina {}] FALLO definitivo tras {} intentos", pageNumber, MAX_RETRIES);
        failedPages.add(pageNumber);
        return null;
    }

    public PokemonTcgApiCardDto fetchCardById(String cardId) {
        String url = BASE_URL + "/" + cardId;
        try {
            ResponseEntity<PokemonTcgApiSingleCardResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                PokemonTcgApiSingleCardResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            }
        } catch (Exception ex) {
            logger.warn("fetchCardById {} failed: {}", cardId, ex.getMessage());
        }
        return null;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during backoff", e);
        }
    }
}
