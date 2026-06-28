## ADDED Requirements

### Requirement: User can register with email, password, and display name
The system SHALL provide a registration page at `/auth/register` with a reactive form containing three fields: email (required, valid email format), password (required, minimum 6 characters), and displayName (required). The form SHALL use Angular's `ReactiveFormsModule`.

#### Scenario: Successful registration
- **WHEN** user submits the form with valid email, password (>= 6 chars), and displayName
- **THEN** system calls POST `/api/users/register` with `{ email, password, displayName }`
- **AND** on success, stores the UserResponse and loads the associated Player via GET `/api/players/{id}`
- **AND** shows a success toast notification via NotificationService
- **AND** redirects to `/decks` after 1.5 seconds

#### Scenario: Backend validation error
- **WHEN** user submits the form and the backend returns an error (e.g., email already exists)
- **THEN** system displays the error message from the backend response below the form
- **AND** form remains editable for retry

#### Scenario: Network error
- **WHEN** user submits the form and a network error occurs
- **THEN** system displays a generic error message "Error de conexión. Intentá de nuevo."
- **AND** form remains editable for retry

### Requirement: Registration form validates input client-side
The system SHALL validate form fields before submission and show inline error messages.

#### Scenario: Empty email
- **WHEN** user leaves email field empty and attempts submit
- **THEN** system shows error "El email es requerido"

#### Scenario: Invalid email format
- **WHEN** user enters an invalid email (e.g., "notanemail") and attempts submit
- **THEN** system shows error "Ingresá un email válido"

#### Scenario: Short password
- **WHEN** user enters a password shorter than 6 characters and attempts submit
- **THEN** system shows error "La contraseña debe tener al menos 6 caracteres"

#### Scenario: Empty display name
- **WHEN** user leaves displayName field empty and attempts submit
- **THEN** system shows error "El nombre de usuario es requerido"

### Requirement: Registration page follows Angular best practices
The registration page component SHALL use `ChangeDetectionStrategy.OnPush`, standalone component pattern, `inject()` function for DI, and native control flow (`@if`, `@for`) in templates.

#### Scenario: Component structure
- **WHEN** register-page component is inspected
- **THEN** it uses OnPush change detection
- **AND** imports only ReactiveFormsModule (no FormsModule)
- **AND** uses inject() for Router, AuthService, and NotificationService
