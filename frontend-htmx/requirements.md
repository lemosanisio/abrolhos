# HTMX Frontend - Requirements

## 1. Functional Requirements (FR)

### FR1: Authentication
- **Stateless:** Must use the same JWT (Bearer Token) mechanism as the React app.
- **Integration:** HTMX requests must automatically include the token.
- **Login:** A dedicated login page that retrieves the token and stores it (localStorage).

### FR2: HATEOAS & "Truly REST"
- **Hypermedia Controls:** The backend must return HTML with valid state transitions (links/buttons).
- **State:** The client (browser) should not maintain complex state; it should reflect the server's state.
- **Navigation:** Full URL support with History API (`hx-push-url`).

### FR3: Feature Parity
- **Public:** View Posts, Categories, Tags.
- **Admin:** Create, Update, Delete resources.
- **UX:** Must feel responsive and modern, similar to the React app.

## 2. Non-Functional Requirements (NFR)

### NFR1: Tech Stack
- **Library:** HTMX (latest).
- **Styling:** Tailwind CSS (Shared config with React or Backend).
- **Scripting:** Minimal Vanilla JS (Alpine.js optional) for auth injection.

### NFR2: Architecture
- **Decoupled:** The "frontend" project manages static assets and build tools, while the "backend" serves the HTML templates.
- **Auth Bridge:** A small client-side script to bridge `localStorage` (JWT) to `HTMX` headers.

### NFR3: UX/UI
- **Theme:** Clean, minimalist, Light/Dark mode.
- **Responsiveness:** Mobile-first.
