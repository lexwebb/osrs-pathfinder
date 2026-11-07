# OSRS Pathfinder 🧭

**OSRS Pathfinder** is a RuneLite plugin designed to break decision paralysis and repetitive grinding in *Old School RuneScape*. 

Instead of pushing players down hyper-efficient, monotonous paths (like tick-manipulation mining or identical quest orders), Pathfinder analyzes your account restrictions, combat/skill levels, and quest completions to uncover **engaging alternative activities, hidden-gem minigames, near-term unlocks, and entry-level bossing opportunities**.

---

## 🌟 Features

- **Account Ingestion:** Automatically inspects your character's current skill levels, total level, combat level, completed quests, and account restrictions (Ironman, Hardcore, etc.).
- **Smart Readiness Engine:**
  - `READY`: All requirements met — start right away.
  - `NEAR UNLOCK`: Within 1–4 skill levels or 1 prerequisite quest away.
  - `LOCKED`: Long-term goals.
- **Focus Lenses & Filters:**
  - Filter by category: *Quests & Unlocks*, *Engaging Skilling*, *Entry & Mid Bossing*, *Minigames & Novelty*, *Achievement Diaries*.
  - **"Hidden Gems Only"**: Surfaces off-the-beaten-path content (Mahogany Homes, Volcanic Mine, Hunter Rumours, Giants' Foundry, etc.).
  - **"Ready to do now"**: Instantly hides content you haven't unlocked yet.
- **🎲 Pathfinder Roulette ("Surprise Me!"):**
  - One-click randomized roll from eligible, ready-to-do content when you want an immediate 30-minute adventure without thinking.
- **Native RuneLite Sidebar UI:**
  - Compact sidebar integration matching RuneLite's color scheme and styling.

---

## 📁 Project Structure

```
osrs-pathfinder/
├── build.gradle                   # Gradle build script targeting Java 11 / RuneLite client
├── settings.gradle
├── gradle.properties              # Configured JDK 17 toolchain
├── runelite-plugin.properties     # Plugin hub metadata
├── src/
│   ├── main/
│   │   ├── java/com/pathfinder/
│   │   │   ├── PathfinderPlugin.java       # RuneLite plugin lifecycle & game event hooks
│   │   │   ├── PathfinderConfig.java       # User preferences & toggles
│   │   │   ├── model/
│   │   │   │   ├── Activity.java           # Activity data model (rewards, requirements, tags)
│   │   │   │   ├── ActivityCategory.java   # Category definitions
│   │   │   │   ├── AccountProfile.java     # Immutable snapshot of player stats/quests
│   │   │   │   ├── SkillRequirement.java   # Skill + level pair
│   │   │   │   └── Recommendation.java     # Evaluated activity with status & scoring
│   │   │   ├── engine/
│   │   │   │   ├── ActivityDatabase.java   # Curated database of engaging content
│   │   │   │   └── RecommendationEngine.java # Matching, scoring, filtering & roulette logic
│   │   │   └── ui/
│   │   │       ├── PathfinderPanel.java     # RuneLite sidebar panel & action controls
│   │   │       └── RecommendationCard.java  # Visual card component for activities
│   │   └── resources/
│   │       └── com/pathfinder/
│   │           └── icon.png                 # Navigation bar icon
│   └── test/
│       └── java/com/pathfinder/
│           ├── PathfinderPluginTest.java    # Local developer client launcher
│           └── RecommendationEngineTest.java# Unit test suite
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 11 or 17 (Java 17 LTS recommended)
- Gradle (handled automatically via `./gradlew`)

### Running the Plugin Locally

To start RuneLite in developer mode with Pathfinder loaded:

```bash
./gradlew run
```

### Running the Test Suite

```bash
./gradlew test
```

### Building the Plugin JAR

```bash
./gradlew shadowJar
```

---

## 🗺️ Roadmap: Wiki Scraping & Knowledge Graph

1. **Phase 1 (Complete):** Core plugin architecture, RuneLite sidebar UI, event listeners, and curated activity database.
2. **Phase 2 (Bank & Economy Awareness):** Hook into `InventoryID.BANK` snapshot to detect available cash stack and suggest gear upgrades (e.g. Barrows armor, Obsidian, Tridents).
3. **Phase 3 (Wiki Data Ingestion Pipeline):**
   - Offline python script to query OSRS Wiki MediaWiki Cargo tables (`osrs.wiki/w/Special:CargoTables`) and Weird Gloop API.
   - Automatically compile item requirements, quest prerequisite graphs, and boss weaknesses into bundled JSON assets.
