#!/usr/bin/env python3
"""
OSRS Pathfinder - Wiki Scraper & Data Pipeline
Extracts structured game activities, minigames, bosses, and gear milestones
from the official Old School RuneScape Wiki API and Weird Gloop price API.
"""

import json
import os
import sys
import urllib.request
import urllib.parse
from typing import Dict, List, Any

USER_AGENT = "OSRS-Pathfinder-DataPipeline/1.0 (contact: github.com/pathfinder)"

CURATED_ACTIVITIES = [
    # Engaging Skilling & Minigames
    {
        "id": "mahogany-homes",
        "wiki_title": "Mahogany Homes",
        "category": "SKILLING",
        "unlock_highlight": "Carpenter Outfit (+2.5% XP) & Plank Sack",
        "recommended_combat": 1,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "CONSTRUCTION", "level": 20}],
        "quest_reqs": [],
        "tags": ["Construction", "Contract", "Chill", "Cost-effective"]
    },
    {
        "id": "giants-foundry",
        "wiki_title": "Giants' Foundry",
        "category": "SKILLING",
        "unlock_highlight": "Smithing Outfit & Colossal Blade",
        "recommended_combat": 1,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "SMITHING", "level": 15}],
        "quest_reqs": ["Sleeping Giants"],
        "tags": ["Smithing", "Minigame", "Profit", "Ironman Favorite"]
    },
    {
        "id": "guardians-of-the-rift",
        "wiki_title": "Guardians of the Rift",
        "category": "MINIGAME",
        "unlock_highlight": "Raiments of the Eye (+60% runes) & Abyssal Needle",
        "recommended_combat": 1,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "RUNECRAFT", "level": 27}],
        "quest_reqs": ["Temple of the Eye"],
        "tags": ["Runecraft", "Co-op", "Outfit", "High Profit"]
    },
    {
        "id": "tempoross",
        "wiki_title": "Tempoross",
        "category": "BOSSING",
        "unlock_highlight": "Fish Barrel (stores 28 raw fish) & Tackle Box",
        "recommended_combat": 1,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "FISHING", "level": 35}],
        "quest_reqs": [],
        "tags": ["Fishing", "Boss", "Safe Death", "Food Supplies"]
    },
    {
        "id": "hunter-rumours",
        "wiki_title": "Hunter Guild",
        "category": "SKILLING",
        "unlock_highlight": "Huntsman's Kit, Guild Hunter Outfit & Quetzal Whistle",
        "recommended_combat": 1,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "HUNTER", "level": 46}],
        "quest_reqs": ["Children of the Sun"],
        "tags": ["Hunter", "Varlamore", "Variety", "Whistle"]
    },
    {
        "id": "volcanic-mine",
        "wiki_title": "Volcanic Mine",
        "category": "MINIGAME",
        "unlock_highlight": "Ash Covered Tome & 80k+ Mining XP/hr",
        "recommended_combat": 50,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "MINING", "level": 50}],
        "quest_reqs": ["Bone Voyage"],
        "tags": ["Mining", "High XP", "Teamwork", "Alternative"]
    },
    {
        "id": "hallowed-sepulchre",
        "wiki_title": "Hallowed Sepulchre",
        "category": "SKILLING",
        "unlock_highlight": "Ring of Endurance & Best Agility XP/GP in game",
        "recommended_combat": 60,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "AGILITY", "level": 52}, {"skill": "THIEVING", "level": 52}],
        "quest_reqs": ["Sins of the Father"],
        "tags": ["Agility", "High Skill", "Huge Profit", "Action"]
    },
    {
        "id": "shades-of-mortton",
        "wiki_title": "Shades of Mort'ton (minigame)",
        "category": "MINIGAME",
        "unlock_highlight": "Zealot's Robes (save bones/ensouled heads) & Amulet of the Damned",
        "recommended_combat": 40,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "CRAFTING", "level": 20}, {"skill": "HERBLORE", "level": 15}],
        "quest_reqs": ["Shades of Mort'ton"],
        "tags": ["Prayer", "Unique Rewards", "Morytania", "Classic"]
    },
    {
        "id": "wintertodt",
        "wiki_title": "Wintertodt",
        "category": "BOSSING",
        "unlock_highlight": "Tome of Fire & Pyromancer Warm Outfit (+2.5% Firemaking XP)",
        "recommended_combat": 1,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "FIREMAKING", "level": 50}],
        "quest_reqs": [],
        "tags": ["Firemaking", "Skilling Boss", "Supplies", "Fast XP"]
    },
    # Bossing & Combat Milestones
    {
        "id": "scurrius",
        "wiki_title": "Scurrius",
        "category": "BOSSING",
        "unlock_highlight": "Bone Weapons (massive combat XP) & Scurrius' Spine",
        "recommended_combat": 40,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "PRAYER", "level": 43}],
        "quest_reqs": [],
        "tags": ["PvM Tutorial", "Prayer Flicking", "Fast XP", "Solo"]
    },
    {
        "id": "barrows",
        "wiki_title": "Barrows",
        "category": "BOSSING",
        "unlock_highlight": "Iconic Barrows Sets (Dharok, Ahrim, Karil, Guthan)",
        "recommended_combat": 60,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "MAGIC", "level": 50}, {"skill": "PRAYER", "level": 43}],
        "quest_reqs": ["Priest in Peril"],
        "tags": ["Classic", "Midgame Armor", "High Value", "Solo"]
    },
    {
        "id": "perilous-moons",
        "wiki_title": "Moons of Peril",
        "category": "BOSSING",
        "unlock_highlight": "Blood, Blue & Eclipse Moon Armor Sets (Level 75 Gear)",
        "recommended_combat": 75,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "ATTACK", "level": 65}, {"skill": "DEFENCE", "level": 70}],
        "quest_reqs": ["Perilous Moons", "Children of the Sun"],
        "tags": ["Varlamore", "Boss Trio", "Free Supplies", "Midgame BiS"]
    },
    {
        "id": "sarachnis",
        "wiki_title": "Sarachnis",
        "category": "BOSSING",
        "unlock_highlight": "Sarachnis Cudgel (Crush Weapon) & Red Spider Egg Sacks",
        "recommended_combat": 70,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "PRAYER", "level": 43}, {"skill": "ATTACK", "level": 60}, {"skill": "STRENGTH", "level": 60}],
        "quest_reqs": [],
        "tags": ["Spider", "Herblore Supplies", "Crush Weapon", "Midgame"]
    },
    {
        "id": "dagannoth-kings",
        "wiki_title": "Dagannoth Kings",
        "category": "BOSSING",
        "unlock_highlight": "Berserker Ring, Archers Ring & Seers Ring",
        "recommended_combat": 80,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "PRAYER", "level": 43}, {"skill": "MAGIC", "level": 60}, {"skill": "RANGED", "level": 60}],
        "quest_reqs": ["The Fremennik Trials", "Waterbirth Island"],
        "tags": ["Trio Boss", "BiS Rings", "Fremennik", "Team or Solo"]
    },
    # Quests & Crucial Unlocks
    {
        "id": "fairy-rings",
        "wiki_title": "Fairytale II - Cure a Queen",
        "category": "QUEST",
        "unlock_highlight": "Instant teleportation access across almost all corners of Gielinor",
        "recommended_combat": 30,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [],
        "quest_reqs": ["Lost City", "Nature Spirit", "Fairytale I - Growing Pains"],
        "tags": ["Essential", "Teleportation", "Huge QoL"]
    },
    {
        "id": "bone-voyage",
        "wiki_title": "Bone Voyage",
        "category": "QUEST",
        "unlock_highlight": "Fossil Island: Birdhouse Runs, Ammonite Crabs & Volcanic Mine",
        "recommended_combat": 25,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [],
        "quest_reqs": ["The Dig Site"],
        "tags": ["Birdhouses", "Passive Hunter", "Top Skilling Hub"]
    },
    {
        "id": "dragon-slayer-i",
        "wiki_title": "Dragon Slayer I",
        "category": "QUEST",
        "unlock_highlight": "Wear Rune Platebody, Green D'hide Body & Anti-Dragon Shield",
        "recommended_combat": 35,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "CRAFTING", "level": 8}],
        "quest_reqs": [],
        "tags": ["Free-to-Play Finale", "Armor Unlock", "Iconic"]
    },
    {
        "id": "monkey-madness-i",
        "wiki_title": "Monkey Madness I",
        "category": "QUEST",
        "unlock_highlight": "Wield Dragon Scimitar & Ape Atoll Access (great chinning dungeon)",
        "recommended_combat": 50,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [],
        "quest_reqs": ["The Grand Tree", "Tree Gnome Village"],
        "tags": ["Dragon Scimitar", "Ape Atoll", "Iconic Quest"]
    },
    {
        "id": "recipe-for-disaster",
        "wiki_title": "Recipe for Disaster",
        "category": "QUEST",
        "unlock_highlight": "Barrows Gloves (+12 all offensive, +12 defense, +12 strength)",
        "recommended_combat": 70,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [
            {"skill": "COOKING", "level": 70},
            {"skill": "AGILITY", "level": 48},
            {"skill": "MINING", "level": 50},
            {"skill": "FISHING", "level": 53},
            {"skill": "THIEVING", "level": 53},
            {"skill": "HERBLORE", "level": 25},
            {"skill": "MAGIC", "level": 59}
        ],
        "quest_reqs": ["Cook's Assistant", "Desert Treasure I", "Legends' Quest"],
        "tags": ["Barrows Gloves", "Best-in-Slot", "Culinaromancer"]
    },
    # Diaries
    {
        "id": "ardougne-easy-diary",
        "wiki_title": "Ardougne Diary",
        "category": "DIARY",
        "unlock_highlight": "Ardougne Cloak 1: Unlimited teleports near an altar & fairy ring",
        "recommended_combat": 20,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "THIEVING", "level": 15}, {"skill": "FISHING", "level": 15}],
        "quest_reqs": ["Biohazard", "Rune Mysteries"],
        "tags": ["Diary", "Unlimited Teleport", "Early Game Game-Changer"]
    },
    {
        "id": "varrock-medium-diary",
        "wiki_title": "Varrock Diary",
        "category": "DIARY",
        "unlock_highlight": "30 Discounted Battlestaves daily from Zaff (~60k GP daily passive profit)",
        "recommended_combat": 40,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "AGILITY", "level": 30}, {"skill": "FARMING", "level": 30}, {"skill": "FIREMAKING", "level": 40}, {"skill": "THIEVING", "level": 25}],
        "quest_reqs": ["Garden of Tranquillity", "A Soul's Bane"],
        "tags": ["Daily GP", "Battlestaves", "Varrock Teleport Grand Exchange"]
    }
]

