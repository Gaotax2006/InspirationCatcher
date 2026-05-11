# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build the project (produces a fat JAR via maven-shade-plugin)
mvn clean package -DskipTests

# Run with JavaFX:
java --module-path "javafx-sdk-21.0.1/lib" --add-modules javafx.controls,javafx.fxml,javafx.web --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED -jar target/InspirationCatcher.jar

# Run a test
javac -d out src/test/java/TestAI.java && java -cp out TestAI
```

**Requirements:** JDK 17+ (project targets Java 24), JavaFX 21.0.1 SDK bundled in `javafx-sdk-21.0.1/`. SQLite database (`inspiration.db`) is auto-created on first launch.

## Project Structure

This is "灵感捕手" (Inspiration Catcher), a JavaFX desktop application for capturing, organizing, and expanding ideas with AI assistance.

### Architecture: Controller → Manager → DAO → SQLite

- **`MainApp.java`** — Entry point, initializes DB, loads FXML, sets up global exception handler
- **Controllers** handle JavaFX UI events and bindings:
  - `MainController` — Hub controller, initializes all managers and sub-controllers
  - `EditorController` — Markdown editor with preview, save/create idea logic
  - `QuickCaptureController` — Modal window for quick idea entry
  - `FilterController` — Search/filter logic for the idea table
- **Managers** contain business logic and coordinate between controllers and DAOs:
  - `IdeaManager` — CRUD operations on ideas, tag management, AI suggestion orchestration
  - `ProjectManager` — Project switching and statistics
  - `MindMapManager` — Mind map graph data (nodes + connections)
  - `TableManager` — TableView configuration and selection
  - `TagManager` — Tag CRUD and usage statistics
  - `FontManager`, `ShortcutManager`, `StatusManager`, `EditorModeManager`, `UIConfigManager` — UI state helpers
- **Models** (`Idea`, `Project`, `Tag`, `MindMapNode`, `MindMapConnection`, `Connection`): Java enums for types, moods, privacy levels, relationship types
- **DAO layer** (raw JDBC on SQLite):
  - `DatabaseManager` — Schema creation (7 tables: projects, ideas, tags, idea_tags, mindmap_nodes, mindmap_connections, connections)
  - `IdeaDao`, `TagDao`, `ProjectDao`, `MindMapNodeDao`, `MindMapConnectionDao`
- **Services**:
  - `AIService` — Asynchronous DeepSeek API calls via `CompletableFuture`
  - `IdeaService` — Thin service wrapper around `IdeaDao`
- **Component**:
  - `MindMapView` — Custom JavaFX canvas-based mind map renderer
- **Config**: `AIConfig` loads from `~/.inspiration-catcher/ai.properties` or classpath resource

### UI Layout (MainView.fxml)

Three-column layout via `SplitPane`:
1. **Left sidebar** — Project selector, search field, filters (type/importance/mood/date/tag), tag cloud
2. **Center** — TabPane with 3 tabs: idea list (TableView), mind map (canvas + draggable idea cards), editor (Markdown editor + WebView preview)
3. **Right panel** — Idea details, AI suggestions, related ideas list, statistics

### Key Design Decisions

- **Raw JDBC + SQLite** (no ORM) — Schema auto-created on launch, SQLite database file `inspiration.db`
- **Contrast between `IdeaService` and `IdeaManager`**: `IdeaService` wraps `IdeaDao` directly (used by QuickCaptureController); `IdeaManager` wraps `IdeaDao` + coordinates with other managers (used by MainController/EditorController). They have overlapping responsibilities.
- **AI via DeepSeek**: Configurable API key/key URL stored in `~/.inspiration-catcher/ai.properties`. UI has settings dialog and test-connection button. AI calls are async via `CompletableFuture`. Two modes: quick suggestions and custom-prompt expansion.
- **Mind map**: Custom canvas rendering with drag-and-drop support from a left-side idea card list. Nodes can be ideas, concepts, or external links. Connections have types (RELATED, DEPENDS_ON, EXTENDS, etc.).
- **Markdown**: Converted to HTML client-side via flexmark library, rendered in a `WebView` preview pane.