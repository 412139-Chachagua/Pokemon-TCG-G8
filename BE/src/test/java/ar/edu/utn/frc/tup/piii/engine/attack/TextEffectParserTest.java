package ar.edu.utn.frc.tup.piii.engine.attack;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextEffectParserTest {

    // ─────────────────────────────────────────────
    // parse() — main orchestrator
    // ─────────────────────────────────────────────

    @Test
    void parse_withNull_returnsEmptyList() {
        assertTrue(TextEffectParser.parse(null).isEmpty());
    }

    @Test
    void parse_withEmptyString_returnsEmptyList() {
        assertTrue(TextEffectParser.parse("").isEmpty());
    }

    @Test
    void parse_withBlankString_returnsEmptyList() {
        assertTrue(TextEffectParser.parse("   ").isEmpty());
    }

    @Test
    void parse_withSimpleDamage_onlyDamageEffectReturned() {
        List<AttackEffect> results = TextEffectParser.parse("Deal 40 damage");
        assertEquals(0, results.size());
    }

    @Test
    void parse_withHealEffect_returnsHealEffect() {
        List<AttackEffect> results = TextEffectParser.parse("Heal 30 damage from this Pokémon");
        assertEquals(1, results.size());
        assertEquals(AttackEffectType.HEAL_USER, results.get(0).getType());
    }

    @Test
    void parse_withSpecialCondition_returnsConditionEffect() {
        List<AttackEffect> results = TextEffectParser.parse("The Defending Pokémon is now Paralyzed");
        assertEquals(1, results.size());
        assertEquals(AttackEffectType.APPLY_SPECIAL_CONDITION, results.get(0).getType());
    }

    @Test
    void parse_withCoinFlipAndCancel_returnsCoinFlipEffect() {
        List<AttackEffect> results = TextEffectParser.parse("Flip a coin. If tails, this attack does nothing.");
        assertFalse(results.isEmpty());
        boolean hasCoinFlip = results.stream()
                .anyMatch(e -> e.getType() == AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        assertTrue(hasCoinFlip);
    }

    @Test
    void parse_withMultipleEffects_returnsAllEffects() {
        List<AttackEffect> results = TextEffectParser.parse(
                "Heal 30 damage from this Pokémon. The Defending Pokémon is now Paralyzed.");
        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.HEAL_USER));
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.APPLY_SPECIAL_CONDITION));
    }

    @Test
    void parse_withDiscardEnergy_returnsDiscardEffect() {
        List<AttackEffect> results = TextEffectParser.parse(
                "Discard 2 Energy from the Defending Pokémon.");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.DISCARD_ENERGY));
    }

    @Test
    void parse_withBenchDamage_returnsBenchDamageEffect() {
        List<AttackEffect> results = TextEffectParser.parse(
                "This attack does 30 damage to 1 of your opponent's Benched Pokémon.");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.DAMAGE_BENCH));
    }

    @Test
    void parse_withDrawCards_returnsDrawEffect() {
        List<AttackEffect> results = TextEffectParser.parse("Draw 2 cards.");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.DRAW_CARDS));
    }

    @Test
    void parse_withSearchDeck_returnsSearchEffect() {
        List<AttackEffect> results = TextEffectParser.parse(
                "Search your deck for a Basic Pokémon and put it onto your Bench.");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.SEARCH_DECK));
    }

    @Test
    void parse_withAttachEnergy_returnsAttachEffect() {
        List<AttackEffect> results = TextEffectParser.parse(
                "Attach a basic Energy card from your deck to this Pokémon.");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getType() == AttackEffectType.ATTACH_ENERGY));
    }

    // ─────────────────────────────────────────────
    // parseSpecialConditions()
    // ─────────────────────────────────────────────

    @Test
    void parseSpecialConditions_withParalyzed_returnsParalyzed() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Paralyzed.");
        assertEquals(1, results.size());
        assertEquals("PARALYZED", results.get(0).getParams().get("condition"));
        assertEquals("defender", results.get(0).getParams().get("target"));
    }

    @Test
    void parseSpecialConditions_withPoisoned_returnsPoisoned() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Poisoned.");
        assertEquals(1, results.size());
        assertEquals("POISONED", results.get(0).getParams().get("condition"));
    }

    @Test
    void parseSpecialConditions_withConfused_returnsConfused() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Confused.");
        assertEquals(1, results.size());
        assertEquals("CONFUSED", results.get(0).getParams().get("condition"));
    }

    @Test
    void parseSpecialConditions_withAsleep_returnsAsleep() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Asleep.");
        assertEquals(1, results.size());
        assertEquals("ASLEEP", results.get(0).getParams().get("condition"));
    }

    @Test
    void parseSpecialConditions_withBurned_returnsBurned() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Burned.");
        assertEquals(1, results.size());
        assertEquals("BURNED", results.get(0).getParams().get("condition"));
    }

    @Test
    void parseSpecialConditions_withParalyzedAndPoisoned_returnsBoth() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "The Defending Pokémon is now Paralyzed and Poisoned.");
        assertEquals(2, results.size());
        assertEquals("PARALYZED", results.get(0).getParams().get("condition"));
        assertEquals("POISONED", results.get(1).getParams().get("condition"));
    }

    @Test
    void parseSpecialConditions_withBothActiveParalyzedAndPoisoned_setsBothTarget() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "Both Active Pokémon are now Paralyzed and Poisoned.");
        assertEquals(2, results.size());
        assertEquals("both", results.get(0).getParams().get("target"));
        assertEquals("both", results.get(1).getParams().get("target"));
    }

    @Test
    void parseSpecialConditions_withSelfTarget_setsTargetToSelf() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "This Pokémon is now Confused.");
        assertEquals(1, results.size());
        assertEquals("self", results.get(0).getParams().get("target"));
    }

    @Test
    void parseSpecialConditions_withNoCondition_returnsEmptyList() {
        List<AttackEffect> results = TextEffectParser.parseSpecialConditions(
                "Deal 40 damage.");
        assertTrue(results.isEmpty());
    }

    // ─────────────────────────────────────────────
    // parseHeal()
    // ─────────────────────────────────────────────

    @Test
    void parseHeal_withHeal30Damage_returnsCount3() {
        AttackEffect result = TextEffectParser.parseHeal("Heal 30 damage from this Pokémon.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("count"));
    }

    @Test
    void parseHeal_withHeal60Damage_returnsCount6() {
        AttackEffect result = TextEffectParser.parseHeal("Heal 60 damage from this Pokémon.");
        assertNotNull(result);
        assertEquals(6, result.getParams().get("count"));
    }

    @Test
    void parseHeal_withRemoveAllDamageAndConditions_setsClearConditions() {
        AttackEffect result = TextEffectParser.parseHeal(
                "Remove all damage and all conditions from this Pokémon.");
        assertNotNull(result);
        assertTrue((Boolean) result.getParams().get("clearConditions"));
    }

    @Test
    void parseHeal_withAllDamage_setsHealFull() {
        AttackEffect result = TextEffectParser.parseHeal(
                "Heal all damage from this Pokémon.");
        assertNotNull(result);
        assertTrue((Boolean) result.getParams().get("healFull"));
    }

    @Test
    void parseHeal_withBenchedTarget_setsTargetBench() {
        AttackEffect result = TextEffectParser.parseHeal(
                "Heal 30 damage from 1 of your Benched Pokémon.");
        assertNotNull(result);
        assertTrue((Boolean) result.getParams().get("targetBench"));
    }

    @Test
    void parseHeal_withoutHeal_returnsNull() {
        assertNull(TextEffectParser.parseHeal("Deal 40 damage."));
    }

    @Test
    void parseHeal_withRemoveAllConditions_setsClearConditions() {
        AttackEffect result = TextEffectParser.parseHeal(
                "Remove all conditions from the Defending Pokémon.");
        assertNotNull(result);
        assertEquals(0, result.getParams().get("count"));
        assertTrue((Boolean) result.getParams().get("clearConditions"));
    }

    // ─────────────────────────────────────────────
    // parseBenchDamage()
    // ─────────────────────────────────────────────

    @Test
    void parseBenchDamage_withOpponentBenched_returnsCorrectDamage() {
        AttackEffect result = TextEffectParser.parseBenchDamage(
                "This attack does 30 damage to 1 of your opponent's Benched Pokémon.");
        assertNotNull(result);
        assertEquals(30, result.getParams().get("damage"));
        assertEquals(AttackEffectType.DAMAGE_BENCH, result.getType());
    }

    @Test
    void parseBenchDamage_withOwnBench_setsOwnBench() {
        AttackEffect result = TextEffectParser.parseBenchDamage(
                "Does 10 damage to each of your Benched Pokémon.");
        assertNotNull(result);
        assertEquals(10, result.getParams().get("damage"));
        assertTrue((Boolean) result.getParams().get("ownBench"));
    }

    @Test
    void parseBenchDamage_withMultipleTargets_returnsCorrectDamage() {
        AttackEffect result = TextEffectParser.parseBenchDamage(
                "Does 20 damage to 2 of your opponent's Benched Pokémon.");
        assertNotNull(result);
        assertEquals(20, result.getParams().get("damage"));
    }

    @Test
    void parseBenchDamage_withoutBenchDamage_returnsNull() {
        assertNull(TextEffectParser.parseBenchDamage("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseDiscardEnergy()
    // ─────────────────────────────────────────────

    @Test
    void parseDiscardEnergy_fromDefender_returnsDefenderTarget() {
        AttackEffect result = TextEffectParser.parseDiscardEnergy(
                "Discard 2 Energy from the Defending Pokémon.");
        assertNotNull(result);
        assertEquals(2, result.getParams().get("count"));
        assertEquals("defender", result.getParams().get("target"));
    }

    @Test
    void parseDiscardEnergy_fromSelf_returnsAttackerTarget() {
        AttackEffect result = TextEffectParser.parseDiscardEnergy(
                "Discard 2 Energy from this Pokémon.");
        assertNotNull(result);
        assertEquals(2, result.getParams().get("count"));
        assertEquals("attacker", result.getParams().get("target"));
    }

    @Test
    void parseDiscardEnergy_allDiscard_returnsCount99() {
        AttackEffect result = TextEffectParser.parseDiscardEnergy(
                "Discard all Energy from the Defending Pokémon.");
        assertNotNull(result);
        assertEquals(99, result.getParams().get("count"));
    }

    @Test
    void parseDiscardEnergy_withAnEnergy_returnsCount1() {
        AttackEffect result = TextEffectParser.parseDiscardEnergy(
                "Discard an Energy from the Defending Pokémon.");
        assertNotNull(result);
        assertEquals(1, result.getParams().get("count"));
    }

    @Test
    void parseDiscardEnergy_withOptional_setsOptional() {
        AttackEffect result = TextEffectParser.parseDiscardEnergy(
                "You may discard 1 Energy from this Pokémon.");
        assertNotNull(result);
        assertTrue((Boolean) result.getParams().get("optional"));
    }

    @Test
    void parseDiscardEnergy_withoutDiscard_returnsNull() {
        assertNull(TextEffectParser.parseDiscardEnergy("Heal 30 damage."));
    }

    // ─────────────────────────────────────────────
    // parseDrawCards()
    // ─────────────────────────────────────────────

    @Test
    void parseDrawCards_withNumber_returnsCorrectCount() {
        AttackEffect result = TextEffectParser.parseDrawCards("Draw 3 cards.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("count"));
        assertEquals(AttackEffectType.DRAW_CARDS, result.getType());
    }

    @Test
    void parseDrawCards_withADraw_returnsCount1() {
        AttackEffect result = TextEffectParser.parseDrawCards("Draw a card.");
        assertNotNull(result);
        assertEquals(1, result.getParams().get("count"));
    }

    @Test
    void parseDrawCards_withSearchDeck_returnsNull() {
        assertNull(TextEffectParser.parseDrawCards(
                "Search your deck for a Pokémon and draw it."));
    }

    @Test
    void parseDrawCards_withoutDraw_returnsNull() {
        assertNull(TextEffectParser.parseDrawCards("Deal 40 damage."));
    }

    @Test
    void parseDrawCards_withNumberAbove10_returnsNull() {
        assertNull(TextEffectParser.parseDrawCards("Draw 99 cards."));
    }

    // ─────────────────────────────────────────────
    // parseSearchDeck()
    // ─────────────────────────────────────────────

    @Test
    void parseSearchDeck_forBasicPokemon_returnsBasicSearchType() {
        AttackEffect result = TextEffectParser.parseSearchDeck(
                "Search your deck for a Basic Pokémon and put it onto your Bench.");
        assertNotNull(result);
        assertEquals("BASIC_POKEMON", result.getParams().get("searchType"));
    }

    @Test
    void parseSearchDeck_forEnergy_returnsEnergySearchType() {
        AttackEffect result = TextEffectParser.parseSearchDeck(
                "Search your deck for up to 2 Fire Energy cards.");
        assertNotNull(result);
        assertEquals("ENERGY", result.getParams().get("searchType"));
    }

    @Test
    void parseSearchDeck_forSupporter_returnsSupporterSearchType() {
        AttackEffect result = TextEffectParser.parseSearchDeck(
                "Search your deck for a Supporter card.");
        assertNotNull(result);
        assertEquals("SUPPORTER", result.getParams().get("searchType"));
    }

    @Test
    void parseSearchDeck_withLightningEnergy_setsEnergyType() {
        AttackEffect result = TextEffectParser.parseSearchDeck(
                "Search your deck for a Lightning Energy.");
        assertNotNull(result);
        assertEquals("ENERGY", result.getParams().get("searchType"));
        assertEquals("LIGHTNING", result.getParams().get("energyType"));
    }

    @Test
    void parseSearchDeck_withoutSearchKeyword_returnsNull() {
        assertNull(TextEffectParser.parseSearchDeck("Draw 2 cards."));
    }

    @Test
    void parseSearchDeck_withCount_returnsCorrectCount() {
        AttackEffect result = TextEffectParser.parseSearchDeck(
                "Search your deck for up to 3 Basic Pokémon.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("count"));
    }

    // ─────────────────────────────────────────────
    // parseAttachEnergy()
    // ─────────────────────────────────────────────

    @Test
    void parseAttachEnergy_fromDeck_returnsDeckSource() {
        AttackEffect result = TextEffectParser.parseAttachEnergy(
                "Attach a basic Energy card from your deck to this Pokémon.");
        assertNotNull(result);
        assertEquals("deck", result.getParams().get("source"));
        assertEquals(1, result.getParams().get("count"));
    }

    @Test
    void parseAttachEnergy_fromDiscard_returnsDiscardSource() {
        AttackEffect result = TextEffectParser.parseAttachEnergy(
                "Attach a basic Energy card from your discard pile to this Pokémon.");
        assertNotNull(result);
        assertEquals("discard", result.getParams().get("source"));
    }

    @Test
    void parseAttachEnergy_withFireType_setsEnergyType() {
        AttackEffect result = TextEffectParser.parseAttachEnergy(
                "Attach a Fire Energy card from your deck to this Pokémon.");
        assertNotNull(result);
        assertEquals("FIRE", result.getParams().get("energyType"));
    }

    @Test
    void parseAttachEnergy_toBench_setsTargetBench() {
        AttackEffect result = TextEffectParser.parseAttachEnergy(
                "Attach a basic Energy card from your deck to 1 of your Benched Pokémon.");
        assertNotNull(result);
        assertEquals("bench", result.getParams().get("target"));
    }

    @Test
    void parseAttachEnergy_withoutEnergyKeyword_returnsNull() {
        assertNull(TextEffectParser.parseAttachEnergy("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseMoveEnergy()
    // ─────────────────────────────────────────────

    @Test
    void parseMoveEnergy_fromAttackerToBench_returnsCorrectTargets() {
        AttackEffect result = TextEffectParser.parseMoveEnergy(
                "Move 1 Energy from this Pokémon to 1 of your Benched Pokémon.");
        assertNotNull(result);
        assertEquals("attacker", result.getParams().get("sourcePokemon"));
        assertEquals("ownBench", result.getParams().get("targetPokemon"));
        assertEquals(1, result.getParams().get("count"));
    }

    @Test
    void parseMoveEnergy_fromDefenderToOpponentBench_returnsCorrectTargets() {
        AttackEffect result = TextEffectParser.parseMoveEnergy(
                "Move 1 Energy from your opponent's Active Pokémon to 1 of their Benched Pokémon.");
        assertNotNull(result);
        assertEquals("defender", result.getParams().get("sourcePokemon"));
        assertEquals("opponentBench", result.getParams().get("targetPokemon"));
    }

    @Test
    void parseMoveEnergy_withoutMoveKeyword_returnsNull() {
        assertNull(TextEffectParser.parseMoveEnergy("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseDamagePrevention()
    // ─────────────────────────────────────────────

    @Test
    void parseDamagePrevention_preventAllDamage_returnsEffect() {
        AttackEffect result = TextEffectParser.parseDamagePrevention(
                "Prevent all damage done to this Pokémon during your opponent's next turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.DAMAGE_PREVENTION, result.getType());
    }

    @Test
    void parseDamagePrevention_withThreshold_setsThreshold() {
        AttackEffect result = TextEffectParser.parseDamagePrevention(
                "If that damage is 30 or less, prevent all damage done to this Pokémon.");
        assertNotNull(result);
        assertEquals(30, result.getParams().get("threshold"));
    }

    @Test
    void parseDamagePrevention_withCoinFlip_returnsNull() {
        assertNull(TextEffectParser.parseDamagePrevention(
                "Flip a coin. If heads, prevent all damage."));
    }

    @Test
    void parseDamagePrevention_withoutPrevent_returnsNull() {
        assertNull(TextEffectParser.parseDamagePrevention("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseCannotAttackNextTurn()
    // ─────────────────────────────────────────────

    @Test
    void parseCannotAttackNextTurn_defenderCannotAttack_returnsDefenderEffect() {
        AttackEffect result = TextEffectParser.parseCannotAttackNextTurn(
                "The Defending Pokémon can't attack during your opponent's next turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.DEFENDER_CANNOT_ATTACK, result.getType());
    }

    @Test
    void parseCannotAttackNextTurn_selfCannotAttack_returnsSelfEffect() {
        AttackEffect result = TextEffectParser.parseCannotAttackNextTurn(
                "This Pokémon can't attack during your next turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.CANNOT_ATTACK_NEXT_TURN, result.getType());
    }

    @Test
    void parseCannotAttackNextTurn_withCoinFlip_returnsNull() {
        assertNull(TextEffectParser.parseCannotAttackNextTurn(
                "Flip a coin. If heads, the Defending Pokémon can't attack."));
    }

    @Test
    void parseCannotAttackNextTurn_withoutKeyword_returnsNull() {
        assertNull(TextEffectParser.parseCannotAttackNextTurn("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseSupporterLock()
    // ─────────────────────────────────────────────

    @Test
    void parseSupporterLock_withSupporterLock_returnsEffect() {
        AttackEffect result = TextEffectParser.parseSupporterLock(
                "Your opponent can't play any Supporter cards during their next turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.SUPPORTER_LOCK, result.getType());
    }

    @Test
    void parseSupporterLock_withCoinFlip_returnsNull() {
        assertNull(TextEffectParser.parseSupporterLock(
                "Flip a coin. If heads, your opponent can't play Supporter cards."));
    }

    @Test
    void parseSupporterLock_withoutKeyword_returnsNull() {
        assertNull(TextEffectParser.parseSupporterLock("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseOpponentDiscardHand()
    // ─────────────────────────────────────────────

    @Test
    void parseOpponentDiscardHand_withCount_returnsCorrectCount() {
        AttackEffect result = TextEffectParser.parseOpponentDiscardHand(
                "Your opponent discards 2 cards from their hand.");
        assertNotNull(result);
        assertEquals(2, result.getParams().get("count"));
        assertEquals(AttackEffectType.OPPONENT_DISCARD_HAND, result.getType());
    }

    @Test
    void parseOpponentDiscardHand_withoutOpponent_returnsNull() {
        AttackEffect result = TextEffectParser.parseOpponentDiscardHand(
                "Discard 2 cards from your hand.");
        assertNull(result);
    }

    @Test
    void parseOpponentDiscardHand_withCoinFlip_returnsNull() {
        assertNull(TextEffectParser.parseOpponentDiscardHand(
                "Flip a coin. If heads, your opponent discards a card from their hand."));
    }

    // ─────────────────────────────────────────────
    // parseNextTurnDamageBonus()
    // ─────────────────────────────────────────────

    @Test
    void parseNextTurnDamageBonus_withBonus_returnsCorrectBonus() {
        AttackEffect result = TextEffectParser.parseNextTurnDamageBonus(
                "During your next turn, this Pokémon's attacks do 30 more damage.");
        assertNotNull(result);
        assertEquals(30, result.getParams().get("bonus"));
        assertEquals(AttackEffectType.NEXT_TURN_DAMAGE_BONUS, result.getType());
    }

    @Test
    void parseNextTurnDamageBonus_withoutNextTurn_returnsNull() {
        assertNull(TextEffectParser.parseNextTurnDamageBonus("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseRetreatLock()
    // ─────────────────────────────────────────────

    @Test
    void parseRetreatLock_withCannotRetreat_returnsEffect() {
        AttackEffect result = TextEffectParser.parseRetreatLock(
                "The Defending Pokémon can't retreat during your opponent's next turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.RETREAT_LOCK, result.getType());
    }

    @Test
    void parseRetreatLock_withoutKeyword_returnsNull() {
        assertNull(TextEffectParser.parseRetreatLock("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseDamageReduction()
    // ─────────────────────────────────────────────

    @Test
    void parseDamageReduction_withReduction_returnsCorrectReduction() {
        AttackEffect result = TextEffectParser.parseDamageReduction(
                "During your opponent's next turn, damage done to this Pokémon is reduced by 20.");
        assertNotNull(result);
        assertEquals(20, result.getParams().get("reduction"));
        assertEquals(AttackEffectType.DAMAGE_REDUCTION, result.getType());
    }

    @Test
    void parseDamageReduction_withCustomValue_returnsCustomReduction() {
        AttackEffect result = TextEffectParser.parseDamageReduction(
                "During your opponent's next turn, damage done to this Pokémon is reduced by 40.");
        assertNotNull(result);
        assertEquals(40, result.getParams().get("reduction"));
    }

    @Test
    void parseDamageReduction_withoutNextTurn_returnsNull() {
        assertNull(TextEffectParser.parseDamageReduction("Reduced by 20 damage."));
    }

    // ─────────────────────────────────────────────
    // parseDiscardOpponentDeck()
    // ─────────────────────────────────────────────

    @Test
    void parseDiscardOpponentDeck_opponentTarget_returnsOpponent() {
        AttackEffect result = TextEffectParser.parseDiscardOpponentDeck(
                "Discard the top card of your opponent's deck.");
        assertNotNull(result);
        assertEquals("opponent", result.getParams().get("target"));
        assertEquals(AttackEffectType.DISCARD_OPPONENT_DECK, result.getType());
    }

    @Test
    void parseDiscardOpponentDeck_selfTarget_returnsSelf() {
        AttackEffect result = TextEffectParser.parseDiscardOpponentDeck(
                "Discard the top card of your deck.");
        assertNotNull(result);
        assertEquals("self", result.getParams().get("target"));
    }

    @Test
    void parseDiscardOpponentDeck_withoutKeyword_returnsNull() {
        assertNull(TextEffectParser.parseDiscardOpponentDeck("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseSearchDiscard()
    // ─────────────────────────────────────────────

    @Test
    void parseSearchDiscard_fromDiscardPile_returnsEffect() {
        AttackEffect result = TextEffectParser.parseSearchDiscard(
                "Put 2 Item cards from your discard pile into your hand.");
        assertNotNull(result);
        assertEquals(2, result.getParams().get("count"));
        assertEquals("ITEM", result.getParams().get("cardType"));
        assertEquals(AttackEffectType.SEARCH_DISCARD, result.getType());
    }

    @Test
    void parseSearchDiscard_withoutDiscardPile_returnsNull() {
        assertNull(TextEffectParser.parseSearchDiscard(
                "Search your deck for a Pokémon."));
    }

    // ─────────────────────────────────────────────
    // parseRecycle()
    // ─────────────────────────────────────────────

    @Test
    void parseRecycle_fromDiscardToDeck_returnsEffect() {
        AttackEffect result = TextEffectParser.parseRecycle(
                "Put 2 Energy cards from your discard pile on top of your deck.");
        assertNotNull(result);
        assertEquals(AttackEffectType.RECYCLE_FROM_DISCARD, result.getType());
    }

    @Test
    void parseRecycle_withoutDiscardPile_returnsNull() {
        assertNull(TextEffectParser.parseRecycle("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseOpponentShuffleDraw()
    // ─────────────────────────────────────────────

    @Test
    void parseOpponentShuffleDraw_returnsDefaultCount4() {
        AttackEffect result = TextEffectParser.parseOpponentShuffleDraw(
                "Your opponent shuffles their hand into their deck and draws cards.");
        assertNotNull(result);
        assertEquals(4, result.getParams().get("count"));
        assertEquals(AttackEffectType.OPPONENT_SHUFFLE_DRAW, result.getType());
    }

    @Test
    void parseOpponentShuffleDraw_withCustomCount_returnsCustomCount() {
        AttackEffect result = TextEffectParser.parseOpponentShuffleDraw(
                "Your opponent shuffles their hand into their deck and draws 6 cards.");
        assertNotNull(result);
        assertEquals(6, result.getParams().get("count"));
    }

    @Test
    void parseOpponentShuffleDraw_withoutOpponent_returnsNull() {
        assertNull(TextEffectParser.parseOpponentShuffleDraw(
                "Shuffle your hand into your deck."));
    }

    // ─────────────────────────────────────────────
    // parseDamageAllBench()
    // ─────────────────────────────────────────────

    @Test
    void parseDamageAllBench_withDamage_returnsCorrectDamageCounters() {
        AttackEffect result = TextEffectParser.parseDamageAllBench(
                "This attack does 20 damage to each of your opponent's Benched Pokémon.");
        assertNotNull(result);
        assertEquals(2, result.getParams().get("damageCounters"));
        assertEquals(AttackEffectType.DAMAGE_ALL_BENCH, result.getType());
    }

    @Test
    void parseDamageAllBench_withDamageCounters_returnsCorrectCounters() {
        AttackEffect result = TextEffectParser.parseDamageAllBench(
                "Put 3 damage counters on each of your opponent's Pokémon.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("damageCounters"));
    }

    @Test
    void parseDamageAllBench_withoutAllBench_returnsNull() {
        assertNull(TextEffectParser.parseDamageAllBench("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseSwitch()
    // ─────────────────────────────────────────────

    @Test
    void parseSwitch_selfSwitch_returnsSwitchAttackerTrue() {
        List<AttackEffect> results = TextEffectParser.parseSwitch(
                "Switch this Pokémon with 1 of your Benched Pokémon.");
        assertEquals(1, results.size());
        assertEquals(AttackEffectType.SWITCH_AFTER_DAMAGE, results.get(0).getType());
        assertTrue((Boolean) results.get(0).getParams().get("switchAttacker"));
    }

    @Test
    void parseSwitch_opponentSwitch_returnsSwitchAttackerFalse() {
        List<AttackEffect> results = TextEffectParser.parseSwitch(
                "Your opponent switches their Active Pokémon with 1 of their Benched Pokémon.");
        assertEquals(1, results.size());
        assertEquals(AttackEffectType.SWITCH_AFTER_DAMAGE, results.get(0).getType());
        assertFalse((Boolean) results.get(0).getParams().get("switchAttacker"));
    }

    @Test
    void parseSwitch_withoutSwitch_returnsEmptyList() {
        assertTrue(TextEffectParser.parseSwitch("Deal 40 damage.").isEmpty());
    }

    // ─────────────────────────────────────────────
    // parseRecoil()
    // ─────────────────────────────────────────────

    @Test
    void parseRecoil_withSelfDamage_returnsCorrectCounters() {
        AttackEffect result = TextEffectParser.parseRecoil(
                "This Pokémon does 30 damage to itself.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("count"));
        assertEquals(AttackEffectType.RECOIL, result.getType());
    }

    @Test
    void parseRecoil_withoutRecoil_returnsNull() {
        assertNull(TextEffectParser.parseRecoil("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseAbilitySuppression()
    // ─────────────────────────────────────────────

    @Test
    void parseAbilitySuppression_suppressDefender_returnsEffect() {
        AttackEffect result = TextEffectParser.parseAbilitySuppression(
                "The Defending Pokémon has no Abilities until the end of your turn.");
        assertNotNull(result);
        assertEquals(AttackEffectType.ABILITY_SUPPRESSION, result.getType());
    }

    @Test
    void parseAbilitySuppression_withoutKeyword_returnsNull() {
        assertNull(TextEffectParser.parseAbilitySuppression("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseToolDiscard()
    // ─────────────────────────────────────────────

    @Test
    void parseToolDiscard_discardOpponentTool_returnsEffect() {
        AttackEffect result = TextEffectParser.parseToolDiscard(
                "Discard a Pokémon Tool from your opponent's Active Pokémon.");
        assertNotNull(result);
        assertEquals(AttackEffectType.DISCARD_TOOL, result.getType());
    }

    @Test
    void parseToolDiscard_withoutOpponent_returnsNull() {
        assertNull(TextEffectParser.parseToolDiscard(
                "Discard a Tool from your own Pokémon."));
    }

    // ─────────────────────────────────────────────
    // parseReorderDeck()
    // ─────────────────────────────────────────────

    @Test
    void parseReorderDeck_lookAtTopAndPutBack_returnsEffect() {
        AttackEffect result = TextEffectParser.parseReorderDeck(
                "Look at the top 3 cards of your deck and put them back in any order.");
        assertNotNull(result);
        assertEquals(3, result.getParams().get("count"));
        assertEquals(AttackEffectType.REORDER_DECK, result.getType());
    }

    @Test
    void parseReorderDeck_withoutReorder_returnsNull() {
        assertNull(TextEffectParser.parseReorderDeck("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parsePeekOpponentDeck()
    // ─────────────────────────────────────────────

    @Test
    void parsePeekOpponentDeck_lookAtTopCard_returnsEffect() {
        AttackEffect result = TextEffectParser.parsePeekOpponentDeck(
                "Look at the top card of your opponent's deck.");
        assertNotNull(result);
        assertEquals(AttackEffectType.PEEK_OPPONENT_DECK, result.getType());
    }

    @Test
    void parsePeekOpponentDeck_withoutLook_returnsNull() {
        assertNull(TextEffectParser.parsePeekOpponentDeck("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseOpponentRandomDiscard()
    // ─────────────────────────────────────────────

    @Test
    void parseOpponentRandomDiscard_withRandomCard_returnsEffect() {
        AttackEffect result = TextEffectParser.parseOpponentRandomDiscard(
                "Your opponent shuffles their hand and discards 1 random card.");
        assertNotNull(result);
        assertEquals(AttackEffectType.OPPONENT_RANDOM_DISCARD, result.getType());
    }

    @Test
    void parseOpponentRandomDiscard_withoutRandom_returnsNull() {
        assertNull(TextEffectParser.parseOpponentRandomDiscard("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseSetHp()
    // ─────────────────────────────────────────────

    @Test
    void parseSetHp_withRemainingHp_returnsEffect() {
        AttackEffect result = TextEffectParser.parseSetHp(
                "The Defending Pokémon's remaining HP becomes 10.");
        assertNotNull(result);
        assertEquals(10, result.getParams().get("targetHp"));
        assertEquals(AttackEffectType.SET_HP, result.getType());
    }

    @Test
    void parseSetHp_withoutRemainingHp_returnsNull() {
        assertNull(TextEffectParser.parseSetHp("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseMentalPanic()
    // ─────────────────────────────────────────────

    @Test
    void parseMentalPanic_withDefendingTriesToAttack_returnsEffect() {
        AttackEffect result = TextEffectParser.parseMentalPanic(
                "If the Defending Pokémon tries to attack during your opponent's next turn, "
                + "it flips a coin. If tails, that attack does nothing.");
        assertNotNull(result);
        assertEquals(AttackEffectType.MENTAL_PANIC, result.getType());
    }

    @Test
    void parseMentalPanic_withoutDefendingKeyword_returnsNull() {
        assertNull(TextEffectParser.parseMentalPanic("Deal 40 damage."));
    }

    // ─────────────────────────────────────────────
    // parseCoinFlipBeforeDamage()
    // ─────────────────────────────────────────────

    @Test
    void parseCoinFlipBeforeDamage_cancelAttack_returnsCancelEffect() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipBeforeDamage(
                "Flip a coin. If tails, this attack does nothing.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("CANCEL_ATTACK", params.get("effectType"));
    }

    @Test
    void parseCoinFlipBeforeDamage_damageMultiplier_returnsMultiplier() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipBeforeDamage(
                "Flip a coin. If heads, this attack does 40 more damage.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("DAMAGE_BONUS", params.get("effectType"));
    }

    @Test
    void parseCoinFlipBeforeDamage_damageTimesHeads_returnsMultiplier() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipBeforeDamage(
                "Flip 2 coins. Does 40 damage times the number of heads.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("DAMAGE_MULTIPLIER", params.get("effectType"));
    }

    @Test
    void parseCoinFlipBeforeDamage_damageForEachHeads_returnsMultiplier() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipBeforeDamage(
                "Flip 2 coins. Does 30 more damage for each heads.");
        assertFalse(results.isEmpty());
        assertEquals("DAMAGE_MULTIPLIER", results.get(0).getParams().get("effectType"));
    }

    @Test
    void parseCoinFlipBeforeDamage_withoutCoinFlip_returnsEmptyList() {
        assertTrue(TextEffectParser.parseCoinFlipBeforeDamage("Deal 40 damage.").isEmpty());
    }

    // ─────────────────────────────────────────────
    // parseCoinFlipAfterDamage()
    // ─────────────────────────────────────────────

    @Test
    void parseCoinFlipAfterDamage_flipUntilTails_returnsFlipUntilTails() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipAfterDamage(
                "Flip a coin until you get tails.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("FLIP_UNTIL_TAILS", params.get("effectType"));
    }

    @Test
    void parseCoinFlipAfterDamage_conditionOnHeads_returnsConditionEffect() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipAfterDamage(
                "Flip a coin. If heads, the Defending Pokémon is now Paralyzed.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("APPLY_SPECIAL_CONDITION", params.get("effectType"));
        assertEquals("PARALYZED", params.get("effectParam"));
        assertEquals("true", params.get("applyOnHeads"));
    }

    @Test
    void parseCoinFlipAfterDamage_recoilOnTails_returnsRecoilEffect() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipAfterDamage(
                "Flip a coin. If tails, this Pokémon does 30 damage to itself.");
        assertFalse(results.isEmpty());
        Map<String, Object> params = results.get(0).getParams();
        assertEquals("RECOIL", params.get("effectType"));
    }

    @Test
    void parseCoinFlipAfterDamage_preventionOnNextTurn_returnsPrevention() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipAfterDamage(
                "Flip a coin. If heads, prevent all damage done to this Pokémon "
                + "during your opponent's next turn.");
        assertFalse(results.isEmpty());
        assertEquals("DAMAGE_PREVENTION", results.get(0).getParams().get("effectType"));
    }

    @Test
    void parseCoinFlipAfterDamage_dualConditions_returnsTwoEffects() {
        List<AttackEffect> results = TextEffectParser.parseCoinFlipAfterDamage(
                "Flip a coin. If heads, the Defending Pokémon is now Asleep. "
                + "If tails, instead it is Confused.");
        assertEquals(2, results.size());
    }

    @Test
    void parseCoinFlipAfterDamage_withoutCoinFlip_returnsEmptyList() {
        assertTrue(TextEffectParser.parseCoinFlipAfterDamage("Deal 40 damage.").isEmpty());
    }
}
