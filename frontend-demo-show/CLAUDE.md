# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MindEase (心安动识)** — a mental wellness SPA built with Vite + React 19 + TypeScript 6 + React Router 7. Currently Phase 1: framework + static mock pages. No real backend, auth, or ML — all data is mock.

## Commands

```bash
npm run dev      # Start dev server at http://localhost:5173
npm run build    # Type-check (tsc -b) then bundle (vite build)
npm run preview  # Preview production build locally
npm run lint     # ESLint across all files
```

## Architecture

- **Entry**: `src/main.tsx` → `src/App.tsx` (router config)
- **Routing**: React Router 7 with `<BrowserRouter>`. Login page is standalone; all other pages are nested under `AppLayout` via `<Outlet />`. Default redirect `/` → `/home`.
- **Layout**: `src/components/AppLayout.tsx` — sidebar (220px, sticky) + content area. Sidebar uses `NavLink` for active state. At narrow widths the sidebar collapses to emoji-only (64px).
- **Pages**: 9 pages in `src/pages/` — `LoginPage`, `HomePage`, `DetectPage`, `ChatPage`, `MindfulnessPage`, `DiaryPage`, `ProfilePage`, `GardenPage`, `MePage`. Each page has a co-located CSS file.
- **Mock data**: `src/data/` — `navItems.ts`, `mockUser.ts`, `mockStats.ts`. All TypeScript interfaces exported.
- **CSS**: Design system in `src/index.css` via CSS custom properties (no framework). Healing aesthetic: warm off-white `--bg: #faf8f5`, soft green/blue/orange accents, rounded cards (`--radius: 14px`), subtle shadows. Component styles are plain CSS files, 1-to-1 with their TSX.

## Route Map

| Path | Page | Layout |
|------|------|--------|
| `/login` | LoginPage | standalone (no sidebar) |
| `/home` | HomePage | AppLayout |
| `/detect` | DetectPage | AppLayout |
| `/chat` | ChatPage | AppLayout |
| `/mindfulness` | MindfulnessPage | AppLayout |
| `/diary` | DiaryPage | AppLayout |
| `/profile` | ProfilePage | AppLayout |
| `/garden` | GardenPage | AppLayout |
| `/me` | MePage | AppLayout |
| `*` | → redirect `/home` | |

## TypeScript Configuration

- Project references: `tsconfig.app.json` (src) + `tsconfig.node.json` (vite config)
- `noUnusedLocals` / `noUnusedParameters` / `noFallthroughCasesInSwitch` / `erasableSyntaxOnly` all active
- `verbatimModuleSyntax`: imports must use full file extensions (`.ts`, `.tsx`)
- `jsx: "react-jsx"` — no need to import React for JSX

## Linting

ESLint flat config: `@eslint/js` recommended + `typescript-eslint` recommended + `react-hooks` + `react-refresh`. `dist/` ignored.
