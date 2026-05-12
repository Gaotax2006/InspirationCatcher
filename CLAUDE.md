# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build fat JAR (shade plugin bundles runtime deps; JavaFX is provided scope)
mvn clean package -DskipTests

# Run (requires JavaFX SDK at javafx-sdk-21.0.1/lib; javafx.swing needed for JGraphX)
java --module-path "javafx-sdk-21.0.1/lib" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing \
  --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED \
  --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED \
  -jar target/InspirationCatcher.jar

# Test AI connection (standalone, not a unit test framework)
javac -d out src/test/java/TestAI.java && java -cp out TestAI
```

**Requirements:** JDK 17+ (project targets Java 24), JavaFX 21.0.1 SDK in project root, SQLite auto-creates `inspiration.db` on first run. On systems with GPU issues, `MainApp.java` sets `prism.order=sw` (software rendering fallback).

## Project Architecture

**Pattern:** Controller → Manager → DAO → SQLite (raw JDBC)

### Entry Point
- `MainApp.java` — Initializes DB, applies AtlantaFX `PrimerLight` theme, loads `MainView.fxml`, sets up global uncaught-exception handler. CSS is loaded in specific cascade order (see below).

### Controllers
- `MainController` — Central orchestrator (singleton via `instance` static field). Initializes all managers and sub-controllers. Handles Navigation Rail switching (3 tabs), AI suggestions (async via `CompletableFuture` + `synchronized` guard), markdown formatting toolbar, mind map panel, and settings dialogs.
- `EditorController` — Markdown editor ↔ WebView preview. Handles save/create/switch modes.
- `QuickCaptureController` — Modal dialog for quick idea entry (bypasses the full editor).
- `FilterController` — Search field, type/importance/mood combos, date pickers, tag filter. Wraps `FilteredList<Idea>`.

### Managers (Business Logic)
- `IdeaManager` — CRUD, title auto-generation from content, AI orchestration. Coordinates with `ProjectManager` and `TagManager`.
- `ProjectManager` — Project switching, statistics, default project creation.
- `MindMapManager` — Graph data (nodes + connections) for the mind map.
- `TableManager` — TableView column setup, selection, detail panel population.
- `TagManager` — Tag CRUD, usage stats, hot tag queries, unused tag cleanup.
- UI helpers: `FontManager`, `ShortcutManager`, `StatusManager`, `EditorModeManager`, `UIConfigManager`.

### Models & Enums
- `Idea` with enums: `IdeaType` (7 values: IDEA/QUOTE/QUESTION/TODO/DISCOVERY/CONFUSION/HYPOTHESIS), `Mood` (10 values), `PrivacyLevel` (3 values)
- `Connection` with `Relationship` enum (6 types: SUPPORTS/OPPOSES/EXTENDS/ANALOGY/CAUSAL/TEMPORAL)
- `MindMapConnection` with `ConnectionType` enum (7 types: RELATED/DEPENDS_ON/EXTENDS/CONTRADICTS/ANALOGY/CAUSAL/USES)

### DAOs (Raw JDBC, no ORM)
- `DatabaseManager` — Singleton connection, creates 7 tables + 3 indexes on init
- `IdeaDao`, `TagDao`, `ProjectDao`, `MindMapNodeDao`, `MindMapConnectionDao`

### Services
- `AIService` — Async DeepSeek API calls (raw HTTP, no SDK). Two modes: `generateSuggestions` (auto) and `generateCustomPrompt` (user-editable prompt). Falls back to offline template on failure.
- `IdeaService` — Thin wrapper around `IdeaDao` (used only by `QuickCaptureController`; `IdeaManager` calls DAO directly).

### Config
- `AIConfig` — Loads `~/.inspiration-catcher/ai.properties`, falls back to classpath `com/inspiration/catcher/config/ai.properties`. Keys: `ai.api.key`, `ai.api.url`, `ai.model`, `ai.max_tokens`, `ai.temperature`.

### Utilities
- `MarkdownUtil` — flexmark parser with tables + strikethrough extensions, renders to HTML with inline CSS
- `DateUtil` — Date formatting helpers

## UI Layout (MainView.fxml)

Three-column SplitPane:
1. **Left sidebar (260px)** — Project selector, search with filters, tag cloud
2. **Center** — TabPane with 3 tabs: idea table → mind map canvas → markdown editor
3. **Right slide panel (280px)** — Idea details, AI suggestions, related ideas, statistics

Navigation Rail on the far left (48px wide, VS Code-style activity bar) switches between center tabs.

## CSS Architecture

Four files loaded in cascade order (each overrides the previous):

1. **`app.css`** — Design system (custom properties for brand, neutral palette, shadows, radius, spacing, typography) + full component library (~1100 lines, 24 sections). VS Code Light+ inspired. Defines CSS classes: `.nav-rail`, `.card`, `.ai-card`, `.idea-card`, `.chat-bubble-user/ai`, `.slide-panel`, `.empty-state`, utility classes (`.p-*`, `.gap-*`, `.text-*`).
2. **`theme.css`** — AtlantaFX `PrimerLight` overrides (SplitPane dividers, dialogs, tooltips)
3. **`components.css`** — Component-specific tweaks (TableView, SplitPane, tag cloud)
4. **`main.css`** — Layout refinements (VBox, AnchorPane, palette accent backgrounds)

## Key Design Details

- **IdeaService vs IdeaManager overlap:** `IdeaService` wraps `IdeaDao` for QuickCaptureController; `IdeaManager` uses `IdeaDao` directly for MainController. They share no common interface.
- **AI is raw HTTP** (no SDK): JSON request body built with string formatting, response parsed via `indexOf("\"content\":\"")` substring extraction. Error-prone but works.
- **Mind map:** Custom `Canvas` renderer in `MindMapView.java`. Nodes dragged from a left-side `VBox` card list. Three node types: IDEA (linked to a saved idea), CONCEPT (freeform), EXTERNAL (URL link).
- **Database path:** `inspiration.db` in the working directory (not user home).
- **Logs:** SLF4J + Logback. Configurable via `logback.xml` if placed on classpath (not currently present).
- **Single test file:** `src/test/java/TestAI.java` is a standalone `main()` method test for the AI API, not a JUnit test.
- **Current state:** ~9,300 lines across 37 source files, 0 compile errors, 0 runtime exceptions.
- **Design report:** See `设计报告.md` in project root.
