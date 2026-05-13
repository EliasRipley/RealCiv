# RealCiv Profession Baseline (Server Owner Preset)

This baseline is designed for readability and easy edits in `config/realciv-common.toml`.

## Native Profession Baselines (Implemented)

Linear caps are enabled by default:

- `profession.useLinearLimitFormulas=true`

Defaults:

- Farmer: `4 + (4 * level)` via:
  - `profession.farmerLimitBase=4`
  - `profession.farmerLimitPerLevel=4`
- Miner: `40 + (10 * level)` via:
  - `profession.minerLimitBase=40`
  - `profession.minerLimitPerLevel=10`
- Terraformer: `40 + (10 * level)` via:
  - `profession.terraformerLimitBase=40`
  - `profession.terraformerLimitPerLevel=10`
- Lumberjack: `8 + (4 * level)` via:
  - `profession.lumberjackLimitBase=8`
  - `profession.lumberjackLimitPerLevel=4`
- Fisher: `4 + (4 * level)` via:
  - `profession.fisherLimitBase=4`
  - `profession.fisherLimitPerLevel=4`
- Hunter: `1 + (2 * level)` via:
  - `profession.hunterLimitBase=1`
  - `profession.hunterLimitPerLevel=2`
- Warrior: `1 + (2 * level)` via:
  - `profession.warriorLimitBase=1`
  - `profession.warriorLimitPerLevel=2`
- Explosives Expert: `1 + (1 * level)` via:
  - `profession.explosivesExpertLimitBase=1`
  - `profession.explosivesExpertLimitPerLevel=1`
- Crafter: `64 + (64 * level)` via:
  - `profession.crafterLimitBase=64`
  - `profession.crafterLimitPerLevel=64`
- Enchanter: `1 + (1 * level)` via:
  - `profession.enchanterLimitBase=1`
  - `profession.enchanterLimitPerLevel=1`
- Brewer: `1 + (1 * level)` via:
  - `profession.brewerLimitBase=1`
  - `profession.brewerLimitPerLevel=1`
- Trader: `1 + (1 * level)` via:
  - `profession.traderLimitBase=1`
  - `profession.traderLimitPerLevel=1`

## Default Event Hook Baselines

Default `profession.eventHookRules` now includes:

- `ITEM_ENCHANT|ENCHANTER|1`
- `POTION_BREW|BREWER|1`
- `VILLAGER_TRADE|TRADER|1`

## Tool Gates (Profession-Based)

Profession tool gating is enabled by default:

- `tools.professionLevelGatesEnabled=true`
- `tools.generalLevelGatesEnabled=false` (optional legacy-style global gate)

Default tier requirements are configured in `profession.toolTierRequirements`:

- Miner, Lumberjack, Terraformer, Farmer, Warrior:
  - Wood: `0`
  - Stone/Gold: `2`
  - Iron: `8`
  - Diamond: `25`
  - Netherite: `40`

## Optional Cap Systems (Admin-Controlled)

Use these only if your server wants tighter economic control:

- Per-profession daily action caps:
  - `profession.dailyActionCaps`
  - Format: `PROFESSION|cap0,cap1,cap2,...`
  - Example: `MINER|0,0,120,140,160`
- Miner per-block caps inside current action window:
  - `profession.minerBlockActionCaps`
  - Format: `block_id|cap0,cap1,...`
  - Example: `minecraft:iron_ore|2,3,4,5`
- Miner per-block daily caps:
  - `profession.minerDailyBlockActionCaps`
  - Format: `block_id|cap0,cap1,...`
  - Example: `minecraft:iron_ore|2,4,6,8`
- Hunter mob restrictions:
  - `profession.hunterMobMinLevels` (minimum level by mob)
  - `profession.hunterMobActionCaps` (max kills by mob, by level)
  - Examples:
    - `minecraft:warden|40`
    - `minecraft:ender_dragon|0,0,0,0,0,0,1`

## Requested Role Mapping

The following maps the requested profession ideas to current support:

- Miner:
  - Native support for base/per-level cap, tool tiers, daily caps, per-ore/per-block caps.
- Lumberjack:
  - Native support for base/per-level cap and tool tiers.
  - Stripped-log specific policy can be layered with hook rule `TOOL_STRIP_LOG`.
- Terraformer:
  - Native support for base/per-level cap, tool tiers, and daily caps.
- Farmer:
  - Native support for planting cap, tool tiers, and daily caps.
  - Harvest-specific policy can be layered via hub reset/tag reward rules and hooks.
- Hunter:
  - Native support for base/per-level cap, daily caps, per-mob caps, and per-mob minimum level.
  - No tool gate is required by default (matches sword ownership by Warrior design).
- Warrior:
  - Native support for base/per-level cap, sword tier gates, and daily caps.
  - Home-defense no-cost behavior is supported by:
    - `combat.warriorHomeDefenseNoActionCost=true`
- Explosives Expert:
  - Native support for base/per-level cap and daily caps.
- Crafter:
  - Native support for stack/item count caps and daily caps.
  - Item-family specialization can be layered via reward/reset/tag rules.
- Fisher:
  - Native support for base/per-level cap and daily caps.

## Template Roles (Not First-Class Yet)

These can be modeled today with `profession.eventHookRules`, but are not yet dedicated enum professions:

- Shepherd:
  - `SHEAR_ENTITY` / `SHEAR_BLOCK`
- Explorer:
  - `ITEM_TOSS` or `STAT_AWARD` templates for rocket behavior
  - Note: territory-aware "civilization zone no-count" for rockets needs dedicated event expansion
- Treasure Hunter:
  - Needs custom natural-loot-chest detection for strict vanilla-loot-only enforcement
- Breeder:
  - `ANIMAL_BREED`
- Smithy:
  - `ANVIL_USE`, `ANVIL_REPAIR`
- Smelter:
  - `ITEM_SMELT`

Use hook options for per-level and per-day style policy:

- `min_profession_level`
- `window_seconds`/`window_minutes`/`window_hours`
- `max_triggers`
- `profession_xp`
- `general_xp`