GEAR_MILESTONES = [
    {
        "id": "dragon-scimitar",
        "name": "Dragon Scimitar",
        "slot": "WEAPON",
        "item_id": 4587,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "quest_req": "Monkey Madness I",
        "estimated_price": 100000,
        "ironman_source": "Purchased from Daga's Scimitar Smithy on Ape Atoll for 100,000 coins",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_scimitar",
        "highlight": "The premier mid-game slash weapon for training Strength"
    },
    {
        "id": "fighter-torso",
        "name": "Fighter Torso",
        "slot": "BODY",
        "item_id": 10551,
        "recommended_combat": 60,
        "skill_reqs": [{"skill": "DEFENCE", "level": 40}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Earned by playing Barbarian Assault minigame (375 points in all roles + Penance Queen kill)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Fighter_torso",
        "highlight": "+4 Melee Strength bonus (equivalent to Bandos Chestplate!) without spending millions"
    },
    {
        "id": "dragon-defender",
        "name": "Dragon Defender",
        "slot": "SHIELD",
        "item_id": 12954,
        "recommended_combat": 65,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}, {"skill": "DEFENCE", "level": 60}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Warriors' Guild basement cyclopes drop (requires 130 combined Attack + Strength to enter guild)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_defender",
        "highlight": "+6 Melee Strength and massive offensive accuracy in shield slot"
    },
    {
        "id": "abyssal-whip",
        "name": "Abyssal Whip",
        "slot": "WEAPON",
        "item_id": 4151,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "ATTACK", "level": 70}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 85}, {"skill": "ATTACK", "level": 70}],
        "quest_req": "",
        "estimated_price": 1800000,
        "ironman_source": "Slayer drop from Abyssal Demons (requires 85 Slayer) or Abyssal Sire",
        "wiki_url": "https://oldschool.runescape.wiki/w/Abyssal_whip",
        "highlight": "Essential high-DPS 4-tick weapon for general melee and Slayer"
    },
    {
        "id": "barrows-gloves",
        "name": "Barrows Gloves",
        "slot": "HANDS",
        "item_id": 7462,
        "recommended_combat": 70,
        "skill_reqs": [{"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "COOKING", "level": 70}, {"skill": "MAGIC", "level": 59}],
        "quest_req": "Recipe for Disaster",
        "estimated_price": 130000,
        "ironman_source": "Purchased from the Culinaromancer's Chest for 130,000 coins after completing Recipe for Disaster",
        "wiki_url": "https://oldschool.runescape.wiki/w/Barrows_gloves",
        "highlight": "Best-in-slot all-around hybrid gloves (+12 Strength, +12 ranged/magic accuracy, high defensive stats)"
    },
    {
        "id": "fire-cape",
        "name": "Fire Cape",
        "slot": "CAPE",
        "item_id": 6570,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "RANGED", "level": 70}, {"skill": "PRAYER", "level": 43}, {"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 70}, {"skill": "PRAYER", "level": 43}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Defeat TzTok-Jad in the TzHaar Fight Cave (requires ~63 waves of prayer/ranged combat)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Fire_cape",
        "highlight": "+4 Melee Strength and high defensive bonuses, iconic rite of passage"
    },
    {
        "id": "amulet-of-fury",
        "name": "Amulet of Fury",
        "slot": "NECK",
        "item_id": 6585,
        "recommended_combat": 60,
        "skill_reqs": [],
        "organic_skill_reqs": [{"skill": "CRAFTING", "level": 90}, {"skill": "MAGIC", "level": 87}],
        "quest_req": "",
        "estimated_price": 2800000,
        "ironman_source": "Cut an Onyx (90 Crafting) purchased with Tokkul, and enchant with Lvl-6 Enchant (87 Magic)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Amulet_of_fury",
        "highlight": "Supreme hybrid amulet with +8 Melee/Ranged/Magic attack, +15 defense, and +8 Melee Strength"
    },
    {
        "id": "toxic-blowpipe",
        "name": "Toxic Blowpipe",
        "slot": "WEAPON",
        "item_id": 12926,
        "recommended_combat": 80,
        "skill_reqs": [{"skill": "RANGED", "level": 75}, {"skill": "AGILITY", "level": 56}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 75}, {"skill": "MAGIC", "level": 75}, {"skill": "FLETCHING", "level": 53}],
        "quest_req": "Regicide",
        "estimated_price": 3200000,
        "ironman_source": "Zulrah boss drop (Tanzanite fang crafted with 53 Fletching)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Toxic_blowpipe",
        "highlight": "Extremely fast 2-tick ranged weapon with venom and healing special attack"
    },
    {
        "id": "trident-of-the-seas",
        "name": "Trident of the Seas",
        "slot": "WEAPON",
        "item_id": 11907,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "MAGIC", "level": 75}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 87}, {"skill": "MAGIC", "level": 75}],
        "quest_req": "",
        "estimated_price": 750000,
        "ironman_source": "Cave Kraken slayer drop (requires 87 Slayer)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Trident_of_the_seas",
        "highlight": "Powered staff that attacks at 4-tick speed without needing standard spellbook casts"
    }
]


