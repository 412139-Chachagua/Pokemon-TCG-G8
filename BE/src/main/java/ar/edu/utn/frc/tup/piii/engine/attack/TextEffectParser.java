package ar.edu.utn.frc.tup.piii.engine.attack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEffectParser {

    private TextEffectParser() {}

    public static List<AttackEffect> parse(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = text.trim().replaceAll("\\s+", " ");
        List<AttackEffect> results = new ArrayList<>();

        results.addAll(parseCoinFlipBeforeDamage(normalized));
        results.addAll(parseCoinFlipAfterDamage(normalized));

        for (AttackEffect sc : parseSpecialConditions(normalized)) {
            if (!alreadyHasCoinFlipResultFor(results, "APPLY_SPECIAL_CONDITION")) {
                results.add(sc);
            }
        }

        AttackEffect heal = parseHeal(normalized);
        if (heal != null && !alreadyHasCoinFlipResultFor(results, "HEAL_USER")) {
            results.add(heal);
        }

        AttackEffect benchDmg = parseBenchDamage(normalized);
        if (benchDmg != null && !alreadyHasCoinFlipResultFor(results, "DAMAGE_BENCH")) {
            results.add(benchDmg);
        }

        AttackEffect discard = parseDiscardEnergy(normalized);
        if (discard != null && !alreadyHasCoinFlipResultFor(results, "DISCARD_ENERGY")) {
            results.add(discard);
        }

        AttackEffect draw = parseDrawCards(normalized);
        if (draw != null && !alreadyHasCoinFlipResultFor(results, "DRAW_CARDS")) {
            results.add(draw);
        }

        AttackEffect search = parseSearchDeck(normalized);
        if (search != null && !alreadyHasCoinFlipResultFor(results, "SEARCH_DECK")) {
            results.add(search);
        }

        AttackEffect attach = parseAttachEnergy(normalized);
        if (attach != null && !alreadyHasCoinFlipResultFor(results, "ATTACH_ENERGY")) {
            results.add(attach);
        }

        AttackEffect move = parseMoveEnergy(normalized);
        if (move != null && !alreadyHasCoinFlipResultFor(results, "MOVE_ENERGY")) {
            results.add(move);
        }

        AttackEffect prevention = parseDamagePrevention(normalized);
        if (prevention != null && !alreadyHasCoinFlipResultFor(results, "DAMAGE_PREVENTION")) {
            results.add(prevention);
        }

        AttackEffect cannotAttack = parseCannotAttackNextTurn(normalized);
        if (cannotAttack != null) {
            results.add(cannotAttack);
        }

        AttackEffect supporterLock = parseSupporterLock(normalized);
        if (supporterLock != null) {
            results.add(supporterLock);
        }

        AttackEffect opponentDiscard = parseOpponentDiscardHand(normalized);
        if (opponentDiscard != null) {
            results.add(opponentDiscard);
        }

        AttackEffect damageBonus = parseNextTurnDamageBonus(normalized);
        if (damageBonus != null) {
            results.add(damageBonus);
        }

        AttackEffect retreatLock = parseRetreatLock(normalized);
        if (retreatLock != null && !alreadyHasCoinFlipResultFor(results, "RETREAT_LOCK")) {
            results.add(retreatLock);
        }

        AttackEffect damageReduction = parseDamageReduction(normalized);
        if (damageReduction != null && !alreadyHasCoinFlipResultFor(results, "DAMAGE_REDUCTION")) {
            results.add(damageReduction);
        }

        AttackEffect discardDeck = parseDiscardOpponentDeck(normalized);
        // Skip raw DISCARD_OPPONENT_DECK if the text also contains coin flip keywords
        // ("heads"/"tails") — those are handled by parseCoinFlipAfterDamage with proper
        // conditional logic (e.g. Mad Mountain: "Flip 2 coins. If both of them are heads...")
        if (discardDeck != null && (!normalized.toLowerCase().contains("heads")
                && !normalized.toLowerCase().contains("tails"))) {
            results.add(discardDeck);
        }

        AttackEffect searchDiscard = parseSearchDiscard(normalized);
        if (searchDiscard != null) {
            results.add(searchDiscard);
        }

        AttackEffect recycle = parseRecycle(normalized);
        if (recycle != null) {
            results.add(recycle);
        }

        AttackEffect oppShuffle = parseOpponentShuffleDraw(normalized);
        if (oppShuffle != null) {
            results.add(oppShuffle);
        }

        AttackEffect allBenchDmg = parseDamageAllBench(normalized);
        if (allBenchDmg != null) {
            results.add(allBenchDmg);
        }

        List<AttackEffect> switchEffects = parseSwitch(normalized);
        for (AttackEffect sw : switchEffects) {
            if (!alreadyHasCoinFlipResultFor(results, "SWITCH_AFTER_DAMAGE")) {
                results.add(sw);
            }
        }

        AttackEffect recoil = parseRecoil(normalized);
        if (recoil != null && !alreadyHasCoinFlipResultFor(results, "RECOIL")) {
            results.add(recoil);
        }

        AttackEffect abilitySuppression = parseAbilitySuppression(normalized);
        if (abilitySuppression != null) {
            results.add(abilitySuppression);
        }

        AttackEffect toolDiscard = parseToolDiscard(normalized);
        if (toolDiscard != null) {
            results.add(toolDiscard);
        }

        AttackEffect mentalPanic = parseMentalPanic(normalized);
        if (mentalPanic != null) {
            results.add(mentalPanic);
        }

        AttackEffect reorder = parseReorderDeck(normalized);
        if (reorder != null) {
            results.add(reorder);
        }

        AttackEffect peek = parsePeekOpponentDeck(normalized);
        if (peek != null) {
            results.add(peek);
        }

        AttackEffect randomDiscard = parseOpponentRandomDiscard(normalized);
        if (randomDiscard != null) {
            results.add(randomDiscard);
        }

        AttackEffect setHp = parseSetHp(normalized);
        if (setHp != null) {
            results.add(setHp);
        }

        return results;
    }

    private static boolean alreadyHasCoinFlipResultFor(List<AttackEffect> results, String effectType) {
        for (AttackEffect ae : results) {
            if (ae.getType() == AttackEffectType.COIN_FLIP_AFTER_DAMAGE || ae.getType() == AttackEffectType.COIN_FLIP_BEFORE_DAMAGE) {
                Map<String, Object> params = ae.getParams();
                if (params != null && effectType.equals(params.get("effectType"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<AttackEffect> parseSpecialConditions(String text) {
        String lower = text.toLowerCase();
        List<AttackEffect> results = new ArrayList<>();

        boolean paralyzed = lower.contains("paralyzed");
        boolean poisoned = lower.contains("poisoned");
        boolean confused = lower.contains("confused");
        boolean asleep = lower.contains("asleep");
        boolean burned = lower.contains("burned");
        boolean bothActive = lower.contains("both active pok\u00e9mon") || lower.contains("both active pokemon");
        boolean selfTarget = lower.contains("this pok\u00e9mon is now") || lower.contains("this pokemon is now")
                || (lower.contains("this pok\u00e9mon") && lower.contains("is now"))
                || (lower.contains("this pokemon") && lower.contains("is now"));
        boolean hasChooseEither = lower.contains("choose either") || lower.contains("choose between");

        // Handle "Choose either X or Y" patterns (e.g. Vivillon Conversion Powder).
        // Add both conditions as a choice; frontend sends the picked one via payload.
        if (hasChooseEither && asleep && poisoned) {
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "ASLEEP", "choice", "either", "text", text)));
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "POISONED", "choice", "either", "text", text)));
            return results;
        }

        if (paralyzed && poisoned && bothActive) {
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "PARALYZED", "target", "both", "text", text)));
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "POISONED", "target", "both", "text", text)));
        } else if (paralyzed && poisoned) {
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "PARALYZED", "text", text)));
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "POISONED", "text", text)));
        } else if (paralyzed) {
            String target = bothActive ? "both" : (selfTarget ? "self" : "defender");
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "PARALYZED", "target", target, "text", text)));
        } else if (poisoned) {
            String target = bothActive ? "both" : (selfTarget ? "self" : "defender");
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "POISONED", "target", target, "text", text)));
        } else if (confused) {
            String target = bothActive ? "both" : (selfTarget ? "self" : "defender");
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "CONFUSED", "target", target, "text", text)));
        } else if (asleep) {
            String target = bothActive ? "both" : (selfTarget ? "self" : "defender");
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "ASLEEP", "target", target, "text", text)));
        } else if (burned) {
            String target = bothActive ? "both" : (selfTarget ? "self" : "defender");
            results.add(new AttackEffect(AttackEffectType.APPLY_SPECIAL_CONDITION,
                    Map.of("condition", "BURNED", "target", target, "text", text)));
        }

        return results;
    }

    static AttackEffect parseHeal(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("heal") && !lower.contains("remove all") && !lower.contains("remove all")) {
            return null;
        }
        if (!lower.contains("heal") && !lower.contains("remove all conditions")) {
            // Only "remove all conditions" without "heal" - handled by parseSpecialConditions or as clear-only heal
            if (lower.contains("remove all") && lower.contains("condition")) {
                // This is a clear-conditions-only effect with no HP heal
                Map<String, Object> params = new HashMap<>();
                params.put("count", 0);
                params.put("clearConditions", true);
                // Check if clearing conditions from defender (opponent's active)
                if (lower.contains("your opponent") || lower.contains("defending")) {
                    params.put("clearDefenderConditions", true);
                }
                params.put("text", text);
                return new AttackEffect(AttackEffectType.HEAL_USER, params);
            }
            return null;
        }
        boolean targetBench = lower.contains("benched") || lower.contains("bench");
        boolean healAny = lower.contains("1 of your") && (lower.contains("pok\u00e9mon") || lower.contains("pokemon"));
        boolean healAll = lower.contains("each of your") && (lower.contains("pok\u00e9mon") || lower.contains("pokemon"));
        boolean clearConditions = lower.contains("remove all") && lower.contains("condition");
        boolean healFull = lower.contains("all damage") || lower.contains("all of its");
        boolean clearDefenderConditions = false;
        if (clearConditions && (lower.contains("your opponent") || lower.contains("defending") || lower.contains("that pok\u00e9mon") || lower.contains("that pokemon"))) {
            clearDefenderConditions = true;
        }

        Pattern p = Pattern.compile("(?i)heal\\s+(\\d+)\\s+damage");
        Matcher m = p.matcher(text);
        Map<String, Object> params = new HashMap<>();
        if (m.find()) {
            int hp = Integer.parseInt(m.group(1));
            params.put("count", hp / 10);
        } else if (clearConditions) {
            params.put("count", 0);
        } else {
            params.put("count", 3);
        }
        if (targetBench) params.put("targetBench", true);
        if (healAny) params.put("selectAny", true);
        if (healAll) params.put("healAll", true);
        if (clearConditions) params.put("clearConditions", true);
        if (clearDefenderConditions) params.put("clearDefenderConditions", true);
        if (healFull) params.put("healFull", true);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.HEAL_USER, params);
    }

    static AttackEffect parseBenchDamage(String text) {
        String lower = text.toLowerCase();
        // "N damage to 1 of your opponent's Benched"
        Pattern p = Pattern.compile("(?i)(\\d+)\\s+damage\\s+to\\s+(your\\s+opponent's\\s+)?benched");
        Matcher m = p.matcher(text);
        if (m.find()) {
            int hp = Integer.parseInt(m.group(1));
            return new AttackEffect(AttackEffectType.DAMAGE_BENCH, Map.of("damage", hp, "text", text));
        }
        // "does N damage to each of your Benched"
        p = Pattern.compile("(?i)does\\s+(\\d+)\\s+damage\\s+to\\s+each\\s+of\\s+your\\s+benched");
        m = p.matcher(text);
        if (m.find()) {
            int hp = Integer.parseInt(m.group(1));
            return new AttackEffect(AttackEffectType.DAMAGE_BENCH, Map.of("damage", hp, "ownBench", true, "text", text));
        }
        // "does N damage to 2 of your opponent's Benched"
        p = Pattern.compile("(?i)does\\s+(\\d+)\\s+damage\\s+to\\s+(\\d+)\\s+of\\s+your\\s+opponent's\\s+benched");
        m = p.matcher(text);
        if (m.find()) {
            int hp = Integer.parseInt(m.group(1));
            return new AttackEffect(AttackEffectType.DAMAGE_BENCH, Map.of("damage", hp, "text", text));
        }
        return null;
    }

    static AttackEffect parseDiscardEnergy(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard")) return null;
        String target = "defender";
        if (lower.contains("this pok\u00e9mon") || lower.contains("this pokemon") || lower.contains("attached to this") || lower.contains("from this pok\u00e9mon") || lower.contains("from this pokemon")) {
            target = "attacker";
        }
        boolean isOptional = lower.contains("you may");
        if (lower.contains("discard all")) {
            if (!lower.contains("tool")) {
                Map<String, Object> params = new HashMap<>(Map.of("count", 99, "target", target, "text", text));
                if (isOptional) params.put("optional", true);
                return new AttackEffect(AttackEffectType.DISCARD_ENERGY, params);
            }
        }
        Pattern p = Pattern.compile("(?i)discards?\\s+(\\d+|an|a)\\s+(?:(?:fire|water|lightning|grass|psychic|fighting|darkness|metal|fairy|colorless|basic)\\s+)?energy");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            int count = m.group(1).equals("an") || m.group(1).equals("a") ? 1 : Integer.parseInt(m.group(1));
            Map<String, Object> params = new HashMap<>(Map.of("count", count, "target", target, "text", text));
            if (isOptional) params.put("optional", true);
            return new AttackEffect(AttackEffectType.DISCARD_ENERGY, params);
        }
        return null;
    }

    static AttackEffect parseDrawCards(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("search your deck for")) return null;
        // "Draw N cards" or "Draw a card"
        Pattern p = Pattern.compile("(?i)draw\\s+(\\d+|a)\\s+");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            int count = m.group(1).equals("a") ? 1 : Integer.parseInt(m.group(1));
            if (count > 0 && count <= 10) {
                return new AttackEffect(AttackEffectType.DRAW_CARDS, Map.of("count", count, "text", text));
            }
        }
        return null;
    }

    static AttackEffect parseSearchDeck(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("search your deck")) return null;
        String searchType = "ANY";
        String energyType = null;
        int count = 1;
        boolean searchingForPokemon = lower.contains("search your deck for") && (lower.contains("pok\u00e9mon") || lower.contains("pokemon"));
        if (lower.contains("supporter")) searchType = "SUPPORTER";
        else if (lower.contains("basic") && !lower.contains("energy")) searchType = "BASIC_POKEMON";
        else if (searchingForPokemon) {
            // Check for specific Pokémon type search (e.g. "Grass Pokémon")
            String pokemonType = detectPokemonType(lower);
            if (pokemonType != null) {
                searchType = "POKEMON";
                energyType = pokemonType; // reuse energyType field for the type filter
            }
        }
        if (!"POKEMON".equals(searchType) && !searchingForPokemon && (lower.contains("energy") || lower.contains("fire") || lower.contains("water") || lower.contains("lightning")
                || lower.contains("grass") || lower.contains("psychic") || lower.contains("fighting")
                || lower.contains("darkness") || lower.contains("metal") || lower.contains("fairy"))) {
            searchType = "ENERGY";
            if (lower.contains("lightning")) energyType = "LIGHTNING";
            else if (lower.contains("fighting")) energyType = "FIGHTING";
            else if (lower.contains("psychic")) energyType = "PSYCHIC";
            else if (lower.contains("water")) energyType = "WATER";
            else if (lower.contains("grass")) energyType = "GRASS";
            else if (lower.contains("fire")) energyType = "FIRE";
            else if (lower.contains("darkness")) energyType = "DARKNESS";
            else if (lower.contains("metal")) energyType = "METAL";
            else if (lower.contains("fairy")) energyType = "FAIRY";
        }
        Pattern countP = Pattern.compile("(?i)(\\d+)\\s+of\\s+your\\s+benched|search\\s+your\\s+deck\\s+for\\s+(?:up\\s+to\\s+)?(\\d+)");
        Matcher m = countP.matcher(lower);
        if (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            if (g1 != null) count = Integer.parseInt(g1);
            else if (g2 != null) count = Integer.parseInt(g2);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("searchType", searchType);
        params.put("count", count);
        if (energyType != null) {
            if ("POKEMON".equals(searchType)) {
                params.put("pokemonType", energyType);
            } else {
                params.put("energyType", energyType);
            }
        }
        params.put("text", text);
        return new AttackEffect(AttackEffectType.SEARCH_DECK, params);
    }

    private static String detectPokemonType(String lower) {
        String[][] types = {
            {"grass", "GRASS"}, {"fire", "FIRE"}, {"water", "WATER"},
            {"lightning", "LIGHTNING"}, {"psychic", "PSYCHIC"}, {"fighting", "FIGHTING"},
            {"darkness", "DARKNESS"}, {"metal", "METAL"}, {"fairy", "FAIRY"},
            {"dragon", "DRAGON"}, {"colorless", "COLORLESS"}
        };
        for (String[] t : types) {
            if (lower.contains(t[0] + " pok\u00e9mon") || lower.contains(t[0] + " pokemon")) {
                return t[1];
            }
        }
        return null;
    }

    static AttackEffect parseAttachEnergy(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("attach") || (!lower.contains("energy") && !lower.contains("discard pile"))) return null;
        if (!lower.contains("your deck") && !lower.contains("discard pile")) return null;
        // Guard: Dig Out style — "If that card is a X Energy, attach it" is handled by
        // parseDiscardOpponentDeck with attachIfEnergyType, not by a standalone attach.
        if (lower.contains("discard the top") && lower.contains("if that card") && lower.contains("energy")) return null;

        String source = lower.contains("discard pile") ? "discard" : "deck";
        String energyType = null;
        if (lower.contains("lightning")) energyType = "LIGHTNING";
        else if (lower.contains("fighting")) energyType = "FIGHTING";
        else if (lower.contains("psychic")) energyType = "PSYCHIC";
        else if (lower.contains("water")) energyType = "WATER";
        else if (lower.contains("grass")) energyType = "GRASS";
        else if (lower.contains("fire")) energyType = "FIRE";
        else if (lower.contains("darkness")) energyType = "DARKNESS";
        else if (lower.contains("metal")) energyType = "METAL";
        else if (lower.contains("fairy")) energyType = "FAIRY";
        else if (lower.contains("colorless")) energyType = "COLORLESS";

        String target = "attacker";
        // Only set target to bench if "benched" appears before "switch" in the text.
        // This prevents the switch clause from incorrectly influencing the attach target
        // (e.g. Energy Glide: "attach it to this Pokémon... switch with 1 of your Benched Pokémon").
        int benchIdx = lower.indexOf("benched");
        int switchIdx = lower.indexOf("switch");
        if (benchIdx >= 0 && (switchIdx < 0 || benchIdx < switchIdx)) {
            target = "bench";
        }

        Map<String, Object> params = new HashMap<>();
        params.put("source", source);
        if (energyType != null) params.put("energyType", energyType);
        params.put("count", 1);
        if (!"attacker".equals(target)) params.put("target", target);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.ATTACH_ENERGY, params);
    }

    static AttackEffect parseMoveEnergy(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("move") || !lower.contains("energy")) return null;
        String sourcePokemon = lower.contains("your opponent") ? "defender" : "attacker";
        String targetPokemonZone = lower.contains("your opponent") ? "opponentBench" : "ownBench";
        Map<String, Object> params = new HashMap<>();
        params.put("sourcePokemon", sourcePokemon);
        params.put("targetPokemon", targetPokemonZone);
        params.put("count", 1);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.MOVE_ENERGY, params);
    }

    static AttackEffect parseDamagePrevention(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("prevent") || (!lower.contains("damage") && !lower.contains("effects of"))) return null;
        // When there's a coin flip, prevention is handled in parseCoinFlipAfterDamage
        if (lower.contains("flip") && lower.contains("coin")) return null;
        Map<String, Object> params = new HashMap<>();
        // Check for damage threshold: "if that damage is N or less"
        Pattern thresholdP = Pattern.compile("(?i)if\\s+that\\s+damage\\s+is\\s+(\\d+)\\s+or\\s+less");
        Matcher thresholdM = thresholdP.matcher(lower);
        if (thresholdM.find()) {
            params.put("threshold", Integer.parseInt(thresholdM.group(1)));
        }
        params.put("text", text);
        return new AttackEffect(AttackEffectType.DAMAGE_PREVENTION, params);
    }

    static AttackEffect parseCannotAttackNextTurn(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("can't attack") && !lower.contains("cannot attack") && !lower.contains("can't use")) return null;
        if (lower.contains("flip") && lower.contains("coin")) return null;
        boolean isDefender = lower.contains("defending pok\u00e9mon") || lower.contains("defending pokemon")
                || lower.contains("your opponent") || lower.contains("that pok\u00e9mon") || lower.contains("that pokemon");
        Map<String, Object> params = new HashMap<>();
        // Extract the specific attack name being restricted, if present
        Pattern attackNameP = Pattern.compile("(?i)can't\\s+use\\s+([a-z\']+(?:\\s+[a-z\']+){0,3})", Pattern.CASE_INSENSITIVE);
        Matcher attackNameM = attackNameP.matcher(text);
        String restrictedAttack = null;
        if (attackNameM.find()) {
            restrictedAttack = attackNameM.group(1).trim();
            if ("that attack".equalsIgnoreCase(restrictedAttack) || "that".equalsIgnoreCase(restrictedAttack)) {
                restrictedAttack = null;
            }
        }
        if (restrictedAttack != null) {
            params.put("attackName", restrictedAttack);
        }
        params.put("text", text);
        return new AttackEffect(
                isDefender ? AttackEffectType.DEFENDER_CANNOT_ATTACK : AttackEffectType.CANNOT_ATTACK_NEXT_TURN,
                params);
    }

    static AttackEffect parseSupporterLock(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("can't") && !lower.contains("cannot")) return null;
        if (!lower.contains("supporter") && !lower.contains("supporter")) return null;
        if (lower.contains("flip") && lower.contains("coin")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        return new AttackEffect(AttackEffectType.SUPPORTER_LOCK, params);
    }

    static AttackEffect parseOpponentDiscardHand(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard") || !lower.contains("hand") || !lower.contains("opponent")) return null;
        if (!lower.contains("your opponent") && !lower.contains("opponent's")) return null;
        if (lower.contains("flip") && lower.contains("coin")) return null;
        int count = 1;
        Pattern p = Pattern.compile("(?i)(\\d+)\\s+card");
        Matcher m = p.matcher(lower);
        if (m.find()) count = Integer.parseInt(m.group(1));
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.OPPONENT_DISCARD_HAND, params);
    }

    static AttackEffect parseNextTurnDamageBonus(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("next turn") || !lower.contains("more damage")) return null;
        Pattern p = Pattern.compile("(?i)(\\d+)\\s+more\\s+damage");
        Matcher m = p.matcher(lower);
        if (!m.find()) return null;
        int bonus = Integer.parseInt(m.group(1));
        Map<String, Object> params = new HashMap<>();
        params.put("bonus", bonus);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.NEXT_TURN_DAMAGE_BONUS, params);
    }

    static AttackEffect parseRetreatLock(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("can't retreat") && !lower.contains("cannot retreat")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        return new AttackEffect(AttackEffectType.RETREAT_LOCK, params);
    }

    static AttackEffect parseDamageReduction(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("reduced by") && !lower.contains("reduce")) return null;
        if (!lower.contains("next turn") && !lower.contains("opponent's")) return null;
        int reduction = 20;
        Pattern p = Pattern.compile("(?i)reduced by\\s+(\\d+)");
        Matcher m = p.matcher(lower);
        if (m.find()) reduction = Integer.parseInt(m.group(1));
        Map<String, Object> params = new HashMap<>();
        params.put("reduction", reduction);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.DAMAGE_REDUCTION, params);
    }

    static AttackEffect parseDiscardOpponentDeck(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard the top")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("count", 1);
        // Distinguish own deck from opponent's deck
        if (lower.contains("your opponent") || lower.contains("opponent's")) {
            params.put("target", "opponent");
        } else {
            params.put("target", "self");
        }
        // Dig Out style: "If that card is a X Energy, attach it"
        if (lower.contains("if that card") && lower.contains("attach")) {
            String[] energyTypes = {"fighting", "fire", "water", "lightning", "grass", "psychic", "darkness", "metal", "fairy"};
            for (String et : energyTypes) {
                if (lower.contains(et + " energy")) {
                    params.put("attachIfEnergyType", et.toUpperCase());
                    break;
                }
            }
        }
        params.put("text", text);
        return new AttackEffect(AttackEffectType.DISCARD_OPPONENT_DECK, params);
    }

    static AttackEffect parseSearchDiscard(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard pile") || !lower.contains("into your hand")) return null;
        int count = 2;
        String cardType = null;
        Pattern p = Pattern.compile("(?i)(\\d+)\\s+item");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            count = Integer.parseInt(m.group(1));
            cardType = "ITEM";
        }
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);
        if (cardType != null) params.put("cardType", cardType);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.SEARCH_DISCARD, params);
    }

    static AttackEffect parseRecycle(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard pile") || !lower.contains("on top of your deck")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        return new AttackEffect(AttackEffectType.RECYCLE_FROM_DISCARD, params);
    }

    static AttackEffect parseOpponentShuffleDraw(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("shuffles") && !lower.contains("shuffle")) return null;
        if (!lower.contains("hand") || !lower.contains("deck")) return null;
        if (!lower.contains("opponent") && !lower.contains("your opponent")) return null;
        int count = 4;
        Pattern p = Pattern.compile("(?i)draws\\s+(\\d+)");
        Matcher m = p.matcher(lower);
        if (m.find()) count = Integer.parseInt(m.group(1));
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.OPPONENT_SHUFFLE_DRAW, params);
    }

    static AttackEffect parseDamageAllBench(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("each of your opponent") && !lower.contains("all of your opponent")) return null;
        if (!lower.contains("damage") || !lower.contains("pok\u00e9mon") && !lower.contains("pokemon")) {
            // Also handle "damage counter" without "damage" keyword (e.g. "Put 2 damage counters each of your opponent's Pokémon")
            if (lower.contains("damage counter") || lower.contains("damage counters")) {
                int counters = 2;
                Pattern p = Pattern.compile("(?i)(\\d+)\\s+damage\\s+counter");
                Matcher m = p.matcher(lower);
                if (m.find()) counters = Integer.parseInt(m.group(1));
                Map<String, Object> params = new HashMap<>();
                params.put("damageCounters", counters);
                params.put("text", text);
                return new AttackEffect(AttackEffectType.DAMAGE_ALL_BENCH, params);
            }
            return null;
        }
        int counters = 2;
        Pattern p = Pattern.compile("(?i)(\\d+)\\s+damage\\s+(\\w+)");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            String unit = m.group(2).toLowerCase();
            int value = Integer.parseInt(m.group(1));
            if (unit.contains("counter")) {
                counters = value;
            } else {
                counters = value / 10;
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.put("damageCounters", counters);
        params.put("text", text);
        return new AttackEffect(AttackEffectType.DAMAGE_ALL_BENCH, params);
    }

    static List<AttackEffect> parseCoinFlipBeforeDamage(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("flip") || !lower.contains("coin")) return List.of();
        List<AttackEffect> results = new ArrayList<>();

        // Coin Flip that does nothing on tails → CANCEL_ATTACK
        // Guard: skip if this is a deferred effect (e.g. Mental Panic: "If the Defending Pokémon
        // tries to attack during your opponent's next turn..."). Those shouldn't cancel the current attack.
        boolean isDeferredFlip = lower.contains("if the defending") || lower.contains("if that pok")
                || (lower.contains("tries to attack") || lower.contains("tries to use"));
        if (!isDeferredFlip && lower.contains("tails")
                && (lower.contains("this attack does nothing") || lower.contains("that attack does nothing"))) {
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, Map.of(
                    "effectType", "CANCEL_ATTACK",
                    "text", text
            )));
        }

        // "does X more damage" → damage bonus on heads
        // Guard: skip if "for each heads" or "times the number of heads" is present,
        // because those are handled by DAMAGE_MULTIPLIER below.
        // Also skip if "discard the top" is present — conditional bonus based on card type, not coin flip.
        Pattern dmgP = Pattern.compile("(?i)does\\s+(\\d+)\\s+more\\s+damage");
        Matcher dmgM = dmgP.matcher(lower);
        if (dmgM.find() && !lower.contains("for each heads") && !lower.contains("times the number of heads") && !lower.contains("discard the top")) {
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, Map.of(
                    "effectType", "DAMAGE_BONUS",
                    "effectParam", dmgM.group(1),
                    "text", text
            )));
        }

        // "does X damage times the number of heads" → damage multiplier
        Pattern multP = Pattern.compile("(?i)does\\s+(\\d+)\\s+(?:more\\s+)?damage\\s+times\\s+the\\s+number\\s+of\\s+heads");
        Matcher multM = multP.matcher(lower);
        if (multM.find()) {
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, Map.of(
                    "effectType", "DAMAGE_MULTIPLIER",
                    "effectParam", multM.group(1),
                    "text", text
            )));
        }

        // "does X damage for each heads" → also a multiplier
        Pattern eachP = Pattern.compile("(?i)does\\s+(\\d+)\\s+(?:more\\s+)?damage\\s+for\\s+each\\s+heads");
        Matcher eachM = eachP.matcher(lower);
        if (eachM.find()) {
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, Map.of(
                    "effectType", "DAMAGE_MULTIPLIER",
                    "effectParam", eachM.group(1),
                    "text", text
            )));
        }

        return results;
    }

    static List<AttackEffect> parseCoinFlipAfterDamage(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("flip") || !lower.contains("coin")) return List.of();
        List<AttackEffect> results = new ArrayList<>();

        boolean hasFlipUntilTails = lower.contains("until you get tails") || lower.contains("until you get tails");
        boolean allHeads = lower.contains("if all of them are heads") || lower.contains("if both of them are heads");

        // "Flip until you get tails" - mark for damage calculation
        // Guard: skip if "for each heads" or "times the number of heads" is present,
        // because PreDamageStep already handles the flip-until-tails + damage bonus.
        if (hasFlipUntilTails && !lower.contains("for each heads") && !lower.contains("times the number of heads")) {
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                    "effectType", "FLIP_UNTIL_TAILS",
                    "text", text
            )));
        }

        // Coin flip + recoil on tails
        if (lower.contains("tails") && (lower.contains("damage to itself") || lower.contains("damage to this"))) {
            Pattern p = Pattern.compile("(?i)(\\d+)\\s+damage\\s+to\\s+(?:itself|this)");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                String recoilCounters = String.valueOf(Integer.parseInt(m.group(1)) / 10);
                boolean onTails = lower.contains("tails");
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "RECOIL",
                        "effectParam", recoilCounters,
                        "applyOnHeads", String.valueOf(!onTails),
                        "text", text
                )));
            }
        }

        // Coin flip + discard energy from opponent (on heads)
        if (lower.contains("discard") && lower.contains("energy") && lower.contains("opponent") && !lower.contains("this pok\u00e9mon") && !lower.contains("this pokemon")) {
            boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
            Pattern p = Pattern.compile("(?i)discards?\\s+(\\d+|an)\\s+energy");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                int count = m.group(1).equals("an") ? 1 : Integer.parseInt(m.group(1));
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "DISCARD_ENERGY",
                        "effectParam", String.valueOf(count),
                        "target", "defender",
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + discard energy from self (on tails typically)
        if ((lower.contains("discard") && lower.contains("energy") && (lower.contains("this pok\u00e9mon") || lower.contains("this pokemon") || lower.contains("attached to this"))) && (lower.contains("heads") || lower.contains("tails"))) {
            boolean onHeads = lower.contains("heads") && (!lower.contains("tails") || lower.indexOf("heads") < lower.indexOf("tails"));
            Pattern p = Pattern.compile("(?i)discards?\\s+(\\d+|an)\\s+energy");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                int count = m.group(1).equals("an") ? 1 : Integer.parseInt(m.group(1));
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "DISCARD_ENERGY",
                        "effectParam", String.valueOf(count),
                        "target", "attacker",
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + switch (e.g., Froakie "Bounce")
        if ((lower.contains("switch this pok\u00e9mon") || lower.contains("switch this pokemon")) && (lower.contains("heads") || lower.contains("tails"))) {
            boolean switchAttacker = lower.contains("switch this pok\u00e9mon") || lower.contains("switch this pokemon");
            boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                if (ae.getType() == AttackEffectType.COIN_FLIP_AFTER_DAMAGE && "SWITCH_AFTER_DAMAGE".equals(ae.getParams().get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "SWITCH_AFTER_DAMAGE",
                        "effectParam", String.valueOf(switchAttacker),
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + damage prevention (e.g., Dig, Scrunch)
        if (lower.contains("prevent") && (lower.contains("damage") || lower.contains("effects of")) && lower.contains("next turn")) {
            boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                if (ae.getType() == AttackEffectType.COIN_FLIP_AFTER_DAMAGE && "DAMAGE_PREVENTION".equals(ae.getParams().get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                Map<String, Object> params = new HashMap<>();
                params.put("effectType", "DAMAGE_PREVENTION");
                params.put("applyOnHeads", String.valueOf(onHeads));
                if (allHeads) params.put("allHeads", "true");
                params.put("text", text);
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, params));
            }
        }

        // Coin flip + dual conditions (Distortion Beam: heads=Asleep, tails=Confused)
        if (lower.contains("asleep") && lower.contains("confused") && lower.contains("heads") && lower.contains("tails")) {
            boolean headsFirst = lower.indexOf("heads") < lower.indexOf("tails");
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                    "effectType", "APPLY_SPECIAL_CONDITION",
                    "effectParam", headsFirst ? "ASLEEP" : "CONFUSED",
                    "applyOnHeads", "true",
                    "text", text
            )));
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                    "effectType", "APPLY_SPECIAL_CONDITION",
                    "effectParam", headsFirst ? "CONFUSED" : "ASLEEP",
                    "applyOnHeads", "false",
                    "text", text
            )));
            return results;
        }

        // Coin flip + condition + discard (Clamp Crush: heads=Paralyzed + discard energy)
        // Both effects use the SAME coin flip result, so they are combined into one effect.
        if ((lower.contains("paralyzed") || lower.contains("poisoned") || lower.contains("confused"))
                && lower.contains("discard") && lower.contains("energy")) {
            boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
            String cond = lower.contains("poisoned") ? "POISONED" : lower.contains("paralyzed") ? "PARALYZED" : "CONFUSED";
            results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                    "effectType", "COMBINED_CONDITION_DISCARD",
                    "effectParam", cond,
                    "discardCount", "1",
                    "applyOnHeads", String.valueOf(onHeads),
                    "text", text
            )));
            return results;
        }

        // Coin flip + status conditions
        String condition = null;
        if (lower.contains("poisoned")) condition = "POISONED";
        else if (lower.contains("paralyzed")) condition = "PARALYZED";
        else if (lower.contains("confused")) condition = "CONFUSED";
        else if (lower.contains("asleep")) condition = "ASLEEP";
        else if (lower.contains("burned")) condition = "BURNED";

        if (condition != null) {
            boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
            String applyOnHeadsStr = String.valueOf(onHeads);
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                if (ae.getType() == AttackEffectType.COIN_FLIP_AFTER_DAMAGE && "APPLY_SPECIAL_CONDITION".equals(ae.getParams().get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "APPLY_SPECIAL_CONDITION",
                        "effectParam", condition,
                        "applyOnHeads", applyOnHeadsStr,
                        "text", text
                )));
            }
        }

        // Coin flip + can't attack next turn (self) - e.g., Yveltal "Darkness Blade"
        if ((lower.contains("can't attack") || lower.contains("cannot attack")) && lower.contains("your next turn")) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "CANNOT_ATTACK_NEXT_TURN".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "CANNOT_ATTACK_NEXT_TURN",
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + supporter lock - e.g., Krookodile "Bother"
        if ((lower.contains("can't") || lower.contains("cannot")) && lower.contains("supporter") && (lower.contains("heads") || lower.contains("tails"))) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "SUPPORTER_LOCK".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "SUPPORTER_LOCK",
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + opponent discards from hand - e.g., Malamar "Mental Trash"
        if (lower.contains("discard") && lower.contains("hand") && lower.contains("opponent") && (lower.contains("heads") || lower.contains("tails"))) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "OPPONENT_DISCARD_HAND".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                int count = 1;
                Pattern countP = Pattern.compile("(?i)flips\\s+(\\d+)\\s+coin");
                Matcher countM = countP.matcher(lower);
                if (countM.find()) {
                    count = Integer.parseInt(countM.group(1));
                }
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "OPPONENT_DISCARD_HAND",
                        "effectParam", String.valueOf(count),
                        "applyOnHeads", "false",
                        "text", text
                )));
            }
        }

        // Coin flip + search deck - e.g., Skiddo "Lead"
        if (lower.contains("search your deck") && (lower.contains("heads") || lower.contains("tails"))) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "SEARCH_DECK".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                String searchType = "SUPPORTER";
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, Map.of(
                        "effectType", "SEARCH_DECK",
                        "effectParam", searchType,
                        "applyOnHeads", String.valueOf(onHeads),
                        "text", text
                )));
            }
        }

        // Coin flip + attach energy - e.g., Lapras "Seafaring"
        if (lower.contains("attach") && lower.contains("energy") && (lower.contains("heads") || lower.contains("tails"))) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "ATTACH_ENERGY".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                Map<String, Object> params = new HashMap<>();
                params.put("effectType", "ATTACH_ENERGY");
                params.put("applyOnHeads", String.valueOf(onHeads));
                String energyType = "WATER";
                if (lower.contains("lightning")) energyType = "LIGHTNING";
                else if (lower.contains("fire")) energyType = "FIRE";
                else if (lower.contains("grass")) energyType = "GRASS";
                else if (lower.contains("psychic")) energyType = "PSYCHIC";
                else if (lower.contains("fighting")) energyType = "FIGHTING";
                else if (lower.contains("darkness")) energyType = "DARKNESS";
                else if (lower.contains("metal")) energyType = "METAL";
                else if (lower.contains("fairy")) energyType = "FAIRY";
                params.put("effectParam", energyType);

                int coinCount = 1;
                java.util.regex.Pattern countP = java.util.regex.Pattern.compile("(?i)flip\\s+(\\d+)\\s+coins?");
                java.util.regex.Matcher countM = countP.matcher(lower);
                if (countM.find()) {
                    coinCount = Integer.parseInt(countM.group(1));
                }
                params.put("coinCount", String.valueOf(coinCount));
                params.put("forEachHeads", String.valueOf(lower.contains("for each heads")));

                params.put("text", text);
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, params));
            }
        }

        // Coin flip + discard opponent deck - e.g., Rhydon "Mad Mountain"
        if (lower.contains("discard the top") && (lower.contains("heads") || lower.contains("tails"))) {
            boolean alreadyHas = false;
            for (AttackEffect ae : results) {
                Map<String, Object> p = ae.getParams();
                if (p != null && "DISCARD_OPPONENT_DECK".equals(p.get("effectType"))) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                boolean onHeads = !lower.contains("tails") || (lower.contains("heads") && lower.indexOf("heads") < lower.indexOf("tails"));
                boolean bothHeads = lower.contains("both of them are heads") || lower.contains("both are heads");
                Map<String, Object> madParams = new HashMap<>();
                madParams.put("effectType", "DISCARD_OPPONENT_DECK");
                madParams.put("effectParam", "1");
                madParams.put("applyOnHeads", bothHeads ? "all" : String.valueOf(onHeads));
                if (bothHeads) madParams.put("allHeads", "true");
                if (lower.contains("for each damage counter")) madParams.put("multiplyByDamageCounters", "true");
                madParams.put("text", text);
                results.add(new AttackEffect(AttackEffectType.COIN_FLIP_AFTER_DAMAGE, madParams));
            }
        }

        return results;
    }

    static List<AttackEffect> parseSwitch(String text) {
        String lower = text.toLowerCase();
        List<AttackEffect> results = new ArrayList<>();

        boolean selfSwitch = lower.contains("switch this pok\u00e9mon with") || lower.contains("switch this pokemon with");
        boolean oppSwitch = lower.contains("opponent switches") || (lower.contains("switch") && lower.contains("opponent's"));

        if (selfSwitch) {
            boolean conditional = lower.contains("if you attached") || lower.contains("if you did");
            results.add(new AttackEffect(AttackEffectType.SWITCH_AFTER_DAMAGE,
                    Map.of("text", text, "switchAttacker", true, "conditional", conditional)));
        }
        if (oppSwitch) {
            results.add(new AttackEffect(AttackEffectType.SWITCH_AFTER_DAMAGE, Map.of("text", text, "switchAttacker", false)));
        }

        return results;
    }

    static AttackEffect parseRecoil(String text) {
        // "This Pokémon does N damage to itself"
        Pattern p = Pattern.compile("(?i)does\\s+(\\d+)\\s+damage\\s+to\\s+itself");
        Matcher m = p.matcher(text);
        if (m.find()) {
            int counters = Integer.parseInt(m.group(1)) / 10;
            return new AttackEffect(AttackEffectType.RECOIL, Map.of("count", counters, "text", text));
        }
        return null;
    }

    static AttackEffect parseAbilitySuppression(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("no abilities") && !lower.contains("has no") && !lower.contains("ability")) return null;
        if (!lower.contains("defending") && !lower.contains("opponent")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        return new AttackEffect(AttackEffectType.ABILITY_SUPPRESSION, params);
    }

    static AttackEffect parseToolDiscard(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("discard") || !lower.contains("tool") && !lower.contains("pok\u00e9mon tool") && !lower.contains("pokemon tool")) return null;
        if (!lower.contains("opponent") && !lower.contains("defending")) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        return new AttackEffect(AttackEffectType.DISCARD_TOOL, params);
    }

    static AttackEffect parseReorderDeck(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("look at the top") || !lower.contains("put them back")) return null;
        int count = 3;
        Pattern p = Pattern.compile("(?i)top\\s+(\\d+)\\s+cards?");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            count = Integer.parseInt(m.group(1));
        }
        return new AttackEffect(AttackEffectType.REORDER_DECK, Map.of("count", count, "text", text));
    }

    static AttackEffect parsePeekOpponentDeck(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("top card") || !lower.contains("opponent") || !lower.contains("deck")) return null;
        if (!lower.contains("look at") && !lower.contains("look")) return null;
        return new AttackEffect(AttackEffectType.PEEK_OPPONENT_DECK, Map.of("text", text));
    }

    static AttackEffect parseOpponentRandomDiscard(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("random card") || !lower.contains("opponent") || !lower.contains("hand")) return null;
        if (!lower.contains("shuffles") && !lower.contains("shuffle")) return null;
        return new AttackEffect(AttackEffectType.OPPONENT_RANDOM_DISCARD, Map.of("text", text));
    }

    static AttackEffect parseSetHp(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("remaining hp") && !lower.contains("remaining hp")) return null;
        if (!lower.contains("10") && !lower.contains("10")) return null;
        return new AttackEffect(AttackEffectType.SET_HP, Map.of("targetHp", 10, "text", text));
    }

    static AttackEffect parseMentalPanic(String text) {
        String lower = text.toLowerCase();
        if (!lower.contains("if the defending") && !lower.contains("if that pok")) return null;
        if (!lower.contains("tries to attack") && !lower.contains("tries to use")) return null;
        if (!lower.contains("flip") || !lower.contains("coin")) return null;
        if (!lower.contains("tails") || !lower.contains("does nothing")) return null;
        return new AttackEffect(AttackEffectType.MENTAL_PANIC, Map.of("text", text));
    }
}
