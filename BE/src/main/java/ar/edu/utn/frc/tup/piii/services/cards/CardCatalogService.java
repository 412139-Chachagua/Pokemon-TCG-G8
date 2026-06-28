package ar.edu.utn.frc.tup.piii.services.cards;

import ar.edu.utn.frc.tup.piii.dtos.cards.CardDetailResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSearchRequest;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSearchResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSummaryResponse;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.cards.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardCatalogService {

    private static final Logger log = LoggerFactory.getLogger(CardCatalogService.class);

    private final CardJpaRepository cardJpaRepository;
    private final CardMapper cardMapper;

    public CardSearchResponse searchCards(CardSearchRequest request) {
        Instant start = Instant.now();
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Specification<CardEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.query() != null && !request.query().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + request.query().toLowerCase() + "%"));
            }

            if (request.supertype() != null && !request.supertype().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("supertype")), request.supertype().toUpperCase()));
            }

            if (request.setCode() != null && !request.setCode().isBlank()) {
                predicates.add(cb.equal(root.get("setCode"), request.setCode()));
            }

            if (request.stage() != null && !request.stage().isBlank()) {
                predicates.add(cb.equal(root.get("pokemonStage"), request.stage()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CardSummaryResponse> resultPage = cardJpaRepository.findAll(spec, pageable)
                .map(cardMapper::toSummaryResponse);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        if (elapsed > 500) {
            log.warn("[PERF] searchCards(query={}) took {}ms (exceeded 500ms)", request.query(), elapsed);
        } else {
            log.debug("[PERF] searchCards(query={}) took {}ms", request.query(), elapsed);
        }

        return new CardSearchResponse(resultPage.getContent(), resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements());
    }

    public CardDetailResponse getCardById(String id) {
        Instant start = Instant.now();
        CardEntity entity = cardJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + id));
        CardDetailResponse result = cardMapper.toDetailResponse(entity);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        if (elapsed > 500) {
            log.warn("[PERF] getCardById({}) took {}ms (exceeded 500ms)", id, elapsed);
        } else {
            log.debug("[PERF] getCardById({}) took {}ms", id, elapsed);
        }

        return result;
    }
}
