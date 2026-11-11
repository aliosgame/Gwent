# AGENTS.md — Project Context and Guidelines for Gwent JavaFX

## 1. Core Concept and Philosophy
* **Goal:** Create an accurate, authentic recreation of the Gwent card game from The Witcher 3 (the original in-game minigame, not the standalone spin-off) using JavaFX.
* **Authenticity:** Use original game assets (extracted via REDkit), matching visual style, scales, timings, and original rules 1-to-1.
* **Code Quality:** All code must be written from the start as a clean, extensible, and human-readable foundation. No throwaway prototype hacks or crude workarounds. Adhere to common sense, KISS, and YAGNI principles.

## 2. Environment and Resources
* **Tech Stack:** Java 25 (Zulu JDK), JavaFX. Build tool: Gradle (Groovy DSL).
* **Resources:** All textures, audio, fonts, and data files must be bundled inside the JAR. Resource loading is handled **strictly** via the Classpath (`getResourceAsStream` / `getResource`). Never use operating system file paths (`java.io.File`) for bundled assets.
* **User Data:** All user configuration (sound volume, custom user deck lists, and personal settings) must be stored outside the JAR in the user directory: `Path.of(System.getProperty("user.home"), "Aliosgame", "Gwent")`.
* **Distribution:** The project is structured with future packaging and distribution via jDeploy in mind.

## 3. Architectural Principles
* **Strict Separation of UI and Logic:**
  * Game Engine (rules, score calculation, card abilities, board state, data parsers) is written in **pure Java**. Any `javafx.*` imports are strictly forbidden within logic packages. Logic must be 100% deterministic and decoupled so that core rules can run in any environment without a graphics subsystem.
  * The UI layer is solely responsible for rendering, animations, user input interception, and state visualization. Manual in-game testing is the primary feedback mechanism for UI; do not twist UI code into knots to make it unit-testable.
* **Layout and UI Components:**
  * Base screen layouts are designed in FXML for seamless visual editing via Scene Builder.
  * Reusable compound UI elements must use the `<fx:root>` pattern (an FXML file with an `<fx:root>` tag bound to an extending Java component class).
  * Avoid assembling UI structures procedurally in Java code (no spaghetti of `new HBox()`, `new ImageView()`) — Java code should only bind data to markup and trigger animations.
  * **Reference Resolution & Scaling:** All layouts are designed against a reference resolution of 1920x1080 (1080p). Root containers must properly scale using transform scaling (`scale`) to adapt cleanly to various window sizes without distortion.
* **DRY and Code Reuse:**
  * Avoid code duplication. If a low-level sequence of instructions (node manipulations, repetitive condition checks, math calculations) appears 2–3 times, extract it into a clear, reusable utility method.
  * Always inspect the existing codebase for implemented methods and classes before writing new ones; reuse existing domain logic instead of reinventing it.
* **Class Structure and Newspaper Code Style:**
  * Organize class members according to the Newspaper Code Principle (high-level narrative at the top; details lower down):
    1. Constants and fields (`static final`, followed by instance fields).
    2. Constructors and factory / primary initialization methods (`init`).
    3. Public business logic methods (core entry points and intent first).
    4. Standard getters and setters.
    5. Private helper methods.
    6. Singleton boilerplate (if applicable: `get()` and nested `Holder` class, clearly demarcated with a comment block).
    7. Test helpers (if strictly necessary, placed at the very bottom under a clear comment block: `// Getters / helpers for unit tests`).
* **Code Documentation:**
  * Classes and non-trivial methods must include concise JavaDoc comments. Omitting JavaDoc is only acceptable for self-explanatory code (such as trivial getters/setters).
* **Structural Balance:**
  * Avoid monolithic "God classes" spanning hundreds or thousands of lines unless absolutely unavoidable.
  * Avoid over-engineering: do not introduce three-line micro-classes or unnecessary abstraction layers with no practical value.
  * Maintain a sensible middle ground with high readability for human engineers.

## 4. Testing Philosophy (Production Code First)
* **Production Code First:**
  * Production code simplicity, cleanliness, and human readability are paramount. KISS and YAGNI strictly supersede test convenience.
  * Never warp production architecture, split methods into artificial sub-methods, add mock-only interfaces, or overload constructors purely to make code easier to unit-test.
  * Tests must adapt to production code, never production code to tests.
* **Balanced Pragmatism (No Dogmatism):**
  * Avoid extremes: non-invasive, localized accommodations for testing (such as a system property override like `gwent.workdir` in `BaseConfig` or compact package-private getters at the very bottom of a class under a dedicated comment) are acceptable when they don't complicate production logic.
  * However, invasive changes (polluting public APIs, method splitting, adding abstraction layers) are strictly forbidden.
* **Sensible Testing Scope (No Extremes in Either Direction):**
  * Do not chase 100% test coverage, but do not shun tests either: unit and integration tests for utilities, configs, UI controllers, and screen transitions are completely welcome whenever they can be implemented cleanly and painlessly without distorting production code.
  * The rule of thumb: if a test can be written without harming production code simplicity, write it. If testing requires messy workarounds, splitting methods, or complicating production logic, skip the unit test in favor of manual in-game verification.
  * The highest priority for automated tests remains non-trivial game engine rules, card calculations, state transitions, and regression scenarios.
  * If critical domain logic is genuinely hard to test cleanly, discuss it with the developer upfront rather than hacking production architecture.
* **Freedom in Test Code:**
  * Code in `src/test/` has much more leeway to be pragmatic (reflection, test fixtures, headless platforms). Keep perfectionism in `src/main`.

## 5. Collaboration Contract with Developer
* **Communication Style:** Informal tone using "ты" (informal second-person singular), in Russian. Friendly and collaborative, but without blind sycophancy: the agent is encouraged to offer reasoned technical criticism and propose superior alternatives.
* **Git Management:**
  * The agent **NEVER runs `git commit` autonomously** unless explicitly instructed to commit by the user.
  * The agent automatically stages new and modified files (`git add`) relevant to the current task, keeping the staging area ready for review.
* **Analysis and Scratchpad Files:**
  * All auxiliary research documents, reports, benchmark notes, temporary HTML/MD summaries, and drafts must be saved strictly in the `.ANALYZE/` directory at the project root.
  * The `.ANALYZE/` folder is listed in `.gitignore` and must never be committed.
* **Development Workflow:**
  * Implement code in small, logically complete increments.
  * Before introducing significant architectural changes, briefly outline the intent and wait for user confirmation.
  * The developer personally reviews and approves all generated code.
