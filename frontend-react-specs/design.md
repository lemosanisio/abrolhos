# React Frontend - Design & Architecture

## 1. Project Structure (Atomic Design)

```text
src/
├── components/
│   ├── atoms/       # Buttons, Inputs, Labels, Icons (shadcn/ui base)
│   ├── molecules/   # Form fields, Search bars, User avatars with text
│   ├── organisms/   # Navbar, Footer, PostCardList, LoginForm
│   └── templates/   # MainLayout, AuthLayout, DashboardLayout
├── pages/           # Route components (HomePage, PostPage, AdminPage)
├── lib/             # Utilities (axios instance, utils)
├── hooks/           # Custom hooks
├── context/         # AuthContext, ThemeContext
└── assets/          # Static assets
```

## 2. Technology Stack Details

- **Bun:** Used for dependency management and running scripts (`bun install`, `bun run dev`).
- **Vite:** Fast build tool.
- **Tailwind CSS:** Utility-first styling.
- **shadcn/ui:** Reusable components built with Radix UI and Tailwind.
    - *Note:* Shadcn components will be placed in `components/atoms` or `components/molecules` depending on complexity.

## 3. State Management Strategy

### Server State (TanStack Query)
- **Queries:** Fetching posts, categories, tags.
- **Mutations:** Login, Create/Update/Delete operations.
- **Caching:** Automatic background refetching, optimistic updates.

### Client State (React Context / Zustand)
- **Auth:** Storing the JWT and User info.
- **Theme:** Toggling Light/Dark mode.

## 4. Authentication Flow

1.  **Login:** `POST /api/v1/auth/login`.
2.  **Storage:** Save JWT in `localStorage`.
3.  **Interceptor:** Axios interceptor attaches `Authorization: Bearer <token>` to every request.
4.  **Guard:** Protected routes check for token presence; redirect to `/login` if missing.

## 5. Routing (React Router v6)

- Use `createBrowserRouter` with `RouterProvider`.
- Implement `loaders` for pre-fetching data (e.g., post details) before rendering the route.
