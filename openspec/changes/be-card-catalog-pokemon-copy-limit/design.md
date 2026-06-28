## Context

The deck builder currently has no server-side validation for Pokémon card copy limits per canonical name. The official TCG rule limits Pokémon cards to 4 copies per deck (by name). The contract `docs/contracts_ai/05-deck-contract.md` already defines this rule and the canonical name resolution logic (ignore level, Team Plasma, Delta species, etc.). The existing enum contract `docs/contracts_ai/03-enums-contract.md` already defines the error code `MORE_THAN_4_COPIES` in `DeckValidationError`.

This validation must be enforced backend-side as the backend is the single source of truth for game rules.

## Goals / Non-Goals

**Goals:**
- Backend rejects adding a 5th copy of the same Pokémon card (by canonical name) to a deck
- Returns existing `MORE_THAN_4_COPIES` error code from the contract
- Rule is enforced at the deck-building service layer (not engine)
- Backend directory structure created under `/backend` per contract

**Non-Goals:**
- No frontend UI changes
- No changes to Energy or Trainer card limits
- No changes to the Game Engine
- No deck validation on game start (only on add-card action)

## Decisions

1. **Validation at service layer** – Add validation in deck-building service (e.g., `DeckService` or `DeckValidator`) rather than the Game Engine. The existing `DeckValidator` at `services/decks/DeckValidator.java` is the correct place since it already handles deck validation rules.

2. **Error code** – Use existing `MORE_THAN_4_COPIES` from `DeckValidationError` enum (defined in `03-enums-contract.md`). No new error code needed.

3. **Count by canonical name** – Per `05-deck-contract.md:36-47`, count copies by canonical name, not by card ID. The canonical name strips level (Nv.), "Equipo Plasma", Delta species (δ), and keeps owner names, symbols, -EX suffix, Mega- prefix. Name normalization must be consistent between add-card validation and full deck validation.

4. **Scope: Pokémon cards only** – Only cards with supertype `POKEMON` are checked. Basic Energy and Trainer cards are excluded.

5. **Backend structure** – Since `/backend` does not exist yet, implementation must create the structure per `02-project-structure-contract.md` and `05-deck-contract.md`.

## Risks / Trade-offs

- [Risk] Name normalization logic could be complex → Mitigation: implement in a single `CardNameNormalizer` utility, reusable across validation contexts
- [Risk] Frontend might still show stale state if error is not handled → Mitigation: frontend already handles validation errors from save flow
