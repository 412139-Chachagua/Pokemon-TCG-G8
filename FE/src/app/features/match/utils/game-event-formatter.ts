

// ─── Modelo ────────────────────────────────────────────────────────────────

import { GameEventDto } from "../../../shared/models/game-action.models";

/**
 * Entrada de log lista para renderizar en el game-log.
 * Separa el "evento que llega del backend" (GameEventDto) del
 * "evento que se muestra en pantalla" (LogEntry).
 */
export interface LogEntry {
  /** Mensaje user-friendly en español */
  message: string;
  /**
   * Clase CSS para el color de fondo según el tipo de evento.
   */
  cssClass: 'hostile' | 'reward' | 'heal' | 'energy' | 'attack' | 'evo' | 'status' | 'phase' | 'mulligan' | 'setup' | 'info';
  /** Número de turno en que ocurrió, para agrupar eventos */
  turnNumber?: number;
}

// ─── CSS class ──────────────────────────────────────────────────────────────

function resolveCssClass(type: string, payload?: Record<string, unknown>): LogEntry['cssClass'] {
  if (type.includes('DAMAGE') || type.includes('KO') || type === 'CONFUSION_SELF_HIT' || type === 'BENCH_DAMAGE') return 'hostile';
  if (type.includes('PRIZE') || type.includes('VICTORY')) return 'reward';
  if (type.includes('HEAL')) return 'heal';
  if (type.includes('ENERGY')) return 'energy';
  if (type.includes('ATTACK') || type === 'RECOIL_OCCURRED') return 'attack';
  if (type.includes('EVOLV') || type === 'TOOL_ATTACHED') return 'evo';
  if (type.includes('STATUS') || type === 'CONFUSION_SELF_HIT') return 'status';
  if (type.includes('PHASE')) return 'phase';
  if (type.includes('MULLIGAN')) return 'mulligan';
  if (type.includes('SETUP')) return 'setup';
  if (type === 'ABILITY_USED' && payload) {
    if (payload['preventEffects']) return 'heal';
    if (payload['bonus']) return 'info';
    if (payload['targetPlayerId']) return 'hostile';
  }
  if (type === 'OPPONENT_DISCARD_HAND') return 'hostile';
  if (type === 'SUPPORTER_LOCK') return 'hostile';
  if (type === 'CANNOT_ATTACK_NEXT_TURN') return 'hostile';
  if (type === 'STATUS_REMOVED') return 'heal';
  return 'info';
}

// ─── Traducción de condiciones especiales ──────────────────────────────────

const CONDITION_TRANSLATIONS: Record<string, string> = {
  ASLEEP: 'Dormido',
  BURNED: 'Quemado',
  CONFUSED: 'Confundido',
  PARALYZED: 'Paralizado',
  POISONED: 'Envenenado',
};

function translateCondition(condition: string): string {
  return CONDITION_TRANSLATIONS[condition] ?? condition;
}

// ─── Helpers de jugador ────────────────────────────────────────────────────

function isMe(playerId: string | undefined | null, myPlayerId: string | null): boolean {
  if (!playerId || !myPlayerId) return false;
  return playerId === myPlayerId;
}

/**
 * Elige la conjugación según quién hizo la acción.
 * `selfMsg` recibe un verbo conjugado en VOS (ej: "Colocaste", "Robaste").
 * `otherMsg` recibe el mismo verbo en tercera persona (ej: "colocó", "robó").
 *
 * Devuelve frases completas como:
 *   "Colocaste tu Pokémon activo"
 *   "El oponente colocó su Pokémon activo"
 */
function buildAction(
  playerId: string | undefined | null,
  myPlayerId: string | null,
  selfMsg: string,
  otherMsg: string,
): string {
  if (!playerId || !myPlayerId) return otherMsg;
  return isMe(playerId, myPlayerId) ? selfMsg : otherMsg;
}

// ─── Formateador principal ─────────────────────────────────────────────────

/**
 * Convierte un GameEventDto (evento crudo del backend) en un LogEntry
 * (entrada lista para renderizar), construyendo el mensaje desde el
 * payload estructurado y resolviendo "vos"/"él" según myPlayerId.
 *
 * @param event       Evento crudo del backend
 * @param myPlayerId  ID del jugador local, o null si no hay sesión
 * @returns           LogEntry con mensaje user-friendly y clase CSS
 */
