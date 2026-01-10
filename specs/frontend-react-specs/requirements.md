# React Frontend - Requirements

## 1. Functional Requirements (FR)

### FR1: Authentication
- **Login:** User can log in using username/password.
- **Stateless:** Authentication must use JWT (Bearer Token) stored in the client (e.g., localStorage).
- **Logout:** User can log out, clearing the token.

### FR2: Public Content (Read-Only)
- **Post List:** View a paginated list of published posts.
- **Post Detail:** View a single post by slug.
- **Filtering:** Filter posts by Category or Tag.
- **Navigation:** Navigate between pages using standard browser history (Forward/Back).

### FR3: Admin Content (Protected)
- **Dashboard:** View all posts (including Drafts).
- **Create Post:** Rich text editor for creating posts.
- **Edit Post:** Update existing posts.
- **Delete Post:** Remove posts.
- **Taxonomy:** Manage Categories and Tags (CRUD).

## 2. Non-Functional Requirements (NFR)

### NFR1: Tech Stack
- **Runtime/Manager:** Bun.
- **Framework:** React.
- **Build Tool:** Vite.
- **Styling:** Tailwind CSS.
- **UI Library:** shadcn/ui.

### NFR2: Architecture
- **Design Pattern:** Atomic Design (Strict).
    - `atoms`: Basic UI elements (buttons, inputs).
    - `molecules`: Simple combinations (search bar, form field).
    - `organisms`: Complex sections (header, post card list).
    - `templates`: Page layouts.
    - `pages`: Route handlers.
- **State Management:** TanStack Query (React Query) for server state.
- **Routing:** React Router v6 (Data API / Loaders).

### NFR3: UX/UI
- **Theme:** Clean, minimalist.
- **Mode:** Light/Dark mode support.
- **Responsiveness:** Mobile-first, hamburger menu on mobile.
