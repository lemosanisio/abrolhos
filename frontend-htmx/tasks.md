# HTMX Frontend - Tasks

- [ ] **Initialization**
    - [ ] Initialize `package.json` for build tools.
    - [ ] Install `tailwindcss`, `htmx.org`.
    - [ ] Configure `tailwind.config.js`.

- [ ] **Asset Pipeline**
    - [ ] Create `src/css/input.css`.
    - [ ] Create `src/js/app.js` (Auth logic).
    - [ ] Set up build scripts to output assets to `backend/src/main/resources/static`.

- [ ] **Backend Integration (Thymeleaf)**
    - [ ] Update `backend` templates to include the generated CSS/JS.
    - [ ] Add `hx-boost="true"` to the main layout.
    - [ ] Implement HATEOAS logic in controllers (conditionally render buttons).

- [ ] **Features**
    - [ ] **Auth:** Implement Login Page (HTML + JS for token storage).
    - [ ] **Theme:** Implement Light/Dark toggle (JS + Tailwind `dark:` classes).
    - [ ] **Navigation:** Ensure full URL updates (`hx-push-url`).

- [ ] **Verification**
    - [ ] Verify JWT injection works for all requests.
    - [ ] Verify Forward/Back button navigation.