export function formatGameEvent(event: GameEventDto, myPlayerId: string | null): LogEntry {
  const { type, payload = {}, turnNumber } = event;
  const cssClass = resolveCssClass(type, payload);
  const message = buildMessage(event, myPlayerId);

  return { message, cssClass, turnNumber };
}

// ─── Construcción de mensajes ──────────────────────────────────────────────

function buildMessage(event: GameEventDto, myPlayerId: string | null): string {
  const { type, payload = {} } = event;

  switch (type) {
    // ── Setup ──────────────────────────────────────────────────────────────
    case 'SETUP_ACTIVE_PLACED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Colocaste tu Pokémon activo',
        'El oponente colocó su Pokémon activo',
      );
    case 'SETUP_ACTIVE_REMOVED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Quitaste tu Pokémon activo',
        'El oponente quitó su Pokémon activo',
      );
    case 'SETUP_BENCH_PLACED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Colocaste un Pokémon en la banca',
        'El oponente colocó un Pokémon en la banca',
      );
    case 'SETUP_BENCH_REMOVED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Quitaste un Pokémon de la banca',
        'El oponente quitó un Pokémon de la banca',
      );
    case 'SETUP_CONFIRMED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Confirmaste tu preparación',
        'El oponente confirmó su preparación',
      );
    case 'SETUP_COMPLETED':
      return 'Ambos jugadores confirmaron. ¡La partida está activa!';

    // ── Coin flip ──────────────────────────────────────────────────────────
    case 'COIN_FLIP_RESULT': {
      const winner = payload['winner'] as string | undefined;
      const result = payload['result'] as string | undefined;
      const lado = result === 'HEADS' ? 'Cara' : 'Cruz';
      if (isMe(winner, myPlayerId)) return `${lado} — comenzás`;
      return `${lado} — el oponente comienza`;
    }

    // ── Turnos ─────────────────────────────────────────────────────────────
    case 'CARD_DRAWN':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Robaste una carta',
        'El oponente robó una carta',
      );
    case 'PHASE_CHANGED': {
      const next = payload['nextPlayerId'] as string | undefined;
      const turn = payload['turnNumber'] as number | undefined;
      if (isMe(next, myPlayerId)) {
        return turn != null
          ? `Turno ${turn} — te toca`
          : 'Cambio de fase — te toca';
      }
      return turn != null
        ? `Turno ${turn} — le toca al oponente`
        : 'Cambio de fase — le toca al oponente';
    }

    // ── Energía ────────────────────────────────────────────────────────────
    case 'ENERGY_ATTACHED': {
      const newTotal = payload['newTotalCount'] as number | undefined;
      let msg = buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Uniste Energía a un Pokémon',
        'El oponente unió Energía a un Pokémon',
      );
      if (newTotal != null) msg += ` (ahora ${newTotal} energía(s))`;
      return msg;
    }
    case 'ENERGY_DISCARDED': {
      const msg = event.message ?? '';
      const count = payload['count'] as number | undefined;
      if (msg.includes('from attacker')) {
        return count != null ? `Descartaste ${count} energía(s) propia(s)` : 'Descartaste energía propia';
      }
      if (msg.includes('from defender')) {
        return count != null ? `Descartaste ${count} energía(s) del defensor` : 'Descartaste energía del defensor';
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Descartaste Energía',
        'El oponente descartó Energía',
      );
    }

    // ── Evolución y herramientas ───────────────────────────────────────────
    case 'POKEMON_EVOLVED': {
      const pkmn = payload['pokemonCardDefinitionId'] as string | undefined;
      const from = payload['evolvedFrom'] as string | undefined;
      let msg = buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        pkmn ? `Evolucionaste a ${pkmn}` : 'Evolucionaste un Pokémon',
        pkmn ? `El oponente evolucionó a ${pkmn}` : 'El oponente evolucionó un Pokémon',
      );
      if (from) msg += ` (desde ${from})`;
      return msg;
    }
    case 'TOOL_ATTACHED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Uniste una Herramienta Pokémon',
        'El oponente unió una Herramienta Pokémon',
      );
    case 'POKEMON_PLACED_ON_BENCH':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Colocaste un Pokémon en la banca',
        'El oponente colocó un Pokémon en la banca',
      );

    // ── Ataque ─────────────────────────────────────────────────────────────
    case 'ATTACK_DECLARED': {
      const attackName = payload['attackName'] as string | undefined;
      if (attackName) {
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          `Usaste ${attackName}`,
          `El oponente usó ${attackName}`,
        );
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Declaraste un ataque',
        'El oponente declaró un ataque',
      );
    }
    case 'DAMAGE_APPLIED': {
      const finalDmg = payload['finalDamage'] as number | undefined;
      const countersDmg = payload['damageCountersAdded'] as number | undefined;
      const baseDmg = payload['baseDamage'] as number | undefined;
      const dmg = finalDmg ?? (countersDmg != null ? countersDmg * 10 : undefined);
      if (dmg != null) {
        const parts: string[] = [`${dmg} de daño`];
        if (baseDmg != null && baseDmg !== dmg) {
          const breakdown: string[] = [`${baseDmg}`];
          if (payload['weaknessApplied']) {
            const w = (payload['weaknessMultiplier'] as number) ?? 2;
            breakdown.push(`×${w} debilidad`);
          }
          if (payload['resistanceApplied']) {
            const r = (payload['resistanceValue'] as number) ?? -20;
            breakdown.push(`${r} resistencia`);
          }
          return `${breakdown.join(' → ')} = ${dmg} total`;
        }
        if (payload['weaknessApplied']) {
          const weaknessVal = (payload['weaknessMultiplier'] as number) ?? 2;
          parts.push(`×${weaknessVal} debilidad`);
        }
        if (payload['resistanceApplied']) {
          const resistVal = (payload['resistanceValue'] as number) ?? -20;
          parts.push(`${resistVal} resistencia`);
        }
        return parts.join(' ');
      }
      const selfDmg = payload['selfDamageCounters'] as number | undefined;
      if (selfDmg != null) return `${selfDmg * 10} de daño por confusión`;
      return 'Daño aplicado';
    }
    case 'CONFUSION_SELF_HIT': {
      const dmg = payload['selfDamageCounters'] as number | undefined;
      if (dmg != null) return `Se golpeó a sí mismo por confusión: ${dmg * 10} de daño`;
      return 'Un Pokémon confundido se golpeó a sí mismo';
    }
    case 'ATTACK_EFFECT_RESOLVED': {
      const effect = payload['effectType'] as string | undefined;
      const effectParam = payload['effectParam'] as string | undefined;
      if (effect === 'APPLY_SPECIAL_CONDITION' && effectParam) {
        return `${translateCondition(effectParam)} aplicado`;
      }
      if (effect === 'DISCARD_ENERGY') {
        return effectParam ? `Se descartaron ${effectParam} energía(s)` : 'Se descartó energía';
      }
      if (effect === 'DAMAGE_BENCH') {
        return effectParam ? `Daño a banca: ${effectParam}` : 'Daño a banca';
      }
      if (effect === 'RECOIL') {
        return effectParam ? `${effectParam} contadores de retroceso` : 'Retroceso';
      }
      if (effect === 'HEAL_USER') {
        return effectParam ? `Se recuperaron ${effectParam} contadores` : 'Recuperación';
      }
      if (effect === 'SWITCH_AFTER_DAMAGE') {
        return 'Intercambio de Pokémon activo';
      }
      if (effect) return `Efecto: ${effect}`;
      return 'Efecto de ataque resuelto';
    }

    // ── Knockout y premios ─────────────────────────────────────────────────
    case 'KNOCKOUT_OCCURRED': {
      const owner = payload['ownerPlayerId'] as string | undefined;
      if (isMe(owner, myPlayerId)) return '¡Debilitaron a tu Pokémon!';
      return '¡Debilitaste a un Pokémon del oponente!';
    }
    case 'KO_REPLACEMENT_REQUIRED':
      return buildAction(
        payload['knockedOutPlayerId'] as string | undefined, myPlayerId,
        'Debés reemplazar tu Pokémon debilitado',
        'El oponente debe reemplazar su Pokémon debilitado',
      );
    case 'KO_REPLACEMENT_DONE':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Reemplazaste tu Pokémon activo',
        'El oponente reemplazó su Pokémon activo',
      );
    case 'PRIZE_TAKEN': {
      const count = (payload['prizeCount'] as number) ?? 1;
      const label = count === 1 ? 'carta de premio' : 'cartas de premio';
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        `Tomaste ${count} ${label}`,
        `El oponente tomó ${count} ${label}`,
      );
    }

    // ── Retiro ─────────────────────────────────────────────────────────────
    case 'RETREAT_EXECUTED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Retiraste tu Pokémon activo',
        'El oponente retiró su Pokémon activo',
      );

    // ── Entrenadores ───────────────────────────────────────────────────────
    case 'TRAINER_PLAYED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Jugaste una carta de Entrenador',
        'El oponente jugó una carta de Entrenador',
      );
    case 'TRAINER_EFFECT_RESOLVED':
      return 'Efecto de Entrenador resuelto';

    // ── Curaciones ─────────────────────────────────────────────────────────
    case 'POKEMON_HEALED': {
      const counters = (payload['countersRemoved'] as number | undefined)
        ?? (payload['healedCounters'] as number | undefined);
      if (counters != null) {
        const hp = counters * 10;
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          `Recuperaste ${hp} PS`,
          `El oponente recuperó ${hp} PS`,
        );
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Recuperaste PS',
        'El oponente recuperó PS',
      );
    }

    // ── Habilidades ────────────────────────────────────────────────────────
    case 'ABILITY_USED': {
      const abilityName = payload['abilityName'] as string | undefined;
      const bonus = payload['bonus'] as number | undefined;
      const preventEffects = payload['preventEffects'] as boolean | undefined;
      const targetPlayerId = payload['targetPlayerId'] as string | undefined;

      if (bonus != null) {
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          `Tu Pokémon hará +${bonus} de daño el próximo turno`,
          `El Pokémon del oponente hará +${bonus} de daño el próximo turno`,
        );
      }
      if (preventEffects) {
        return 'El Pokémon se protegió: no recibirá daño el próximo turno';
      }
      if (targetPlayerId && !abilityName) {
        if (isMe(targetPlayerId, myPlayerId)) return '¡No podés jugar Partidarios el próximo turno!';
        return '¡El oponente no puede jugar Partidarios el próximo turno!';
      }
      if (!abilityName && payload['pokemonInstanceId']) {
        return 'El Pokémon no puede atacar el próximo turno';
      }
      if (abilityName) {
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          `Usaste ${abilityName}`,
          `El oponente usó ${abilityName}`,
        );
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Usaste una habilidad',
        'El oponente usó una habilidad',
      );
    }
    case 'ABILITY_BLOCKED': {
      const abilityName = payload['abilityName'] as string | undefined;
      if (abilityName) return `Habilidad bloqueada: ${abilityName}`;
      return 'Habilidad bloqueada';
    }

    // ── Daño a banca ───────────────────────────────────────────────────────
    case 'BENCH_DAMAGE': {
      const targets = payload['targets'] as Array<{instanceId?: string; damageCounters?: number}> | undefined;
      if (targets && targets.length > 0) {
        const totalDmg = targets.reduce((sum, t) => sum + (t.damageCounters ?? 0), 0);
        return `Daño a banca: ${totalDmg} contadores en ${targets.length} Pokémon`;
      }
      return 'Daño aplicado a Pokémon en banca';
    }

    // ── Búsqueda en mazo ───────────────────────────────────────────────────
    case 'POKEMON_SEARCHED': {
      const searchType = payload['searchType'] as string | undefined;
      const foundCount = payload['foundCount'] as number | undefined;
      const typeLabel = searchType === 'SUPPORTER' ? 'Partidario' : searchType === 'ENERGY' ? 'Energía' : 'carta';
      if (foundCount != null && foundCount > 0) {
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          `Buscaste ${foundCount} ${typeLabel}(s) en tu mazo`,
          `El oponente buscó ${foundCount} ${typeLabel}(s) en su mazo`,
        );
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Buscaste en tu mazo',
        'El oponente buscó en su mazo',
      );
    }

    // ── Estadio ────────────────────────────────────────────────────────────
    case 'STADIUM_PLAYED':
      return 'Se jugó una carta de Estadio';
    case 'STADIUM_REMOVED':
      return 'El Estadio fue reemplazado';

    // ── Varios ─────────────────────────────────────────────────────────────
    case 'SUDDEN_DEATH_STARTED':
      return '¡Muerte súbita! Cada jugador tiene 1 carta de premio.';
    case 'VICTORY_DECIDED':
      return '¡La partida terminó!';

    // ── Ataque cancelado ───────────────────────────────────────────────────
    case 'ATTACK_CANCELED': {
      const reason = payload['reason'] as string | undefined;
      if (reason === 'asleep') return 'El Pokémon está Dormido y no puede atacar';
      if (reason === 'paralyzed') return 'El Pokémon está Paralizado y no puede atacar';
      return 'El ataque falló';
    }

    // ── Recoil ─────────────────────────────────────────────────────────────
    case 'RECOIL_OCCURRED': {
      const dmg = payload['damage'] as number | undefined;
      if (dmg != null) return `${dmg} de daño por retroceso`;
      return 'Daño por retroceso';
    }

    // ── Switch ejecutado ───────────────────────────────────────────────────
    case 'SWITCH_EXECUTED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Cambiaste tu Pokémon activo',
        'El oponente cambió su Pokémon activo',
      );

    // ── Cartas robadas por efectos ─────────────────────────────────────────
    case 'CARDS_DRAWN':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Robaste cartas por efecto',
        'El oponente robó cartas por efecto',
      );

    // ── Condiciones especiales ─────────────────────────────────────────────
    case 'STATUS_APPLIED': {
      const condition = payload['condition'] as string | undefined;
      const blocked = payload['blocked'] as boolean | undefined;
      if (condition) {
        const translated = translateCondition(condition);
        if (blocked) return `Condición bloqueada: ${translated}`;
        return `${translated} aplicado`;
      }
      return 'Condición especial aplicada';
    }

    // ── Condición removida ─────────────────────────────────────────────────
    case 'STATUS_REMOVED': {
      const condition2 = payload['condition'] as string | undefined;
      if (condition2) {
        return `${translateCondition(condition2)} removido`;
      }
      return 'Condición especial eliminada';
    }

    // ── Mulligan ───────────────────────────────────────────────────────────
    case 'INITIAL_MULLIGAN_NEEDED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Necesitás decidir tu mulligan inicial',
        'El oponente necesita decidir su mulligan inicial',
      );
    case 'MULLIGAN_REVEALED':
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Revelaste tu mano por mulligan',
        'El oponente reveló su mano por mulligan',
      );
    case 'INITIAL_MULLIGAN_RESOLVED': {
      const action = payload['action'] as string | undefined;
      const hasBasic = payload['hasBasic'] as boolean | undefined;
      if (action === 'MULLIGAN') {
        if (hasBasic) {
          return buildAction(
            payload['playerId'] as string | undefined, myPlayerId,
            'Hiciste mulligan y obtuviste un Pokémon Basic',
            'El oponente hizo mulligan y obtuvo un Pokémon Basic',
          );
        }
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          'Hiciste mulligan pero aún no tenés un Pokémon Basic',
          'El oponente hizo mulligan pero aún no tiene un Pokémon Basic',
        );
      }
      // KEEP
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'Decidiste quedarte tu mano sin Pokémon Basic',
        'El oponente decidió quedarse su mano sin Pokémon Basic',
      );
    }
    case 'MULLIGAN_DRAW_OPPORTUNITY': {
      const count = (payload['count'] as number) ?? 1;
      const label = count === 1 ? 'carta' : 'cartas';
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        `Podés robar ${count} ${label} por mulligan del oponente`,
        `El oponente puede robar ${count} ${label} por mulligan`,
      );
    }
    case 'MULLIGAN_DRAW_RESOLVED': {
      const drewCards = payload['drewCards'] as boolean | undefined;
      if (drewCards) {
        return buildAction(
          payload['playerId'] as string | undefined, myPlayerId,
          'Robaste cartas extra por mulligan',
          'El oponente robó cartas extra por mulligan',
        );
      }
      return buildAction(
        payload['playerId'] as string | undefined, myPlayerId,
        'No robaste cartas extra por mulligan',
        'El oponente no robó cartas extra por mulligan',
      );
    }

    default:
      // Fallback: mostramos el mensaje original del backend
      return event.message;
  }
}
