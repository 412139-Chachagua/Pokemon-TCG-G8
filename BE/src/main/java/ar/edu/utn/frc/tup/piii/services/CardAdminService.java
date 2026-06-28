package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.attack.TextEffectParser;
import ar.edu.utn.frc.tup.piii.mappers.cards.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardAdminService {

    private static final Logger log = LoggerFactory.getLogger(CardAdminService.class);
    private final CardJpaRepository cardJpaRepository;

    public CardAdminService(CardJpaRepository cardJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
    }

    @Transactional
    public RegenerateResult regenerateAllEffectCodes() {
        List<CardEntity> allCards = cardJpaRepository.findAll();
        int totalAttacks = 0;
        int updatedAttacks = 0;
        int skippedAttacks = 0;
        int errorAttacks = 0;
        List<String> errors = new ArrayList<>();

        for (CardEntity card : allCards) {
            if (card.getAttacks() == null || card.getAttacks().isEmpty()) continue;

            for (CardAttackEntity attack : card.getAttacks()) {
                totalAttacks++;
                String effectText = attack.getEffectText();
                if (effectText == null || effectText.isBlank()) {
                    skippedAttacks++;
                    continue;
                }

                try {
                    List<AttackEffect> effects = TextEffectParser.parse(effectText);
                    if (effects == null || effects.isEmpty()) {
                        attack.setEffectCode(null);
                    } else {
                        String newCode = effects.stream()
                                .map(CardMapper::formatEffectCode)
                                .collect(Collectors.joining(";;"));
                        String oldCode = attack.getEffectCode();
                        if (!newCode.equals(oldCode)) {
                            attack.setEffectCode(newCode);
                            updatedAttacks++;
                            log.warn("[admin] Updated effectCode for {}/{}: '{}' -> '{}'",
                                    card.getId(), attack.getName(), oldCode, newCode);
                        }
                    }
                } catch (Exception e) {
                    errorAttacks++;
                    errors.add(String.format("%s/%s: %s", card.getId(), attack.getName(), e.getMessage()));
                    log.error("[admin] Error parsing effectText for {}/{}", card.getId(), attack.getName(), e);
                }
            }
        }

        cardJpaRepository.saveAll(allCards);

        RegenerateResult result = new RegenerateResult();
        result.totalAttacks = totalAttacks;
        result.updatedAttacks = updatedAttacks;
        result.skippedAttacks = skippedAttacks;
        result.errorAttacks = errorAttacks;
        result.errors = errors;
        return result;
    }

    public static class RegenerateResult {
        public int totalAttacks;
        public int updatedAttacks;
        public int skippedAttacks;
        public int errorAttacks;
        public List<String> errors;
    }
}
