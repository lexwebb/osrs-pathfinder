# OSRS Pathfinder 🧭

**OSRS Pathfinder** is an intelligent next-step adventure advisor, North Star goal pathfinder, and progression engine for *Old School RuneScape*.

Instead of pushing players down hyper-efficient, monotonous paths or leaving them stuck in decision paralysis, Pathfinder analyzes your account restrictions, combat and skill levels, completed quests, and scanned bank items to uncover **engaging alternative activities, hidden-gem minigames, near-term unlocks, entry bossing opportunities, and clear training pathways toward your long-term goals**.

---

## 🌟 Key Features

### 1. 🎯 Universal North Star Goal Pathfinding
Set any major milestone as your active **North Star**, and Pathfinder will dynamically construct your roadmap:
- **Tiered Combat Checkpoints**: Early, Mid, and Late setups for Melee, Ranged, and Magic (e.g. Zombie Axe, Fighter Torso, Dragon Defender, Glory, Fire Cape, Barrows Gloves).
- **Boss Readiness Checkpoints**: Preparation profiles for Zulrah, Vorkath, Corrupted Gauntlet, Phantom Muspah, and Tombs of Amascut.
- **Quest & Gear Milestones**: Set grandmaster quests or individual gear pieces as your target.
- **Dynamic Gap Analysis**: Automatically calculates missing skill levels, incomplete prerequisite quests, and unowned gear.
- **Target Pathways**: Automatically badges training activities with gold pathway tags (e.g. `🎯 Pathway for Mid-Game Melee: Slayer 50 → 58 (+8 lvls)`) and provides a **"Show Pathways Only"** toggle.

### 2. 🛡️ Bronzeman & Ironman Realism
- **Mode-Aware Analysis**: Automatically detects Ironman, Hardcore, Ultimate, or Bronzeman accounts.
- **Organic Requirements**: For Bronzemen and Ironmen, unowned gear goals enforce their true organic gathering requirements (e.g., 90 Crafting & 87 Magic for Amulet of Fury, 85 Slayer for Abyssal Whip, 83 Slayer for Dragon Boots, *Recipe for Disaster* for Barrows Gloves).
- **Slot-Tier Equivalence**: Owning a higher-tier item (such as an Abyssal Whip or Fang) automatically fulfills lower-tier requirements (like Zombie Axe or Rune Scimitar).

### 3. 💰 Bank, Inventory & Gear Ingestion
- Automatically scans your Bank, Inventory, and Equipment when opening a bank booth.
- Tracks liquid GP cash stacks and total estimated wealth.
- Excludes gear you already own from recommendations and filters out completed quests and achievement diaries.

### 4. ⚔️ Slayer Training Progression
- Dedicated Slayer training activities including **Konar quo Maten**, **Chaeldar**, **Nieve / Steve**, **Duradel**, **Catacombs Bursting**, and **Wilderness Slayer (Krystilia)**.
- Automatically surfaces optimal Slayer masters whenever a Slayer level gap is detected for an active North Star goal.

### 5. 🎲 Pathfinder Roulette ("Surprise Me!")
- One-click randomized roll from eligible, ready-to-do content when you want an immediate 30-minute adventure without overthinking.

### 6. 💾 Offline Profile Browsing
- Automatically caches your last logged-in character profile via RuneLite's `ConfigManager`.
- Browse recommendations, plan gear setups, and configure goals offline without needing to be logged into the game.

### 7. 🎨 Native RuneLite UI
- Designed strictly to RuneLite's 225px sidebar standard with zero text overlap, balanced controls, responsive chip wrapping ([`WrapLayout`](file:///home/lex/Work/osrs-pathfinder/src/main/java/com/pathfinder/ui/WrapLayout.java)), and high-contrast color coding.

---

## 📁 Project Structure

