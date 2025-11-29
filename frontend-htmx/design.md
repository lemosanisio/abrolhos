# HTMX Frontend - Design & Architecture

## 1. Architecture: "Hypermedia as the Engine of Application State"

Unlike the React app, this frontend relies on the Backend to drive the application state.

- **Backend (Spring Boot):** Renders HTML fragments (Thymeleaf) containing data AND controls (links/buttons).
- **Frontend (Browser):** A "dumb" client that executes the controls using HTMX.

## 2. Stateless Authentication Strategy

To maintain statelessness (JWT) without using Cookies (as per "Truly REST" preference for Bearer tokens):

1.  **Login Page:** A standard HTML form. On submit, it calls `POST /api/v1/auth/login`.
2.  **Token Storage:** On success, a small JS snippet saves the returned JWT to `localStorage`.
3.  **Request Interception:**
    - We configure `htmx.on('htmx:configRequest', ...)` to read the token from `localStorage`.
    - It injects `Authorization: Bearer <token>` into *every* HTMX request.
4.  **Navigation:**
    - We use `hx-boost="true"` on the `<body>`.
    - This converts all standard `<a>` tags into AJAX requests.
    - Because they are AJAX, the interceptor *can* add the Bearer header.
    - This allows "standard" navigation to be authenticated and stateless.

## 3. Project Structure

Since the templates live in `backend/src/main/resources/templates`, this `frontend-htmx` folder serves as the **Asset Pipeline**:

```text
frontend-htmx/
├── src/
│   ├── css/
│   │   └── input.css      # Tailwind directives
│   └── js/
│       └── app.js         # HTMX config, Auth logic, Theme toggle
├── package.json           # Build scripts (Tailwind CLI, etc.)
├── tailwind.config.js     # Shared config
└── index.html             # Dev entry point (optional)
```

## 4. Styling (Tailwind CSS)

- We will use the **same** `tailwind.config.js` theme configuration as the React app to ensure visual consistency.
- The build process will output a `styles.css` file that the Backend includes in its Thymeleaf templates.

## 5. HATEOAS Principles

- **No Client Routing:** The browser URL is just a reflection of the resource URL.
- **Dynamic Controls:**
    - *Example:* The "Edit" button for a post is *only* rendered by the server if the user is the author/admin.
    - The client does not check `if (user.isAdmin)`. It just renders what it gets.
