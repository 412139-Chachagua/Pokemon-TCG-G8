## ADDED Requirements

### Requirement: CardRepositoryService caches card details
The system SHALL create `core/services/card-repository.service.ts` with a signal-based cache of `CardDetailResponse` by cardId. Methods: `resolve(id)`, `preload(ids[])`, `getFromCache(id)`.

#### Scenario: resolve returns cached card
- **WHEN** `resolve('xy1-1')` is called and the card is already cached
- **THEN** it SHALL return the cached `CardDetailResponse` without an HTTP call

#### Scenario: resolve fetches and caches uncached card
- **WHEN** `resolve('xy1-1')` is called and the card is NOT cached
- **THEN** it SHALL call `CardApiService.getCardById('xy1-1')`, cache the result, and return it

#### Scenario: preload fetches multiple cards
- **WHEN** `preload(['xy1-1', 'xy1-2', 'xy1-3'])` is called
- **THEN** it SHALL resolve all uncached cards in parallel and cache them

#### Scenario: getFromCache returns null for missing
- **WHEN** `getFromCache('nonexistent')` is called
- **THEN** it SHALL return `null`

### Requirement: NotificationService provides global notifications
The system SHALL create `core/services/notification.service.ts` with signal-based notifications. Methods: `show(message, type, duration?)`, `dismiss(id)`.

#### Scenario: show creates notification
- **WHEN** `notificationService.show('Saved!', 'success', 3000)` is called
- **THEN** a notification SHALL be added to the signals array with the message, type, and auto-dismiss timer

#### Scenario: dismiss removes notification
- **WHEN** `notificationService.dismiss('notif-1')` is called
- **THEN** the notification with that ID SHALL be removed from the signals array

#### Scenario: auto-dismiss after duration
- **WHEN** a notification is shown with `duration: 3000`
- **THEN** it SHALL be automatically dismissed after 3000ms
