package ar.edu.utn.frc.tup.piii.mappers.cards;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.AbilityType;
import ar.edu.utn.frc.tup.piii.dtos.cards.*;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardResistanceEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardWeaknessEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardMapperTest {

    private AbilityRegistry abilityRegistry;
    private CardMapper cardMapper;

    @BeforeEach
    void setUp() {
        abilityRegistry = mock(AbilityRegistry.class);
        cardMapper = new CardMapper(abilityRegistry);
    }

    @Nested
    class ToCardEntity {

        @Test
        void toCardEntity_pokemonCard_retornaCardEntity() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic", "EX"), "120",
                    List.of("Fire"), List.of("Fire", "Colorless"),
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("xy1-1", entity.getId());
            assertEquals("Charizard", entity.getName());
            assertEquals("POKEMON", entity.getSupertype());
            assertEquals("Basic,EX", entity.getSubtypes());
            assertEquals("xy1", entity.getSetCode());
            assertEquals("1", entity.getNumber());
            assertEquals(120, entity.getHp());
            assertEquals("BASIC", entity.getPokemonStage());
            assertEquals("Fire", entity.getPokemonTypes());
            assertEquals("Fire,Colorless", entity.getRetreatCost());
            assertTrue(entity.getIsEx());
            assertFalse(entity.getIsMega());
        }

        @Test
        void toCardEntity_energyCardBasic_retornaEnergyCardType() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-100", "Fire Energy", "Energy",
                    List.of("Basic"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("BASIC", entity.getEnergyCardType());
            assertEquals("FIRE", entity.getProvidesEnergyTypes());
            assertEquals("BASIC", entity.getStrategyKey());
        }

        @Test
        void toCardEntity_energyCardSpecial_retornaSpecialType() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-101", "Double Colorless Energy", "Energy",
                    List.of("Special"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("SPECIAL", entity.getEnergyCardType());
            assertEquals("DOUBLE_COLORLESS", entity.getStrategyKey());
        }

        @Test
        void toCardEntity_energyRainbow_retornaRainbowStrategy() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-102", "Rainbow Energy", "Energy",
                    List.of("Special"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("RAINBOW", entity.getStrategyKey());
        }

        @Test
        void toCardEntity_energyStrong_retornaStrongStrategy() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-103", "Strong Energy", "Energy",
                    List.of("Special"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("STRONG", entity.getStrategyKey());
        }

        @Test
        void toCardEntity_energyWithRules_retornaProvidesEnergyTypes() {
            PokemonTcgApiCardDto dto = createPokemonDtoWithRules(
                    "xy1-104", "Special Energy", "Energy",
                    List.of("Special"), null, null, List.of("Provides 2 of any energy"),
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("Provides 2 of any energy", entity.getProvidesEnergyTypes());
        }

        @Test
        void toCardEntity_trainerCardSupporter_retornaTrainerSubtype() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-50", "Professor Sycamore", "Trainer",
                    List.of("Supporter"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("SUPPORTER", entity.getTrainerSubtype());
            assertEquals("DISCARD_HAND_DRAW_7", entity.getEffectCode());
        }

        @Test
        void toCardEntity_trainerCardItem_retornaItemSubtype() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-51", "Evosoda", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("ITEM", entity.getTrainerSubtype());
            assertEquals("EVOSODA", entity.getEffectCode());
        }

        @Test
        void toCardEntity_trainerCardStadium_retornaStadiumSubtype() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-52", "Fairy Garden", "Trainer",
                    List.of("Stadium"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("STADIUM", entity.getTrainerSubtype());
            assertEquals("FAIRY_GARDEN", entity.getEffectCode());
        }

        @Test
        void toCardEntity_trainerCardTool_retornaPokemonToolSubtype() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-53", "Muscle Band", "Trainer",
                    List.of("Tool"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("POKEMON_TOOL", entity.getTrainerSubtype());
            assertEquals("ATTACH_TOOL", entity.getEffectCode());
        }

        @Test
        void toCardEntity_trainerWithAceSpec_retornaAceSpecTrue() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-54", "Ace Card", "Trainer",
                    List.of("Item", "ACE SPEC"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertTrue(entity.getIsAceSpec());
        }

        @Test
        void toCardEntity_conAttacks_retornaListaAttacks() {
            AttackDto attack = new AttackDto(0, "Flamethrower", List.of("Fire", "Colorless"), 2, "60", null, 60);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, List.of(attack), null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNotNull(entity.getAttacks());
            assertEquals(1, entity.getAttacks().size());
            assertEquals("Flamethrower", entity.getAttacks().getFirst().getName());
            assertEquals("Fire,Colorless", entity.getAttacks().getFirst().getPrintedCost());
            assertEquals(2, entity.getAttacks().getFirst().getConvertedEnergyCost());
            assertEquals("60", entity.getAttacks().getFirst().getDamageText());
            assertEquals(60, entity.getAttacks().getFirst().getBaseDamage());
        }

        @Test
        void toCardEntity_conWeaknesses_retornaListaWeaknesses() {
            WeaknessDto weakness = new WeaknessDto("Water", "×2");
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, List.of(weakness)
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNotNull(entity.getWeaknesses());
            assertEquals(1, entity.getWeaknesses().size());
            assertEquals("Water", entity.getWeaknesses().getFirst().getEnergyType());
            assertEquals(2, entity.getWeaknesses().getFirst().getMultiplier());
        }

        @Test
        void toCardEntity_conResistances_retornaListaResistances() {
            ResistanceDto resistance = new ResistanceDto("Fire", "-20");
            ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
            SetInfoDto set = new SetInfoDto("xy1");
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, List.of(resistance),
                    null, null, set, images, "Rare"
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNotNull(entity.getResistances());
            assertEquals(1, entity.getResistances().size());
            assertEquals("Fire", entity.getResistances().getFirst().getEnergyType());
            assertEquals(-20, entity.getResistances().getFirst().getValue());
        }

        @Test
        void toCardEntity_conEvolvesFrom_retornaEvolvesFrom() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-2", "Charmeleon", "Pokémon",
                    List.of("Stage 1"), "80", null, null,
                    "Charmander", null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("Charmeleon", entity.getName());
            assertEquals("STAGE_1", entity.getPokemonStage());
            assertEquals("Charmander", entity.getEvolvesFrom());
        }

        @Test
        void toCardEntity_megaCard_retornaIsMegaTrue() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-3", "Charizard EX", "Pokémon",
                    List.of("MEGA"), "220", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertTrue(entity.getIsMega());
            assertEquals("MEGA", entity.getPokemonStage());
        }

        @Test
        void toCardEntity_conRules_retornaRulesText() {
            PokemonTcgApiCardDto dto = createPokemonDtoWithRules(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, List.of("Rule 1", "Rule 2"),
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("Rule 1|Rule 2", entity.getRulesText());
        }

        @Test
        void toCardEntity_conAbilities_retornaAbilitiesJson() {
            AbilityDto ability = new AbilityDto("Blaze", "Description", "Ability");
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, List.of(ability), null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNotNull(entity.getAbilities());
            assertTrue(entity.getAbilities().contains("Blaze"));
        }

        @Test
        void toCardEntity_setNull_retornaSetCodeNull() {
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getSetCode());
        }

        @Test
        void toCardEntity_hpNull_retornaHpNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getHp());
        }

        @Test
        void toCardEntity_conEvolvesTo_retornaEvolvesTo() {
            ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
            SetInfoDto set = new SetInfoDto("xy1");
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charmander", "Pokémon",
                    List.of("Basic"), "50", null, null,
                    null, List.of("Charmeleon"), null, null, null, null,
                    null, null, set, images, "Rare"
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("Charmeleon", entity.getEvolvesTo());
        }

        @Test
        void toCardEntity_conConvertedRetreatCost_retornaValor() {
            ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
            SetInfoDto set = new SetInfoDto("xy1");
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, null,
                    null, 3, set, images, "Rare"
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(3, entity.getConvertedRetreatCost());
        }
    }

    @Nested
    class ToSummaryResponse {

        @Test
        void toSummaryResponse_conDatos_retornaCardSummaryResponse() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic,EX");

            CardSummaryResponse response = cardMapper.toSummaryResponse(entity);

            assertEquals("xy1-1", response.id());
            assertEquals("Charizard", response.name());
            assertEquals("POKEMON", response.supertype());
            assertEquals("xy1", response.setCode());
            assertEquals("1", response.number());
            assertEquals(List.of("Basic", "EX"), response.subtypes());
        }

        @Test
        void toSummaryResponse_subtypesNull_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", null);

            CardSummaryResponse response = cardMapper.toSummaryResponse(entity);

            assertTrue(response.subtypes().isEmpty());
        }
    }

    @Nested
    class ToDetailResponse {

        @Test
        void toDetailResponse_completo_retornaCardDetailResponse() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic,EX");
            entity.setHp(120);
            entity.setPokemonStage("BASIC");
            entity.setEvolvesFrom(null);
            entity.setPokemonTypes("Fire");
            entity.setRetreatCost("Colorless,Colorless");
            entity.setIsEx(true);
            entity.setIsMega(false);
            entity.setRulesText("Rule 1|Rule 2");
            entity.setImageLargeUrl("http://example.com/large.png");

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals("xy1-1", response.id());
            assertEquals("Charizard", response.name());
            assertEquals(120, response.hp());
            assertEquals("BASIC", response.stage());
            assertEquals(List.of("Fire"), response.types());
            assertEquals(List.of("Colorless", "Colorless"), response.retreatCost());
            assertTrue(response.isEx());
            assertFalse(response.isMega());
            assertEquals(List.of("Rule 1", "Rule 2"), response.rulesText());
            assertEquals("http://example.com/large.png", response.imageLargeUrl());
        }

        @Test
        void toDetailResponse_sinAttacks_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAttacks(null);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.attacks().isEmpty());
        }

        @Test
        void toDetailResponse_conAttacks_retornaListaAttacks() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardAttackEntity attack = new CardAttackEntity();
            attack.setAttackIndex(0);
            attack.setName("Flamethrower");
            attack.setPrintedCost("Fire,Colorless");
            attack.setConvertedEnergyCost(2);
            attack.setDamageText("60");
            attack.setEffectText(null);
            attack.setBaseDamage(60);
            entity.setAttacks(List.of(attack));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(1, response.attacks().size());
            assertEquals("Flamethrower", response.attacks().getFirst().name());
        }

        @Test
        void toDetailResponse_conWeaknesses_retornaListaWeaknesses() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardWeaknessEntity weakness = new CardWeaknessEntity();
            weakness.setEnergyType("Water");
            weakness.setMultiplier(2);
            entity.setWeaknesses(List.of(weakness));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(1, response.weaknesses().size());
            assertEquals("Water", response.weaknesses().getFirst().type());
            assertEquals("2", response.weaknesses().getFirst().value());
        }

        @Test
        void toDetailResponse_conResistances_retornaListaResistances() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardResistanceEntity resistance = new CardResistanceEntity();
            resistance.setEnergyType("Fire");
            resistance.setValue(-20);
            entity.setResistances(List.of(resistance));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(1, response.resistances().size());
            assertEquals("Fire", response.resistances().getFirst().type());
            assertEquals("-20", response.resistances().getFirst().value());
        }

        @Test
        void toDetailResponse_rulesTextNull_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setRulesText(null);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.rulesText().isEmpty());
        }

        @Test
        void toDetailResponse_conAbilities_retornaListaAbilities() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAbilities("[{\"name\":\"Blaze\",\"text\":\"Desc\",\"type\":\"Ability\"}]");
            when(abilityRegistry.has("Blaze")).thenReturn(true);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(1, response.abilities().size());
            assertEquals("Blaze", response.abilities().getFirst().name());
            assertTrue(response.abilities().getFirst().isActivable());
        }

        @Test
        void toDetailResponse_abilitiesNull_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAbilities(null);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.abilities().isEmpty());
        }
    }

    @Nested
    class ToAbilityDefinitions {

        @Test
        void toAbilityDefinitions_jsonValido_retornaLista() {
            String json = "[{\"name\":\"Blaze\",\"text\":\"Desc\",\"type\":\"Ability\"}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(1, result.size());
            assertEquals("Blaze", result.getFirst().getName());
            assertEquals("Desc", result.getFirst().getText());
            assertEquals(AbilityType.ABILITY, result.getFirst().getType());
        }

        @Test
        void toAbilityDefinitions_null_retornaListaVacia() {
            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(null);

            assertTrue(result.isEmpty());
        }

        @Test
        void toAbilityDefinitions_blank_retornaListaVacia() {
            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions("  ");

            assertTrue(result.isEmpty());
        }

        @Test
        void toAbilityDefinitions_jsonInvalido_retornaListaVacia() {
            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions("not json");

            assertTrue(result.isEmpty());
        }

        @Test
        void toAbilityDefinitions_typePower_retornaPokemonPower() {
            String json = "[{\"name\":\"Power\",\"text\":\"Desc\",\"type\":\"Pokémon Power\"}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(AbilityType.POKEMON_POWER, result.getFirst().getType());
        }

        @Test
        void toAbilityDefinitions_typeBody_retornaPokemonBody() {
            String json = "[{\"name\":\"Body\",\"text\":\"Desc\",\"type\":\"Pokémon Body\"}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(AbilityType.POKEMON_BODY, result.getFirst().getType());
        }

        @Test
        void toAbilityDefinitions_typeNull_retornaAbility() {
            String json = "[{\"name\":\"Test\",\"text\":\"Desc\",\"type\":null}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(AbilityType.ABILITY, result.getFirst().getType());
        }
    }

    @Nested
    class FormatEffectCode {

        @Test
        void formatEffectCode_drawCards_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DRAW_CARDS, Map.of("count", 3));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DRAW_CARDS:3", result);
        }

        @Test
        void formatEffectCode_drawCardsSinCount_retornaDefault1() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DRAW_CARDS, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DRAW_CARDS:1", result);
        }

        @Test
        void formatEffectCode_applySpecialCondition_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "BURN", "target", "defender"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("APPLY_SPECIAL_CONDITION:BURN", result);
        }

        @Test
        void formatEffectCode_applySpecialConditionSelf_retornaTarget() {
            AttackEffect effect = new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "POISON", "target", "self"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("APPLY_SPECIAL_CONDITION:POISON:self", result);
        }

        @Test
        void formatEffectCode_discardEnergy_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DISCARD_ENERGY,
                    Map.of("count", 2, "target", "attacker"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DISCARD_ENERGY:2:attacker", result);
        }

        @Test
        void formatEffectCode_discardEnergySinTarget_retornaSinTarget() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DISCARD_ENERGY,
                    Map.of("count", 1));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DISCARD_ENERGY:1", result);
        }

        @Test
        void formatEffectCode_searchDeck_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.SEARCH_DECK,
                    Map.of("searchType", "ENERGY", "count", 2, "energyType", "Fire"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("SEARCH_DECK:ENERGY:2:Fire", result);
        }

        @Test
        void formatEffectCode_attachEnergy_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.ATTACH_ENERGY,
                    Map.of("source", "deck", "energyType", "Fire", "count", 1, "target", "attacker"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("ATTACH_ENERGY:deck:Fire:1", result);
        }

        @Test
        void formatEffectCode_attachEnergyBenchTarget_retornaTarget() {
            AttackEffect effect = new AttackEffect(AttackEffectType.ATTACH_ENERGY,
                    Map.of("source", "deck", "energyType", "Water", "count", 1, "target", "bench"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("ATTACH_ENERGY:deck:Water:1:bench", result);
        }

        @Test
        void formatEffectCode_moveEnergy_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.MOVE_ENERGY,
                    Map.of("sourcePokemon", "attacker", "targetPokemon", "ownBench", "count", 2));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("MOVE_ENERGY:attacker:ownBench:2", result);
        }

        @Test
        void formatEffectCode_damagePrevention_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DAMAGE_PREVENTION,
                    Map.of("threshold", 30));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DAMAGE_PREVENTION:true:30", result);
        }

        @Test
        void formatEffectCode_damagePreventionSinThreshold_retornaSinThreshold() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DAMAGE_PREVENTION, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DAMAGE_PREVENTION:true", result);
        }

        @Test
        void formatEffectCode_cannotAttackNextTurn_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.CANNOT_ATTACK_NEXT_TURN,
                    Map.of("attackName", "Flamethrower"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("CANNOT_ATTACK_NEXT_TURN:true:Flamethrower", result);
        }

        @Test
        void formatEffectCode_supporterLock_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.SUPPORTER_LOCK, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("SUPPORTER_LOCK:true", result);
        }

        @Test
        void formatEffectCode_opponentDiscardHand_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.OPPONENT_DISCARD_HAND,
                    Map.of("count", 2));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("OPPONENT_DISCARD_HAND:2", result);
        }

        @Test
        void formatEffectCode_nextTurnDamageBonus_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.NEXT_TURN_DAMAGE_BONUS,
                    Map.of("bonus", 40));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("NEXT_TURN_DAMAGE_BONUS:40", result);
        }

        @Test
        void formatEffectCode_retreatLock_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.RETREAT_LOCK, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("RETREAT_LOCK:true", result);
        }

        @Test
        void formatEffectCode_damageReduction_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DAMAGE_REDUCTION,
                    Map.of("reduction", 30));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DAMAGE_REDUCTION:30", result);
        }

        @Test
        void formatEffectCode_discardOpponentDeck_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DISCARD_OPPONENT_DECK,
                    Map.of("count", 3, "target", "self"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DISCARD_OPPONENT_DECK:3:self", result);
        }

        @Test
        void formatEffectCode_searchDiscard_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.SEARCH_DISCARD,
                    Map.of("count", 2));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("SEARCH_DISCARD:2", result);
        }

        @Test
        void formatEffectCode_recycleFromDiscard_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.RECYCLE_FROM_DISCARD, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("RECYCLE_FROM_DISCARD:true", result);
        }

        @Test
        void formatEffectCode_opponentShuffleDraw_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.OPPONENT_SHUFFLE_DRAW,
                    Map.of("count", 4));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("OPPONENT_SHUFFLE_DRAW:4", result);
        }

        @Test
        void formatEffectCode_damageAllBench_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DAMAGE_ALL_BENCH,
                    Map.of("damageCounters", 2));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DAMAGE_ALL_BENCH:2", result);
        }

        @Test
        void formatEffectCode_defenderCannotAttack_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DEFENDER_CANNOT_ATTACK, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DEFENDER_CANNOT_ATTACK:true", result);
        }

        @Test
        void formatEffectCode_damageBench_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DAMAGE_BENCH,
                    Map.of("damage", 20, "ownBench", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DAMAGE_BENCH:20:ownBench", result);
        }

        @Test
        void formatEffectCode_healUser_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.HEAL_USER,
                    Map.of("count", 3));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("HEAL_USER:3", result);
        }

        @Test
        void formatEffectCode_healUserBench_retornaBench() {
            AttackEffect effect = new AttackEffect(AttackEffectType.HEAL_USER,
                    Map.of("count", 2, "targetBench", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("HEAL_USER:2:bench", result);
        }

        @Test
        void formatEffectCode_healUserAll_retornaAll() {
            AttackEffect effect = new AttackEffect(AttackEffectType.HEAL_USER,
                    Map.of("count", 1, "healAll", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("HEAL_USER:1:all", result);
        }

        @Test
        void formatEffectCode_healUserFull_retornaFull() {
            AttackEffect effect = new AttackEffect(AttackEffectType.HEAL_USER,
                    Map.of("count", 1, "healFull", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("HEAL_USER:1:full", result);
        }

        @Test
        void formatEffectCode_healUserClear_retornaClear() {
            AttackEffect effect = new AttackEffect(AttackEffectType.HEAL_USER,
                    Map.of("count", 1, "clearConditions", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("HEAL_USER:1:clear", result);
        }

        @Test
        void formatEffectCode_coinFlipBeforeDamage_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE,
                    Map.of("effectType", "DRAW_CARDS", "effectParam", "3"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("COIN_FLIP_BEFORE_DAMAGE:DRAW_CARDS:3", result);
        }

        @Test
        void formatEffectCode_coinFlipAfterDamage_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE,
                    Map.of("effectType", "HEAL_USER", "effectParam", "2", "applyOnHeads", "false"));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("COIN_FLIP_AFTER_DAMAGE:HEAL_USER:2:false", result);
        }

        @Test
        void formatEffectCode_recoil_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.RECOIL,
                    Map.of("count", 2));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("RECOIL:2", result);
        }

        @Test
        void formatEffectCode_switchAfterDamage_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.SWITCH_AFTER_DAMAGE,
                    Map.of("switchAttacker", true));

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("SWITCH_AFTER_DAMAGE:true", result);
        }

        @Test
        void formatEffectCode_abilitySuppression_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.ABILITY_SUPPRESSION, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("ABILITY_SUPPRESSION:true", result);
        }

        @Test
        void formatEffectCode_discardTool_retornaCodigoCorrecto() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DISCARD_TOOL, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DISCARD_TOOL:true", result);
        }

        @Test
        void formatEffectCode_defaultCase_retornaSoloType() {
            AttackEffect effect = new AttackEffect(AttackEffectType.REORDER_DECK, new HashMap<>());

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("REORDER_DECK", result);
        }

        @Test
        void formatEffectCode_paramsNull_retornaSoloType() {
            AttackEffect effect = new AttackEffect(AttackEffectType.DRAW_CARDS, null);

            String result = CardMapper.formatEffectCode(effect);

            assertEquals("DRAW_CARDS", result);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void toCardEntity_conAttackSinCost_retornaCostNull() {
            AttackDto attack = new AttackDto(0, "Tackle", null, 1, "10", null, 10);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Pikachu", "Pokémon",
                    List.of("Basic"), "60", null, null,
                    null, null, List.of(attack), null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getAttacks().getFirst().getPrintedCost());
        }

        @Test
        void toCardEntity_conAttackConvertedEnergyCostNull_retornaDefault0() {
            AttackDto attack = new AttackDto(0, "Tackle", List.of("Colorless"), null, "10", null, 10);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Pikachu", "Pokémon",
                    List.of("Basic"), "60", null, null,
                    null, null, List.of(attack), null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(0, entity.getAttacks().getFirst().getConvertedEnergyCost());
        }

        @Test
        void toCardEntity_weaknessValueNoNumerico_retornaDefault2() {
            WeaknessDto weakness = new WeaknessDto("Water", "×2");
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, List.of(weakness)
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(2, entity.getWeaknesses().getFirst().getMultiplier());
        }

        @Test
        void toCardEntity_weaknessValueNull_retornaDefault2() {
            WeaknessDto weakness = new WeaknessDto("Water", null);
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, List.of(weakness)
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(2, entity.getWeaknesses().getFirst().getMultiplier());
        }

        @Test
        void toCardEntity_resistanceValueNull_retornaDefaultMinus20() {
            ResistanceDto resistance = new ResistanceDto("Fire", null);
            ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
            SetInfoDto set = new SetInfoDto("xy1");
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, List.of(resistance),
                    null, null, set, images, "Rare"
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(-20, entity.getResistances().getFirst().getValue());
        }

        @Test
        void toCardEntity_resistanceValueInvalido_retornaDefaultMinus20() {
            ResistanceDto resistance = new ResistanceDto("Fire", "invalid");
            ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
            SetInfoDto set = new SetInfoDto("xy1");
            PokemonTcgApiCardDto dto = new PokemonTcgApiCardDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null, null, List.of(resistance),
                    null, null, set, images, "Rare"
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals(-20, entity.getResistances().getFirst().getValue());
        }

        @Test
        void toCardEntity_supertypeConAcento_retornaSinAcento() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("POKEMON", entity.getSupertype());
        }

        @Test
        void toDetailResponse_conProvidesEnergyTypes_retornaLista() {
            CardEntity entity = createCardEntity("xy1-100", "Fire Energy", "ENERGY", "Basic");
            entity.setProvidesEnergyTypes("Fire");

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(List.of("Fire"), response.providesEnergyTypes());
        }

        @Test
        void toDetailResponse_providesEnergyTypesNull_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-100", "Fire Energy", "ENERGY", "Basic");
            entity.setProvidesEnergyTypes(null);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.providesEnergyTypes().isEmpty());
        }
    }

    @Nested
    class GenerateEffectCode {

        @Test
        void generateEffectCode_textoVacio_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );
            dto = new PokemonTcgApiCardDto(
                    dto.id(), dto.name(), dto.supertype(), dto.subtypes(), dto.hp(),
                    dto.types(), dto.rules(), dto.evolvesFrom(), dto.evolvesTo(),
                    dto.abilities(), List.of(new AttackDto(0, "Tackle", List.of("Colorless"), 1, "10", "  ", 10)),
                    dto.weaknesses(), dto.resistances(), dto.retreatCost(), dto.convertedRetreatCost(),
                    dto.set(), dto.images(), dto.rarity()
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getAttacks().getFirst().getEffectCode());
        }

        @Test
        void generateEffectCode_textoNull_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );
            dto = new PokemonTcgApiCardDto(
                    dto.id(), dto.name(), dto.supertype(), dto.subtypes(), dto.hp(),
                    dto.types(), dto.rules(), dto.evolvesFrom(), dto.evolvesTo(),
                    dto.abilities(), List.of(new AttackDto(0, "Tackle", List.of("Colorless"), 1, "10", null, 10)),
                    dto.weaknesses(), dto.resistances(), dto.retreatCost(), dto.convertedRetreatCost(),
                    dto.set(), dto.images(), dto.rarity()
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getAttacks().getFirst().getEffectCode());
        }
    }

    @Nested
    class DetermineStage {

        @Test
        void toCardEntity_stage1_retornaStage1() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-2", "Charmeleon", "Pokémon",
                    List.of("Stage 1"), "80", null, null,
                    "Charmander", null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("STAGE_1", entity.getPokemonStage());
        }

        @Test
        void toCardEntity_stage2_retornaStage2() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-3", "Charizard", "Pokémon",
                    List.of("Stage 2"), "120", null, null,
                    "Charmeleon", null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("STAGE_2", entity.getPokemonStage());
        }

        @Test
        void toCardEntity_subtypesNull_retornaStageNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    null, "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getPokemonStage());
        }

        @Test
        void toCardEntity_subtypesSinStage_retornaStageNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("EX"), "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getPokemonStage());
        }
    }

    @Nested
    class GenerateTrainerEffectCode {

        @Test
        void generateTrainerEffectCode_nombreNull_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-50", null, "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_nombreNoReconocido_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-50", "Random Card", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_greatBall_retornaGreatBall() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-51", "Great Ball", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("GREAT_BALL", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_maxRevive_retornaMaxRevive() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-52", "Max Revive", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("MAX_REVIVE", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_professorsLetter_retornaProfessorsLetter() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-53", "Professor's Letter", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("PROFESSORS_LETTER", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_redCard_retornaRedCard() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-54", "Red Card", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("RED_CARD", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_rollerSkates_retornaCoinFlipDraw3() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-55", "Roller Skates", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("COIN_FLIP_DRAW_3", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_superPotion_retornaHeal60Discard1() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-56", "Super Potion", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("HEAL_60_DISCARD_1", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_cassius_retornaCassius() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-57", "Cassius", "Trainer",
                    List.of("Item"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("CASSIUS", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_shauna_retornaShuffleDraw5() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-58", "Shauna", "Trainer",
                    List.of("Supporter"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("SHUFFLE_DRAW_5", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_teamFlareGrunt_retornaTeamFlareGrunt() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-59", "Team Flare Grunt", "Trainer",
                    List.of("Supporter"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("TEAM_FLARE_GRUNT", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_shadowCircle_retornaShadowCircle() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-60", "Shadow Circle", "Trainer",
                    List.of("Stadium"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("SHADOW_CIRCLE", entity.getEffectCode());
        }

        @Test
        void generateTrainerEffectCode_hardCharm_retornaAttachTool() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-61", "Hard Charm", "Trainer",
                    List.of("Tool"), null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("ATTACH_TOOL", entity.getEffectCode());
        }

        @Test
        void toCardEntity_trainerSubtypeNull_retornaTrainerSubtypeNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-50", "Evosoda", "Trainer",
                    null, null, null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getTrainerSubtype());
        }
    }

    @Nested
    class ListAndCommaString {

        @Test
        void toSummaryResponse_commaStringConEspacios_retornaListaLimpia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic , EX , Stage 1");

            CardSummaryResponse response = cardMapper.toSummaryResponse(entity);

            assertEquals(3, response.subtypes().size());
            assertEquals("Basic", response.subtypes().get(0));
            assertEquals("EX", response.subtypes().get(1));
            assertEquals("Stage 1", response.subtypes().get(2));
        }

        @Test
        void toSummaryResponse_commaStringVacia_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "");

            CardSummaryResponse response = cardMapper.toSummaryResponse(entity);

            assertTrue(response.subtypes().isEmpty());
        }

        @Test
        void toDetailResponse_attacksConCostVacio_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardAttackEntity attack = new CardAttackEntity();
            attack.setAttackIndex(0);
            attack.setName("Tackle");
            attack.setPrintedCost("");
            attack.setConvertedEnergyCost(1);
            entity.setAttacks(List.of(attack));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.attacks().getFirst().cost().isEmpty());
        }

        @Test
        void toDetailResponse_weaknessMultiplierNull_retornaNullValue() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardWeaknessEntity weakness = new CardWeaknessEntity();
            weakness.setEnergyType("Water");
            weakness.setMultiplier(null);
            entity.setWeaknesses(List.of(weakness));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertNull(response.weaknesses().getFirst().value());
        }

        @Test
        void toDetailResponse_resistanceValueNull_retornaNullValue() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardResistanceEntity resistance = new CardResistanceEntity();
            resistance.setEnergyType("Fire");
            resistance.setValue(null);
            entity.setResistances(List.of(resistance));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertNull(response.resistances().getFirst().value());
        }

        @Test
        void toDetailResponse_attackIndexNull_retornaDefault0() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            CardAttackEntity attack = new CardAttackEntity();
            attack.setAttackIndex(null);
            attack.setName("Tackle");
            attack.setPrintedCost("Colorless");
            attack.setConvertedEnergyCost(1);
            entity.setAttacks(List.of(attack));

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(0, response.attacks().getFirst().index());
        }
    }

    @Nested
    class ToAbilityDefinitionsEdgeCases {

        @Test
        void toAbilityDefinitions_tipoNull_retornaAbility() {
            String json = "[{\"name\":\"Test\",\"text\":\"Desc\",\"type\":null}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(AbilityType.ABILITY, result.getFirst().getType());
        }

        @Test
        void toAbilityDefinitions_tipoVacio_retornaAbility() {
            String json = "[{\"name\":\"Test\",\"text\":\"Desc\",\"type\":\"\"}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(AbilityType.ABILITY, result.getFirst().getType());
        }

        @Test
        void toAbilityDefinitions_multiplesHabilidades_retornaTodas() {
            String json = "[{\"name\":\"Ability1\",\"text\":\"Desc1\",\"type\":\"Ability\"},{\"name\":\"Ability2\",\"text\":\"Desc2\",\"type\":\"Pokémon Power\"}]";

            List<AbilityDefinition> result = cardMapper.toAbilityDefinitions(json);

            assertEquals(2, result.size());
            assertEquals("Ability1", result.get(0).getName());
            assertEquals("Ability2", result.get(1).getName());
        }
    }

    @Nested
    class ToCardAbilityResponseEdgeCases {

        @Test
        void toDetailResponse_abilitiesJsonInvalido_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAbilities("invalid json");

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.abilities().isEmpty());
        }

        @Test
        void toDetailResponse_abilitiesVacio_retornaListaVacia() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAbilities("[]");

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertTrue(response.abilities().isEmpty());
        }

        @Test
        void toDetailResponse_abilityNoRegistrada_retornaIsActivableFalse() {
            CardEntity entity = createCardEntity("xy1-1", "Charizard", "POKEMON", "Basic");
            entity.setAbilities("[{\"name\":\"UnknownAbility\",\"text\":\"Desc\",\"type\":\"Ability\"}]");
            when(abilityRegistry.has("UnknownAbility")).thenReturn(false);

            CardDetailResponse response = cardMapper.toDetailResponse(entity);

            assertEquals(1, response.abilities().size());
            assertFalse(response.abilities().getFirst().isActivable());
        }
    }

    @Nested
    class NormalizeSupertype {

        @Test
        void normalizeSupertype_null_retornaNull() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", null,
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertNull(entity.getSupertype());
        }

        @Test
        void normalizeSupertype_conEMinuscula_retornaSinAcento() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "Pokémon",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("POKEMON", entity.getSupertype());
        }

        @Test
        void normalizeSupertype_conEMayuscula_retornaSinAcento() {
            PokemonTcgApiCardDto dto = createPokemonDto(
                    "xy1-1", "Charizard", "POKéMON",
                    List.of("Basic"), "120", null, null,
                    null, null, null, null
            );

            CardEntity entity = cardMapper.toCardEntity(dto);

            assertEquals("POKEMON", entity.getSupertype());
        }
    }

    private PokemonTcgApiCardDto createPokemonDto(String id, String name, String supertype,
                                                   List<String> subtypes, String hp,
                                                   List<String> types, List<String> retreatCost,
                                                   String evolvesFrom, List<AbilityDto> abilities,
                                                   List<AttackDto> attacks, List<WeaknessDto> weaknesses) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        SetInfoDto set = new SetInfoDto("xy1");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, hp, types, null,
                evolvesFrom, null, abilities, attacks, weaknesses, null,
                retreatCost, null, set, images, "Rare"
        );
    }

    private PokemonTcgApiCardDto createPokemonDtoWithRules(String id, String name, String supertype,
                                                            List<String> subtypes, String hp,
                                                            List<String> types, List<String> rules,
                                                            String evolvesFrom, List<AbilityDto> abilities,
                                                            List<AttackDto> attacks, List<WeaknessDto> weaknesses) {
        ImagesDto images = new ImagesDto("http://example.com/small.png", "http://example.com/large.png");
        SetInfoDto set = new SetInfoDto("xy1");
        return new PokemonTcgApiCardDto(
                id, name, supertype, subtypes, hp, types, rules,
                evolvesFrom, null, abilities, attacks, weaknesses, null,
                null, null, set, images, "Rare"
        );
    }

    private CardEntity createCardEntity(String id, String name, String supertype, String subtypes) {
        CardEntity entity = new CardEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSupertype(supertype);
        entity.setSubtypes(subtypes);
        entity.setSetCode("xy1");
        entity.setNumber(id.contains("-") ? id.substring(id.indexOf("-") + 1) : null);
        entity.setIsEx(false);
        entity.setIsMega(false);
        return entity;
    }
}
