## 1. Backend Structure

- [x] 1.1 Create backend directory structure per `02-project-structure-contract.md` and `05-deck-contract.md`
- [x] 1.2 Create base Maven project with Spring Boot 3.x, Java 21

## 2. Canonical Name Normalization

- [x] 2.1 Implement `CardNameNormalizer` utility that strips level (Nv.), "Equipo Plasma", Delta species (δ) per canonical name rules in `05-deck-contract.md`
- [x] 2.2 Write unit tests for name normalization (level, Team Plasma, Delta, -EX, Mega-, symbols, owner names)

## 3. Validation Rule

- [x] 3.1 Add copy-limit validation in `DeckValidator` using canonical name counting and existing `MORE_THAN_4_COPIES` error code
- [x] 3.2 Wire validation into add-card-to-deck endpoint (reject 5th+ copy by canonical name)
- [x] 3.3 Write unit tests for copy limit (within limit, at limit, exceeded, non-Pokémon cards excluded)

## 4. Verification

- [x] 4.1 Run backend build (`mvn compile` or equivalent)
- [x] 4.2 Run backend tests (`mvn test` or equivalent) and confirm all pass