```
osrs-pathfinder/
├── build.gradle                   # Gradle build targeting Java 11 / RuneLite client
├── settings.gradle
├── gradle.properties              # Configured toolchain settings
├── runelite-plugin.properties     # Official Plugin Hub metadata
├── LICENSE                        # BSD 2-Clause License
├── src/
│   ├── main/
│   │   ├── java/com/pathfinder/
│   │   │   ├── PathfinderPlugin.java       # Plugin lifecycle & client event hooks
│   │   │   ├── PathfinderConfig.java       # Config options & toggles
│   │   │   ├── model/
│   │   │   │   ├── AccountProfile.java     # Immutable snapshot of skills, quests, and bank
│   │   │   │   ├── Activity.java           # Activity schema (rewards, tags, trained skills)
│   │   │   │   ├── ActivityCategory.java   # Quests, Skilling, Bossing, Minigames, Diaries, Gear
│   │   │   │   ├── BankSnapshot.java       # Scanned wealth and owned item IDs
│   │   │   │   ├── BossCheckpoint.java     # Boss readiness requirement models
│   │   │   │   ├── CombatCheckpoint.java   # Style-tiered combat setup models
│   │   │   │   ├── GearUpgrade.java        # Gear slot, equip reqs, and organic sources
│   │   │   │   ├── NorthStarGoal.java      # Active target goal data model
│   │   │   │   ├── NorthStarProgress.java  # Gap analysis (skills, quests, gear)
│   │   │   │   ├── Recommendation.java     # Evaluated activity with readiness status & score
│   │   │   │   └── SkillRequirement.java   # Skill + level pair
│   │   │   ├── engine/
│   │   │   │   ├── ActivityDatabase.java   # Curated database loader for JSON assets
│   │   │   │   └── RecommendationEngine.java # Evaluation, scoring, filtering & pathway logic
│   │   │   └── ui/
│   │   │       ├── DynamicGridLayout.java  # RuneLite responsive vertical layout
│   │   │       ├── NorthStarCard.java      # Active North Star progress & gap widget
│   │   │       ├── PathfinderPanel.java     # Main RuneLite sidebar panel
│   │   │       ├── RecommendationCard.java  # Visual activity/gear recommendation card
│   │   │       └── WrapLayout.java          # Dynamic chip-wrapping layout manager
│   │   └── resources/com/pathfinder/
│   │       ├── activities.json              # Curated skilling, slayer, bossing, and minigames
│   │       ├── boss_checkpoints.json        # Boss readiness profiles (Zulrah, CG, ToA, etc.)
│   │       ├── combat_checkpoints.json      # Combat gear tiers (Melee, Ranged, Magic)
│   │       ├── gear_upgrades.json           # Gear milestones and organic requirements
│   │       └── icon.png                     # Navigation toolbar icon
│   └── test/
│       └── java/com/pathfinder/
│           ├── PathfinderPluginTest.java    # Full RuneLite client launcher
│           ├── RecommendationEngineTest.java# Unit test suite for recommendation logic
│           ├── NorthStarEngineTest.java     # Unit test suite for North Star & Bronzeman logic
│           └── ui/
│               └── PathfinderPreview.java   # Standalone 1-second instant UI preview runner
```

---

## 🚀 Development & Usage

### Prerequisites
- JDK 11 or 17 (Java 17 recommended)
- Gradle (handled automatically via `./gradlew`)

### Running the Plugin in RuneLite
To launch the full RuneLite client in developer mode with Pathfinder loaded:
```bash
./gradlew run
```

### Instant UI Preview (<1 Second)
To launch a lightweight preview window of the exact 225px sidebar panel with mock account data (without launching RuneLite or logging in):
```bash
./gradlew preview
```

### Running Automated Tests
```bash
./gradlew test
```

### Building the Plugin JAR
```bash
./gradlew shadowJar
```

---

## 🗺️ Roadmap & Current Status

- [x] **Phase 1: Core Architecture & UI**: Plugin lifecycle, responsive sidebar UI, readiness calculation (`READY`, `ALMOST_READY`, `LOCKED`), and Pathfinder Roulette.
- [x] **Phase 2: Bank & Economy Awareness**: Inventory, equipment, and bank container scanning, liquid GP tracking, and owned-item filtering.
- [x] **Phase 3: North Star Goal Pathfinding**: Tiered Combat Checkpoints (Melee/Ranged/Magic), Boss Readiness profiles, gap analysis, and pathway recommendation tagging.
- [x] **Phase 4: Bronzeman & Ironman Realism**: Organic drop and crafting requirement discovery (e.g. Fury, Whip, Dragon Boots) and slot-tier equivalence.
- [x] **Phase 5: Slayer Progression & Training**: Added comprehensive Slayer masters and burst training methods.
- [x] **Phase 6: Official Plugin Hub Submission**: Submitted to the official RuneLite Plugin Hub via PR [#20283](https://github.com/runelite/plugin-hub/pull/20283) with all CI checks passing.
- [ ] **Phase 7 (Future Enhancements)**:
  - Skill goal setting (e.g., target 70 Construction for SOTE) directly from the skill tab.
  - Custom user-defined gear and boss preset checkpoints.
  - Interactive Wiki popup integration via RuneLite client browser.

---

## 📄 License

This project is licensed under the open-source **BSD 2-Clause License** — see the [LICENSE](LICENSE) file for details.