def fetch_wiki_extract(wiki_title: str) -> str:
    """Fetch introductory summary text from OSRS Wiki API"""
    try:
        url = (
            "https://oldschool.runescape.wiki/api.php?"
            + urllib.parse.urlencode({
                "action": "query",
                "prop": "extracts",
                "exintro": "1",
                "explaintext": "1",
                "titles": wiki_title,
                "format": "json"
            })
        )
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=5) as response:
            data = json.loads(response.read().decode("utf-8"))
            pages = data.get("query", {}).get("pages", {})
            for page in pages.values():
                extract = page.get("extract", "").strip()
                if extract:
                    # Clean up first 1-2 sentences
                    first_para = extract.split("\n")[0]
                    return first_para[:350]
    except Exception as e:
        print(f"[WARN] Failed fetching wiki extract for {wiki_title}: {e}", file=sys.stderr)
    return ""


def main():
    print("🧭 Starting OSRS Pathfinder Wiki Scraper & Data Pipeline...")

    enriched_activities = []
    for act in CURATED_ACTIVITIES:
        wiki_title = act["wiki_title"]
        print(f" -> Querying Wiki for: {wiki_title}")
        extract = fetch_wiki_extract(wiki_title)
        
        wiki_slug = urllib.parse.quote(wiki_title.replace(" ", "_"))
        wiki_url = f"https://oldschool.runescape.wiki/w/{wiki_slug}"

        activity_record = {
            "id": act["id"],
            "title": wiki_title.replace(" (minigame)", ""),
            "wikiUrl": wiki_url,
            "category": act["category"],
            "description": extract if extract else f"Engaging OSRS activity for {wiki_title}.",
            "unlockHighlight": act["unlock_highlight"],
            "recommendedCombat": act["recommended_combat"],
            "ironmanFriendly": act["ironman_friendly"],
            "hiddenGem": act["is_hidden_gem"],
            "skillRequirements": act["skill_reqs"],
            "questRequirements": act["quest_reqs"],
            "tags": act["tags"]
        }
        enriched_activities.append(activity_record)

    # Output directory in RuneLite plugin resources
    res_dir = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "com", "pathfinder")
    os.makedirs(res_dir, exist_ok=True)

    activities_path = os.path.join(res_dir, "activities.json")
    with open(activities_path, "w", encoding="utf-8") as f:
        json.dump(enriched_activities, f, indent=2)
    print(f"✅ Generated {len(enriched_activities)} activities -> {activities_path}")

    gear_path = os.path.join(res_dir, "gear_upgrades.json")
    with open(gear_path, "w", encoding="utf-8") as f:
        json.dump(GEAR_MILESTONES, f, indent=2)
    print(f"✅ Generated {len(GEAR_MILESTONES)} gear milestones -> {gear_path}")


if __name__ == "__main__":
    main()
