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
  * Game Engine (rules, score calculation, card abilities, board state, data parsers) is written in **pure Java**. Any `javafx.*` imports are strictly forbidden within logic packages. Logic must be 100% deterministic, decoupled, and easily unit-testable without initializing the graphics subsystem.
  * The UI layer is solely responsible for rendering, animations, user input interception, and state visualization.
* **Layout and UI Components:**
  * Base screen layouts are designed in FXML for seamless visual editing via Scene Builder.
  * Reusable compound UI elements must use the `<fx:root>` pattern (an FXML file with an `<fx:root>` tag bound to an extending Java component class).
  * Avoid assembling UI structures procedurally in Java code (no spaghetti of `new HBox()`, `new ImageView()`) — Java code should only bind data to markup and trigger animations.
  * **Reference Resolution & Scaling:** All layouts are designed against a reference resolution of 1920x1080 (1080p). Root containers must properly scale using transform scaling (`scale`) to adapt cleanly to various window sizes without distortion.
* **DRY and Code Reuse:**
  * Avoid code duplication. If a low-level sequence of instructions (node manipulations, repetitive condition checks, math calculations) appears 2–3 times, extract it into a clear, reusable utility method.
  * Always inspect the existing codebase for implemented methods and classes before writing new ones; reuse existing domain logic instead of reinventing it.
* **Code Documentation:**
  * Classes and non-trivial methods must include concise JavaDoc comments. Omitting JavaDoc is only acceptable for self-explanatory code (such as trivial getters/setters).
* **Structural Balance:**
  * Avoid monolithic "God classes" spanning hundreds or thousands of lines unless absolutely unavoidable.
  * Avoid over-engineering: do not introduce three-line micro-classes or unnecessary abstraction layers with no practical value.
  * Maintain a sensible middle ground with high readability for human engineers.

## 4. Collaboration Contract with Developer
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