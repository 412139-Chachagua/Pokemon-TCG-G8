package ar.edu.utn.frc.tup.piii.mappers.cards;

import ar.edu.utn.frc.tup.piii.dtos.cards.*;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.EnergyCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardResistanceEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardWeaknessEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.TrainerCardEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiCardMapperTest {

    private ApiCardMapper apiCardMapper;

    @BeforeEach
    void setUp() {
        apiCardMapper = new ApiCardMapper();
    }

    @Nested
    class ToPokemonCardEntity {

        @Test
        void toPokemonCardEntity_datosValidos_retornaPokemonCardEntity() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic", "EX"), "120",
                    List.of("Fire"), List.of("Fire", "Colorless"),
                    "Charmander", 3, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertEquals("xy1-1", entity.getId());
            assertEquals("Charizard", entity.getName());
            assertEquals("Pokémon", entity.getSupertype());
            assertEquals("Basic,EX", entity.getSubtypes());
            assertEquals(120, entity.getHp());
            assertEquals("Fire", entity.getPokemonTypes());
            assertEquals("Charmander", entity.getEvolvesFrom());
            assertEquals("Fire,Colorless", entity.getRetreatCost());
            assertEquals(3, entity.getConvertedRetreatCost());
            assertTrue(entity.getIsEx());
            assertFalse(entity.getIsMega());
            assertEquals("Rare", entity.getRarity());
            assertEquals("http://example.com/small.png", entity.getImageSmallUrl());
            assertEquals("http://example.com/large.png", entity.getImageLargeUrl());
        }

        @Test
        void toPokemonCardEntity_conAttacks_retornaListaAttacks() {
            AttackDto attack = new AttackDto(0, "Flamethrower", List.of("Fire", "Colorless"), 2, "60", null, 60);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, List.of(attack), null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getAttacks());
            assertEquals(1, entity.getAttacks().size());
            assertEquals("Flamethrower", entity.getAttacks().getFirst().getName());
            assertEquals("Fire,Colorless", entity.getAttacks().getFirst().getPrintedCost());
            assertEquals(2, entity.getAttacks().getFirst().getConvertedEnergyCost());
            assertEquals("60", entity.getAttacks().getFirst().getDamageText());
        }

        @Test
        void toPokemonCardEntity_conWeaknesses_retornaListaWeaknesses() {
            WeaknessDto weakness = new WeaknessDto("Water", "×2");
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, List.of(weakness), null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getWeaknesses());
            assertEquals(1, entity.getWeaknesses().size());
            assertEquals("Water", entity.getWeaknesses().getFirst().getWeaknessType());
            assertEquals("×2", entity.getWeaknesses().getFirst().getWeaknessValue());
        }

        @Test
        void toPokemonCardEntity_conResistances_retornaListaResistances() {
            ResistanceDto resistance = new ResistanceDto("Fire", "-20");
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, List.of(resistance)
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getResistances());
            assertEquals(1, entity.getResistances().size());
            assertEquals("Fire", entity.getResistances().getFirst().getResistanceType());
            assertEquals("-20", entity.getResistances().getFirst().getResistanceValue());
        }

        @Test
        void toPokemonCardEntity_megaCard_retornaIsMegaTrue() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-3", "Charizard EX", "Pokémon",
                    List.of("MEGA"), "220", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertTrue(entity.getIsMega());
        }

        @Test
        void toPokemonCardEntity_conRules_retornaRulesText() {
            PokemonTcgApiCardDto dto = createPokemonDtoWithRules(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, List.of("Rule 1", "Rule 2"),
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertEquals("Rule 1|Rule 2", entity.getRulesText());
        }

        @Test
        void toPokemonCardEntity_hpNull_retornaHpNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), null, null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNull(entity.getHp());
        }

        @Test
        void toPokemonCardEntity_imagesNull_retornaImagesNull() {
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNull(entity.getImageSmallUrl());
            assertNull(entity.getImageLargeUrl());
        }
    }

    @Nested
    class ToTrainerCardEntity {

        @Test
        void toTrainerCardEntity_datosValidos_retornaTrainerCardEntity() {
            PokemonTcgApiCardDto dto = createTrainerDto(
                    "xy1-50", "Professor Sycamore", "Trainer",
                    List.of("Supporter"), List.of("Discard your hand, draw 7 cards")
            );

            TrainerCardEntity entity = apiCardMapper.toTrainerCardEntity(dto);

            assertEquals("xy1-50", entity.getId());
            assertEquals("Professor Sycamore", entity.getName());
            assertEquals("Trainer", entity.getSupertype());
            assertEquals("Supporter", entity.getSubtypes());
            assertEquals("Discard your hand, draw 7 cards", entity.getRulesText());
            assertEquals("Rare", entity.getRarity());
            assertEquals("http://example.com/small.png", entity.getImageSmallUrl());
            assertEquals("http://example.com/large.png", entity.getImageLargeUrl());
        }

        @Test
        void toTrainerCardEntity_rulesNull_retornaRulesTextNull() {
            PokemonTcgApiCardDto dto = createTrainerDto(
                    "xy1-51", "Evosoda", "Trainer",
                    List.of("Item"), null
            );

            TrainerCardEntity entity = apiCardMapper.toTrainerCardEntity(dto);

            assertNull(entity.getRulesText());
        }

        @Test
        void toTrainerCardEntity_subtypesNull_retornaSubtypesNull() {
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-51", "Evosoda", "Trainer",
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null
            );

            TrainerCardEntity entity = apiCardMapper.toTrainerCardEntity(dto);

            assertNull(entity.getSubtypes());
        }
    }

    @Nested
    class ToEnergyCardEntity {

        @Test
        void toEnergyCardEntity_datosValidos_retornaEnergyCardEntity() {
            PokemonTcgApiCardDto dto = createEnergyDto(
                    "xy1-100", "Fire Energy", "Energy",
                    List.of("Basic"), null
            );

            EnergyCardEntity entity = apiCardMapper.toEnergyCardEntity(dto);

            assertEquals("xy1-100", entity.getId());
            assertEquals("Fire Energy", entity.getName());
            assertEquals("Energy", entity.getSupertype());
            assertEquals("Basic", entity.getSubtypes());
            assertEquals("http://example.com/small.png", entity.getImageSmallUrl());
            assertEquals("http://example.com/large.png", entity.getImageLargeUrl());
        }

        @Test
        void toEnergyCardEntity_rulesNull_retornaRulesTextNull() {
            PokemonTcgApiCardDto dto = createEnergyDto(
                    "xy1-100", "Fire Energy", "Energy",
                    List.of("Basic"), null
            );

            EnergyCardEntity entity = apiCardMapper.toEnergyCardEntity(dto);

            assertNull(entity.getRulesText());
        }

        @Test
        void toEnergyCardEntity_conRules_retornaRulesText() {
            PokemonTcgApiCardDto dto = createEnergyDto(
                    "xy1-101", "Special Energy", "Energy",
                    List.of("Special"), List.of("Provides 2 of any energy")
            );

            EnergyCardEntity entity = apiCardMapper.toEnergyCardEntity(dto);

            assertEquals("Provides 2 of any energy", entity.getRulesText());
        }

        @Test
        void toEnergyCardEntity_imagesNull_retornaImagesNull() {
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-100", "Fire Energy", "Energy",
                    List.of("Basic"), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null
            );

            EnergyCardEntity entity = apiCardMapper.toEnergyCardEntity(dto);

            assertNull(entity.getImageSmallUrl());
            assertNull(entity.getImageLargeUrl());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void toPokemonCardEntity_conAttackSinCost_retornaCostNull() {
            AttackDto attack = new AttackDto(0, "Tackle", null, 1, "10", null, 10);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Pikachu", "Pokémon",
                    List.of("Basic"), "60", null, null,
                    null, null, null, List.of(attack), null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNull(entity.getAttacks().getFirst().getPrintedCost());
        }

        @Test
        void toPokemonCardEntity_conAttackConvertedEnergyCostNull_retornaDefault0() {
            AttackDto attack = new AttackDto(0, "Tackle", List.of("Colorless"), null, "10", null, 10);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Pikachu", "Pokémon",
                    List.of("Basic"), "60", null, null,
                    null, null, null, List.of(attack), null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertEquals(0, entity.getAttacks().getFirst().getConvertedEnergyCost());
        }

        @Test
        void toPokemonCardEntity_evolvesFromNull_retornaEvolvesFromNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charmander", "Pokémon",
                    List.of("Basic"), "50", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNull(entity.getEvolvesFrom());
        }

        @Test
        void toPokemonCardEntity_convertedRetreatCostNull_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNull(entity.getConvertedRetreatCost());
        }

        @Test
        void toPokemonCardEntity_attacksNull_retornaListaVacia() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getAttacks());
            assertTrue(entity.getAttacks().isEmpty());
        }

        @Test
        void toPokemonCardEntity_weaknessesNull_retornaListaVacia() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getWeaknesses());
            assertTrue(entity.getWeaknesses().isEmpty());
        }

        @Test
        void toPokemonCardEntity_resistancesNull_retornaListaVacia() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null
            );

            PokemonCardEntity entity = apiCardMapper.toPokemonCardEntity(dto);

            assertNotNull(entity.getResistances());
            assertTrue(entity.getResistances().isEmpty());
        }
    }

    private PokemonTcgApiCardDto createPokemonDto(String id, String name, String supertype,
                                                   List<String> subtypes, String hp,
                                                   List<String> types, List<String> retreatCost,
                                                   String evolvesFrom, Integer convertedRetreatCost,
                                                   List<AbilityDto> abilities, List<AttackDto> attacks,
                                                   List<WeaknessDto> weaknesses, List<ResistanceDto> resistances) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, hp, types, null,
                evolvesFrom, null, abilities, attacks, weaknesses, resistances,
                retreatCost, convertedRetreatCost, null, images, "Rare"
        );
    }

    private PokemonTcgApiCardDto createPokemonDtoWithRules(String id, String name, String supertype,
                                                            List<String> subtypes, String hp,
                                                            List<String> types, List<String> rules,
                                                            String evolvesFrom, Integer convertedRetreatCost,
                                                            List<AbilityDto> abilities, List<AttackDto> attacks,
                                                            List<WeaknessDto> weaknesses, List<ResistanceDto> resistances) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, hp, types, rules,
                evolvesFrom, null, abilities, attacks, weaknesses, resistances,
                null, convertedRetreatCost, null, images, "Rare"
        );
    }

    private PokemonTcgApiCardDto createTrainerDto(String id, String name, String supertype,
                                                   List<String> subtypes, List<String> rules) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, null, null, rules,
                null, null, null, null, null, null,
                null, null, null, images, "Rare"
        );
    }

    private PokemonTcgApiCardDto createEnergyDto(String id, String name, String supertype,
                                                  List<String> subtypes, List<String> rules) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, null, null, rules,
                null, null, null, null, null, null,
                null, null, null, images, "Rare"
        );
    }
}
