#!/usr/bin/env python3
"""
OSRS Pathfinder - Wiki Scraper & Data Pipeline
Extracts structured game activities, minigames, bosses, and extensive gear tier milestones
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
    {
        "id": "tithe-farm",
        "wiki_title": "Tithe Farm",
        "category": "MINIGAME",
        "unlock_highlight": "Herb Sack, Seed Box & Farmer's Outfit",
        "recommended_combat": 1,
        "is_hidden_gem": True,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "FARMING", "level": 34}],
        "quest_reqs": [],
        "tags": ["Farming", "Active Farming", "Herb Sack", "Seed Box"]
    },
    {
        "id": "blast-furnace",
        "wiki_title": "Blast Furnace",
        "category": "SKILLING",
        "unlock_highlight": "Cuts coal cost in half & creates bars at 5,000+ bars/hr",
        "recommended_combat": 1,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "SMITHING", "level": 30}],
        "quest_reqs": ["The Giant Dwarf"],
        "tags": ["Smithing", "Huge Profit", "Keldagrim", "Efficient"]
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
    {
        "id": "vorkath",
        "wiki_title": "Vorkath",
        "category": "BOSSING",
        "unlock_highlight": "Vorkath's Head (Ava's Assembler) & Draconic Visage",
        "recommended_combat": 95,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "RANGED", "level": 80}, {"skill": "PRAYER", "level": 74}],
        "quest_reqs": ["Dragon Slayer II"],
        "tags": ["Dragon", "High Profit", "Assembler", "Solo"]
    },
    {
        "id": "zulrah",
        "wiki_title": "Zulrah",
        "category": "BOSSING",
        "unlock_highlight": "Blowpipe, Magic Fang (Trident swamp) & Serpentine Helm",
        "recommended_combat": 85,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [{"skill": "MAGIC", "level": 75}, {"skill": "RANGED", "level": 75}, {"skill": "AGILITY", "level": 56}],
        "quest_reqs": ["Regicide"],
        "tags": ["Snake Boss", "BiS Weapons", "Morytania", "Solo"]
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
    {
        "id": "desert-treasure-i",
        "wiki_title": "Desert Treasure I",
        "category": "QUEST",
        "unlock_highlight": "Ancient Magicks Spellbook (Ice Barrage, Smoke & Shadow Spells)",
        "recommended_combat": 60,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [
            {"skill": "MAGIC", "level": 50},
            {"skill": "THIEVING", "level": 53},
            {"skill": "FIREMAKING", "level": 50},
            {"skill": "SLAYER", "level": 10}
        ],
        "quest_reqs": ["The Dig Site", "Temple of Ikov", "The Tourist Trap", "Priest in Peril", "Waterfall Quest"],
        "tags": ["Ancient Magicks", "Bursting", "Iconic Quest"]
    },
    {
        "id": "song-of-the-elves",
        "wiki_title": "Song of the Elves",
        "category": "QUEST",
        "unlock_highlight": "Prifddinas Elven City: The Gauntlet, Zalcano & Crystal Armour/BofA",
        "recommended_combat": 85,
        "is_hidden_gem": False,
        "ironman_friendly": True,
        "skill_reqs": [
            {"skill": "AGILITY", "level": 70},
            {"skill": "CONSTRUCTION", "level": 70},
            {"skill": "FARMING", "level": 70},
            {"skill": "HERBLORE", "level": 70},
            {"skill": "HUNTER", "level": 70},
            {"skill": "MINING", "level": 70},
            {"skill": "SMITHING", "level": 70},
            {"skill": "WOODCUTTING", "level": 70}
        ],
        "quest_reqs": ["Mourning's End Part II", "Making History"],
        "tags": ["Prifddinas", "Grandmaster", "Gauntlet", "Crystal Equipment"]
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

# Comprehensive OSRS Combat Gear Milestones & Tiers
GEAR_MILESTONES = [
    # --- MELEE WEAPONS ---
    {
        "id": "rune-scimitar",
        "name": "Rune Scimitar",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "EARLY",
        "item_id": 1333,
        "recommended_combat": 30,
        "skill_reqs": [{"skill": "ATTACK", "level": 40}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 40}],
        "quest_req": "",
        "estimated_price": 15000,
        "ironman_source": "Drop from Fire Giants in Waterfall Dungeon or bought in Al Kharid Scimitar Shop",
        "wiki_url": "https://oldschool.runescape.wiki/w/Rune_scimitar",
        "highlight": "Premier early-game slash weapon for training Attack and Strength"
    },
    {
        "id": "dragon-scimitar",
        "name": "Dragon Scimitar",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 4587,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "quest_req": "Monkey Madness I",
        "estimated_price": 100000,
        "ironman_source": "Purchased from Daga's Scimitar Smithy on Ape Atoll for 100,000 coins",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_scimitar",
        "highlight": "Premier mid-game slash weapon with 4-tick speed"
    },
    {
        "id": "zombie-axe",
        "name": "Zombie Axe",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 28987,
        "recommended_combat": 65,
        "skill_reqs": [{"skill": "ATTACK", "level": 65}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 65}, {"skill": "SMITHING", "level": 70}],
        "quest_req": "Defender of Varrock",
        "estimated_price": 1400000,
        "ironman_source": "Broken zombie axe drop from Armoured Zombies; repaired with 70 Smithing",
        "wiki_url": "https://oldschool.runescape.wiki/w/Zombie_axe",
        "highlight": "Heavy 5-tick crush/slash axe with massive melee Strength bonus"
    },
    {
        "id": "dragon-dagger-p-plus-plus",
        "name": "Dragon Dagger (p++)",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 5698,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "quest_req": "Lost City",
        "estimated_price": 20000,
        "ironman_source": "Buy dragon dagger in Zanaris shop and poison with Weapon poison(++)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_dagger(p%2B%2B)",
        "highlight": "Iconic spec weapon: 25% special attack costs for 4 double-hit bursts"
    },
    {
        "id": "sarachnis-cudgel",
        "name": "Sarachnis Cudgel",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 23544,
        "recommended_combat": 70,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 60}],
        "quest_req": "",
        "estimated_price": 400000,
        "ironman_source": "Drop from Sarachnis in Forthos Dungeon",
        "wiki_url": "https://oldschool.runescape.wiki/w/Sarachnis_cudgel",
        "highlight": "Fast 4-tick crush weapon, excellent for Gargoyles and Calvar'ion"
    },
    {
        "id": "abyssal-whip",
        "name": "Abyssal Whip",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "LATE",
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
        "id": "osmumtens-fang",
        "name": "Osmumten's Fang",
        "slot": "WEAPON",
        "style": "MELEE",
        "tier": "ENDGAME",
        "item_id": 26219,
        "recommended_combat": 95,
        "skill_reqs": [{"skill": "ATTACK", "level": 82}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 82}],
        "quest_req": "Beneath Cursed Sands",
        "estimated_price": 15000000,
        "ironman_source": "Raid drop from Tombs of Amascut (Raid 3)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Osmumten%27s_fang",
        "highlight": "Rolls accuracy twice per attack; premier stab weapon for high-defence bosses"
    },
    # --- MELEE SHIELDS & DEFENDERS ---
    {
        "id": "rune-defender",
        "name": "Rune Defender",
        "slot": "SHIELD",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 8850,
        "recommended_combat": 60,
        "skill_reqs": [{"skill": "ATTACK", "level": 40}, {"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 65}, {"skill": "STRENGTH", "level": 65}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Warriors' Guild cyclopes drop (requires 130 combined Attack + Strength to enter)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Rune_defender",
        "highlight": "+5 Melee Strength and offensive accuracy in shield slot"
    },
    {
        "id": "dragon-defender",
        "name": "Dragon Defender",
        "slot": "SHIELD",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 12954,
        "recommended_combat": 65,
        "skill_reqs": [{"skill": "ATTACK", "level": 60}, {"skill": "DEFENCE", "level": 60}],
        "organic_skill_reqs": [{"skill": "ATTACK", "level": 65}, {"skill": "STRENGTH", "level": 65}, {"skill": "DEFENCE", "level": 60}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Warriors' Guild basement cyclopes drop after showing a Rune Defender",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_defender",
        "highlight": "+6 Melee Strength and high offensive stats in shield slot"
    },
    # --- MELEE ARMOUR & ACCESSORIES ---
    {
        "id": "helm-of-neitiznot",
        "name": "Helm of Neitiznot",
        "slot": "HEAD",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 10828,
        "recommended_combat": 55,
        "skill_reqs": [{"skill": "DEFENCE", "level": 44}],
        "organic_skill_reqs": [{"skill": "DEFENCE", "level": 44}],
        "quest_req": "The Fremennik Isles",
        "estimated_price": 50000,
        "ironman_source": "Quest reward from The Fremennik Isles",
        "wiki_url": "https://oldschool.runescape.wiki/w/Helm_of_neitiznot",
        "highlight": "+3 Melee Strength, +3 Prayer, with zero negative combat penalties"
    },
    {
        "id": "neitiznot-faceguard",
        "name": "Neitiznot Faceguard",
        "slot": "HEAD",
        "style": "MELEE",
        "tier": "LATE",
        "item_id": 24271,
        "recommended_combat": 85,
        "skill_reqs": [{"skill": "DEFENCE", "level": 70}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 60}, {"skill": "DEFENCE", "level": 70}],
        "quest_req": "The Fremennik Exiles",
        "estimated_price": 18000000,
        "ironman_source": "Basilisk jaw drop from Basilisk Knights (60 Slayer); combined with Helm of Neitiznot",
        "wiki_url": "https://oldschool.runescape.wiki/w/Neitiznot_faceguard",
        "highlight": "+6 Melee Strength non-degrading best-in-slot melee helmet"
    },
    {
        "id": "fighter-torso",
        "name": "Fighter Torso",
        "slot": "BODY",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 10551,
        "recommended_combat": 60,
        "skill_reqs": [{"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "DEFENCE", "level": 40}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Barbarian Assault minigame (375 honor points in all 4 roles + Penance Queen kill)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Fighter_torso",
        "highlight": "+4 Melee Strength bonus (same as Bandos Chestplate!) without spending millions"
    },
    {
        "id": "blood-moon-chestplate",
        "name": "Blood Moon Chestplate",
        "slot": "BODY",
        "style": "MELEE",
        "tier": "LATE",
        "item_id": 28929,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "DEFENCE", "level": 70}, {"skill": "ATTACK", "level": 65}],
        "organic_skill_reqs": [{"skill": "DEFENCE", "level": 70}, {"skill": "ATTACK", "level": 65}],
        "quest_req": "Perilous Moons",
        "estimated_price": 6000000,
        "ironman_source": "Drop from the Blood Moon boss in Moons of Peril dungeon",
        "wiki_url": "https://oldschool.runescape.wiki/w/Blood_moon_chestplate",
        "highlight": "+4 Melee Strength with high slash defence and set bonus healing"
    },
    {
        "id": "dragon-boots",
        "name": "Dragon Boots",
        "slot": "BOOTS",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 11840,
        "recommended_combat": 70,
        "skill_reqs": [{"skill": "DEFENCE", "level": 60}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 83}, {"skill": "DEFENCE", "level": 60}],
        "quest_req": "",
        "estimated_price": 140000,
        "ironman_source": "Drop from Spiritual Mages inside God Wars Dungeon (83 Slayer required)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Dragon_boots",
        "highlight": "+4 Melee Strength bonus in the footwear slot"
    },
    {
        "id": "barrows-gloves",
        "name": "Barrows Gloves",
        "slot": "HANDS",
        "style": "HYBRID",
        "tier": "MID",
        "item_id": 7462,
        "recommended_combat": 70,
        "skill_reqs": [{"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "COOKING", "level": 70}, {"skill": "MAGIC", "level": 59}, {"skill": "DEFENCE", "level": 40}],
        "quest_req": "Recipe for Disaster",
        "estimated_price": 130000,
        "ironman_source": "Culinaromancer's Chest for 130,000 coins after completing all RFD subquests",
        "wiki_url": "https://oldschool.runescape.wiki/w/Barrows_gloves",
        "highlight": "Supreme hybrid gloves (+12 Melee Strength, +12 ranged/magic accuracy, +12 defences)"
    },
    {
        "id": "fire-cape",
        "name": "Fire Cape",
        "slot": "CAPE",
        "style": "MELEE",
        "tier": "MID",
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
        "id": "amulet-of-glory",
        "name": "Amulet of Glory",
        "slot": "NECK",
        "style": "HYBRID",
        "tier": "EARLY",
        "item_id": 1704,
        "recommended_combat": 40,
        "skill_reqs": [],
        "organic_skill_reqs": [{"skill": "CRAFTING", "level": 80}, {"skill": "MAGIC", "level": 68}],
        "quest_req": "",
        "estimated_price": 12000,
        "ironman_source": "Craft dragonstone amulet (80 Crafting) and enchant with Lvl-5 Enchant (68 Magic) or Crystal chest",
        "wiki_url": "https://oldschool.runescape.wiki/w/Amulet_of_glory",
        "highlight": "+10 all offensive bonuses, +3 Melee Strength, +3 Prayer, and Edgeville/Draynor teleports"
    },
    {
        "id": "amulet-of-fury",
        "name": "Amulet of Fury",
        "slot": "NECK",
        "style": "HYBRID",
        "tier": "MID",
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
        "id": "berserker-ring",
        "name": "Berserker Ring",
        "slot": "RING",
        "style": "MELEE",
        "tier": "MID",
        "item_id": 6737,
        "recommended_combat": 70,
        "skill_reqs": [],
        "organic_skill_reqs": [],
        "quest_req": "The Fremennik Trials",
        "estimated_price": 3500000,
        "ironman_source": "Dagannoth Rex boss drop on Waterbirth Island (soloable with Iban Blast)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Berserker_ring",
        "highlight": "+4 Melee Strength (+8 when imbued at Nightmare Zone or Soul Wars!)"
    },

    # --- RANGED WEAPONS ---
    {
        "id": "magic-shortbow-i",
        "name": "Magic Shortbow (i)",
        "slot": "WEAPON",
        "style": "RANGED",
        "tier": "EARLY",
        "item_id": 12788,
        "recommended_combat": 40,
        "skill_reqs": [{"skill": "RANGED", "level": 50}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 50}],
        "quest_req": "",
        "estimated_price": 1000,
        "ironman_source": "Fletch magic shortbow (80 Fletching or hard clue) and imbue with LMS scroll",
        "wiki_url": "https://oldschool.runescape.wiki/w/Magic_shortbow_(i)",
        "highlight": "Fast 3-tick bow firing Rune/Amethyst arrows with 35% special attack cost"
    },
    {
        "id": "rune-crossbow",
        "name": "Rune Crossbow",
        "slot": "WEAPON",
        "style": "RANGED",
        "tier": "EARLY",
        "item_id": 9185,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "RANGED", "level": 61}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 61}],
        "quest_req": "",
        "estimated_price": 9000,
        "ironman_source": "Drop from Crazy Archaeologist in Ruins of Uzer, or Iron/Steel Dragons",
        "wiki_url": "https://oldschool.runescape.wiki/w/Rune_crossbow",
        "highlight": "Essential bossing crossbow; fires enchanted Ruby and Diamond bolts"
    },
    {
        "id": "hunters-sunlight-crossbow",
        "name": "Hunter's Sunlight Crossbow",
        "slot": "WEAPON",
        "style": "RANGED",
        "tier": "MID",
        "item_id": 28869,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "RANGED", "level": 50}],
        "organic_skill_reqs": [{"skill": "HUNTER", "level": 57}, {"skill": "FLETCHING", "level": 53}, {"skill": "RANGED", "level": 50}],
        "quest_req": "Children of the Sun",
        "estimated_price": 20000,
        "ironman_source": "Fletch from sunlight antelope horn (57 Hunter in Varlamore)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Hunters%27_sunlight_crossbow",
        "highlight": "Extremely fast 3-tick crossbow firing sunlight bolts with high DPS"
    },
    {
        "id": "toxic-blowpipe",
        "name": "Toxic Blowpipe",
        "slot": "WEAPON",
        "style": "RANGED",
        "tier": "LATE",
        "item_id": 12926,
        "recommended_combat": 80,
        "skill_reqs": [{"skill": "RANGED", "level": 75}, {"skill": "AGILITY", "level": 56}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 75}, {"skill": "MAGIC", "level": 75}, {"skill": "FLETCHING", "level": 53}],
        "quest_req": "Regicide",
        "estimated_price": 3200000,
        "ironman_source": "Zulrah boss drop (Tanzanite fang crafted with 53 Fletching)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Toxic_blowpipe",
        "highlight": "Extremely fast 2-tick rapid weapon with venom and healing special attack"
    },
    {
        "id": "bow-of-faerdhinen",
        "name": "Bow of Faerdhinen",
        "slot": "WEAPON",
        "style": "RANGED",
        "tier": "LATE",
        "item_id": 25865,
        "recommended_combat": 90,
        "skill_reqs": [{"skill": "RANGED", "level": 75}, {"skill": "AGILITY", "level": 70}],
        "organic_skill_reqs": [{"skill": "SMITHING", "level": 82}, {"skill": "CRAFTING", "level": 82}, {"skill": "RANGED", "level": 75}],
        "quest_req": "Song of the Elves",
        "estimated_price": 125000000,
        "ironman_source": "Enhanced Crystal Weapon Seed from The Corrupted Gauntlet in Prifddinas",
        "wiki_url": "https://oldschool.runescape.wiki/w/Bow_of_faerdhinen",
        "highlight": "Massive 10-tile range, supreme accuracy; pairs with Crystal Armour"
    },

    # --- RANGED ARMOUR & ACCESSORIES ---
    {
        "id": "black-dhide-body",
        "name": "Black D'hide Body",
        "slot": "BODY",
        "style": "RANGED",
        "tier": "MID",
        "item_id": 2503,
        "recommended_combat": 60,
        "skill_reqs": [{"skill": "RANGED", "level": 70}, {"skill": "DEFENCE", "level": 40}],
        "organic_skill_reqs": [{"skill": "CRAFTING", "level": 84}, {"skill": "RANGED", "level": 70}, {"skill": "DEFENCE", "level": 40}],
        "quest_req": "",
        "estimated_price": 8000,
        "ironman_source": "Craft with 84 Crafting from Black dragon leather, or drop from Deranged Archaeologist",
        "wiki_url": "https://oldschool.runescape.wiki/w/Black_d%27hide_body",
        "highlight": "+30 Ranged attack bonus with excellent magical and ranged defence"
    },
    {
        "id": "avas-accumulator",
        "name": "Ava's Accumulator",
        "slot": "CAPE",
        "style": "RANGED",
        "tier": "EARLY",
        "item_id": 10499,
        "recommended_combat": 35,
        "skill_reqs": [{"skill": "RANGED", "level": 50}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 50}],
        "quest_req": "Animal Magnetism",
        "estimated_price": 999,
        "ironman_source": "Purchased from Ava in Draynor Manor after Animal Magnetism",
        "wiki_url": "https://oldschool.runescape.wiki/w/Ava%27s_accumulator",
        "highlight": "Automatically retrieves 72% of fired arrows and bolts to your quiver"
    },
    {
        "id": "avas-assembler",
        "name": "Ava's Assembler",
        "slot": "CAPE",
        "style": "RANGED",
        "tier": "LATE",
        "item_id": 22109,
        "recommended_combat": 90,
        "skill_reqs": [{"skill": "RANGED", "level": 70}],
        "organic_skill_reqs": [{"skill": "RANGED", "level": 70}],
        "quest_req": "Dragon Slayer II",
        "estimated_price": 0,
        "ironman_source": "Combine Ava's Accumulator with Vorkath's Head (guaranteed on 50th Vorkath kill)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Ava%27s_assembler",
        "highlight": "Best-in-slot ranged cape: +2 Ranged Strength and retrieves 80% of ammo"
    },
    {
        "id": "necklace-of-anguish",
        "name": "Necklace of Anguish",
        "slot": "NECK",
        "style": "RANGED",
        "tier": "LATE",
        "item_id": 19547,
        "recommended_combat": 85,
        "skill_reqs": [{"skill": "HITPOINTS", "level": 75}],
        "organic_skill_reqs": [{"skill": "CRAFTING", "level": 98}, {"skill": "MAGIC", "level": 93}, {"skill": "SLAYER", "level": 69}],
        "quest_req": "Monkey Madness II",
        "estimated_price": 14000000,
        "ironman_source": "Zenyte shard from Demonic Gorillas; cut with 98 Crafting and enchant with 93 Magic",
        "wiki_url": "https://oldschool.runescape.wiki/w/Necklace_of_anguish",
        "highlight": "Best-in-slot ranged necklace: +5 Ranged Strength and +15 Ranged attack"
    },

    # --- MAGIC WEAPONS ---
    {
        "id": "ibans-staff-u",
        "name": "Iban's Staff (u)",
        "slot": "WEAPON",
        "style": "MAGIC",
        "tier": "EARLY",
        "item_id": 1409,
        "recommended_combat": 40,
        "skill_reqs": [{"skill": "MAGIC", "level": 50}, {"skill": "ATTACK", "level": 50}],
        "organic_skill_reqs": [{"skill": "MAGIC", "level": 50}],
        "quest_req": "Underground Pass",
        "estimated_price": 200000,
        "ironman_source": "Reward from Underground Pass; upgrade to (u) with 200k coins at Dark Mage in West Ardougne",
        "wiki_url": "https://oldschool.runescape.wiki/w/Iban%27s_staff_(u)",
        "highlight": "Iban Blast hits 25 base damage at only level 50 Magic! Premier early bossing weapon"
    },
    {
        "id": "ancient-staff",
        "name": "Ancient Staff",
        "slot": "WEAPON",
        "style": "MAGIC",
        "tier": "MID",
        "item_id": 4675,
        "recommended_combat": 60,
        "skill_reqs": [{"skill": "MAGIC", "level": 50}, {"skill": "ATTACK", "level": 50}],
        "organic_skill_reqs": [{"skill": "MAGIC", "level": 50}, {"skill": "ATTACK", "level": 50}],
        "quest_req": "Desert Treasure I",
        "estimated_price": 60000,
        "ironman_source": "Purchased from Eblis after Desert Treasure I, or drop from Chaos Druids in wilderness",
        "wiki_url": "https://oldschool.runescape.wiki/w/Ancient_staff",
        "highlight": "Autocasts Ancient Magicks (Ice Burst, Smoke Barrage) for Slayer bursting"
    },
    {
        "id": "warped-sceptre",
        "name": "Warped Sceptre",
        "slot": "WEAPON",
        "style": "MAGIC",
        "tier": "MID",
        "item_id": 28581,
        "recommended_combat": 65,
        "skill_reqs": [{"skill": "MAGIC", "level": 62}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 56}, {"skill": "MAGIC", "level": 62}],
        "quest_req": "The Path of Glouphrie",
        "estimated_price": 350000,
        "ironman_source": "Drop from Warped Creatures in Poison Waste Sewer (56 Slayer required)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Warped_sceptre",
        "highlight": "Accessible mid-game powered staff with built-in spell and 4-tick speed"
    },
    {
        "id": "trident-of-the-seas",
        "name": "Trident of the Seas",
        "slot": "WEAPON",
        "style": "MAGIC",
        "tier": "LATE",
        "item_id": 11907,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "MAGIC", "level": 75}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 87}, {"skill": "MAGIC", "level": 75}],
        "quest_req": "",
        "estimated_price": 750000,
        "ironman_source": "Cave Kraken slayer drop (requires 87 Slayer)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Trident_of_the_seas",
        "highlight": "Essential powered staff that attacks at 4-tick speed for bossing and raids"
    },
    {
        "id": "trident-of-the-swamp",
        "name": "Trident of the Swamp",
        "slot": "WEAPON",
        "style": "MAGIC",
        "tier": "LATE",
        "item_id": 12899,
        "recommended_combat": 80,
        "skill_reqs": [{"skill": "MAGIC", "level": 78}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 87}, {"skill": "MAGIC", "level": 78}],
        "quest_req": "",
        "estimated_price": 4500000,
        "ironman_source": "Combine Magic Fang from Zulrah with Trident of the Seas (requires 59 Crafting)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Trident_of_the_swamp",
        "highlight": "+3 max hits over Trident of the Seas, +15 magic accuracy, and inflicts venom"
    },

    # --- MAGIC ARMOUR & ACCESSORIES ---
    {
        "id": "god-cape-imbued",
        "name": "Imbued God Cape",
        "slot": "CAPE",
        "style": "MAGIC",
        "tier": "MID",
        "item_id": 21791,
        "recommended_combat": 75,
        "skill_reqs": [{"skill": "MAGIC", "level": 75}],
        "organic_skill_reqs": [{"skill": "MAGIC", "level": 75}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Complete Mage Arena II miniquest in the deep Wilderness",
        "wiki_url": "https://oldschool.runescape.wiki/w/Imbued_saradomin_cape",
        "highlight": "Best-in-slot magic cape: +2% Magic damage bonus and +15 Magic attack"
    },
    {
        "id": "occult-necklace",
        "name": "Occult Necklace",
        "slot": "NECK",
        "style": "MAGIC",
        "tier": "LATE",
        "item_id": 12002,
        "recommended_combat": 85,
        "skill_reqs": [{"skill": "MAGIC", "level": 70}],
        "organic_skill_reqs": [{"skill": "SLAYER", "level": 93}, {"skill": "MAGIC", "level": 70}],
        "quest_req": "",
        "estimated_price": 750000,
        "ironman_source": "Drop from Smoke Devils in Smoke Devil Dungeon (requires 93 Slayer)",
        "wiki_url": "https://oldschool.runescape.wiki/w/Occult_necklace",
        "highlight": "Premier magic damage amulet: +5% Magic damage and +12 Magic attack"
    },
    {
        "id": "tormented-bracelet",
        "name": "Tormented Bracelet",
        "slot": "HANDS",
        "style": "MAGIC",
        "tier": "LATE",
        "item_id": 19544,
        "recommended_combat": 85,
        "skill_reqs": [{"skill": "HITPOINTS", "level": 75}],
        "organic_skill_reqs": [{"skill": "CRAFTING", "level": 98}, {"skill": "MAGIC", "level": 93}, {"skill": "SLAYER", "level": 69}],
        "quest_req": "Monkey Madness II",
        "estimated_price": 12000000,
        "ironman_source": "Zenyte shard from Demonic Gorillas; cut with 98 Crafting and enchant with 93 Magic",
        "wiki_url": "https://oldschool.runescape.wiki/w/Tormented_bracelet",
        "highlight": "Best-in-slot magic bracelet: +5% Magic damage and +10 Magic attack"
    },

    # --- PRAYER & UTILITY GEAR ---
    {
        "id": "proselyte-armour",
        "name": "Proselyte Armour Set",
        "slot": "BODY",
        "style": "UTILITY",
        "tier": "EARLY",
        "item_id": 9674,
        "recommended_combat": 45,
        "skill_reqs": [{"skill": "DEFENCE", "level": 30}],
        "organic_skill_reqs": [{"skill": "DEFENCE", "level": 30}],
        "quest_req": "The Slug Menace",
        "estimated_price": 25000,
        "ironman_source": "Purchased from Sir Tiffy Cashien in Falador park after The Slug Menace",
        "wiki_url": "https://oldschool.runescape.wiki/w/Proselyte_armour",
        "highlight": "Highest prayer bonus per slot in the entire game (+31 Prayer bonus with helm/body/legs!)"
    },
    {
        "id": "rune-pouch",
        "name": "Rune Pouch",
        "slot": "UTILITY",
        "style": "UTILITY",
        "tier": "MID",
        "item_id": 12791,
        "recommended_combat": 40,
        "skill_reqs": [],
        "organic_skill_reqs": [],
        "quest_req": "",
        "estimated_price": 3800000,
        "ironman_source": "750 Slayer reward points from any Slayer Master, or 75 Last Man Standing (LMS) points",
        "wiki_url": "https://oldschool.runescape.wiki/w/Rune_pouch",
        "highlight": "Holds up to 16,000 of 3 different rune types in just 1 inventory space"
    },
    {
        "id": "herb-sack",
        "name": "Herb Sack",
        "slot": "UTILITY",
        "style": "UTILITY",
        "tier": "MID",
        "item_id": 13226,
        "recommended_combat": 50,
        "skill_reqs": [{"skill": "HERBLORE", "level": 58}],
        "organic_skill_reqs": [{"skill": "HERBLORE", "level": 58}],
        "quest_req": "",
        "estimated_price": 0,
        "ironman_source": "Purchased for 750 Slayer reward points from any Slayer master, or 250 Tithe Farm points",
        "wiki_url": "https://oldschool.runescape.wiki/w/Herb_sack",
        "highlight": "Stores up to 420 grimy herbs (30 of each type), invaluable for Slayer runs"
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
