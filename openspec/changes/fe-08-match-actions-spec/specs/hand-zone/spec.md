## ADDED Requirements

### Requirement: HandZoneComponent

`HandZoneComponent` SHALL render the player's hand cards with selection visual feedback.

**Inputs:**
- `hand: PrivateHandCardModel[]`
- `selectionMode: SelectionMode`
- `validTargets: string[]` (hand indices como strings: "0", "1", "2"...)
- `selectedHandIndex: number | null`

**Outputs:** `cardClicked: { card: PrivateHandCardModel; handIndex: number }`

**Display:**
```
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Charmander│ │  Fire E  │ │  Potion  │ │  ...     │
│  POKEMON  │ │  ENERGY  │ │  TRAINER │ │          │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
```

**Behavior:**
- Each card SHALL display `name` and `supertype` badge
- Cards whose handIndex is NOT in `validTargets` during selection mode SHALL have `opacity: 50%` and `pointer-events: none`
- Card whose handIndex matches `selectedHandIndex` SHALL have a golden border
- Click SHALL emit `cardClicked` with the card data and its `handIndex` (0-based position in hand array)
- SHALL be responsive: horizontal scroll on mobile, wrap on desktop
- SHALL use `standalone: true`, `ChangeDetectionStrategy.OnPush`, `input()/output()`, inline template
- `validTargets` SHALL be `string[]` because that is how `SelectionState` defines it
- HandZone SHALL NOT resolve `cardDef` — it only displays `name` and `supertype` from `PrivateHandCardModel`

**Contract references:** `15-frontend-state-contract.md` (frontend state model), `08-game-action-contract.md` (action payloads reference cards by handIndex)

#### Scenario: HandZone shows cards
- WHEN receiving 4 `PrivateHandCardModel` items
- THEN 4 card items SHALL be rendered with name and supertype badge

#### Scenario: HandZone dims invalid targets
- WHEN `selectionMode` is not `'NONE'` and a card's handIndex is NOT in `validTargets`
- THEN that card SHALL have reduced opacity and `pointer-events: none`

#### Scenario: HandZone highlights selected card
- WHEN `selectedHandIndex` equals a card's handIndex
- THEN that card SHALL have a golden border

#### Scenario: HandZone click emits cardClicked
- WHEN user clicks a card in the hand zone
- THEN `cardClicked` SHALL emit with that card's data and its `handIndex`
- AND `handIndex` SHALL be the 0-based position in the hand array
