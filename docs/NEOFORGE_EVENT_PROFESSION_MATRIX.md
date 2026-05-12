# RealCiv NeoForge Event/Profession Matrix (MC 1.21.1 / NeoForge 21.1.228)

This document maps NeoForge gameplay events to profession-friendly enforcement points so server owners can design custom loops from config instead of Java patches.

## Scope

- Version scope: RealCiv pinned to NeoForge `21.1.228`.
- Focus scope: server-authoritative gameplay hooks (not rendering/client UI events).
- Design goal: hook IDs that are stable, readable, and composable via `profession.eventHookRules`.

## Core Loop Coverage (Existing Hardcoded Gates)

These are already part of RealCiv's default profession loop:

- `BlockEvent.BreakEvent` + `BlockDropsEvent`: miner/terraformer/lumberjack action budget.
- `BlockEvent.EntityPlaceEvent`: crop placement limits + civic placement policy.
- `ItemFishedEvent`: fisher action budget.
- `AttackEntityEvent` + `LivingDeathEvent`: hunter/warrior gates and diplomacy coupling.
- `ExplosionEvent.Start` + `ExplosionEvent.Detonate`: explosives role + claim damage policy.
- `PlayerEvent.ItemCraftedEvent`: crafter output budget.
- `ItemEntityPickupEvent.Pre`: carry-cap enforcement.

## Config-Driven Hook Surface (Now Implemented)

Configured in `profession.eventHookRules` with:

- Legacy: `hook|profession|actions_per_trigger|optional custom deny message`
- Extended: `hook|profession|actions_per_trigger|key=value|key=value|...`
- Common options:
- `min_profession_level`
- `min_general_level`
- `min_membership_hours`
- `window_seconds` / `window_minutes` / `window_hours`
- `max_triggers`
- `profession_xp`
- `general_xp`
- `stat_prefix` (for `STAT_AWARD` filtering)
- `deny_message`

Implemented hook IDs:

- `ANIMAL_BREED` -> `BabyEntitySpawnEvent` (cancelable)
- `ANIMAL_TAME` -> `AnimalTameEvent` (cancelable)
- `SHEAR_ENTITY` -> `PlayerInteractEvent.EntityInteractSpecific` + `IShearable` checks (cancelable)
- `SHEAR_BLOCK` -> shearing paths from `BlockToolModificationEvent` and `UseItemOnBlockEvent` (cancelable)
- `PLACE_SCAFFOLDING` -> `BlockEvent.EntityPlaceEvent` when placing scaffolding (cancelable)
- `BONEMEAL_USE` -> `BonemealEvent` on valid targets (cancelable)
- `TOOL_STRIP_LOG` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.AXE_STRIP` (cancelable)
- `TOOL_TILL_SOIL` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.HOE_TILL` (cancelable)
- `TOOL_FLATTEN_PATH` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.SHOVEL_FLATTEN` (cancelable)
- `TOOL_DOUSE_CAMPFIRE` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.SHOVEL_DOUSE` (cancelable)
- `TOOL_SCRAPE_COPPER` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.AXE_SCRAPE` (cancelable)
- `TOOL_WAX_OFF` -> `BlockEvent.BlockToolModificationEvent` + `ItemAbilities.AXE_WAX_OFF` (cancelable)
- `FARMLAND_TRAMPLE` -> `BlockEvent.FarmlandTrampleEvent` (cancelable)
- `VILLAGER_INTERACT` -> `PlayerInteractEvent.EntityInteract` on `AbstractVillager` target (cancelable)
- `VILLAGER_TRADE` -> `TradeWithVillagerEvent` (non-cancelable; accounting-focused)
- `ANVIL_USE` -> `PlayerInteractEvent.RightClickBlock` on anvils (cancelable)
- `ANVIL_REPAIR` -> `AnvilRepairEvent` (non-cancelable; accounting-focused)
- `ITEM_SMELT` -> `PlayerEvent.ItemSmeltedEvent` (non-cancelable; accounting-focused)
- `ITEM_ENCHANT` -> `PlayerEnchantItemEvent` (non-cancelable; accounting-focused)
- `POTION_BREW` -> `PlayerBrewedPotionEvent` (non-cancelable; accounting-focused)
- `ITEM_TOSS` -> `ItemTossEvent` (cancelable)
- `STAT_AWARD` -> `StatAwardEvent` (cancelable, optionally filter by `stat_prefix`)

### Hook Engine Behavior

- Multiple rules can target the same hook.
- Costs are aggregated per profession and validated before any mutation.
- Application is atomic: if one profession fails limit/focus checks, no hook charges are applied.
- Optional level/membership gates can deny before action-cost mutation.
- Optional per-window trigger quotas are tracked per rule (for daily/hourly style limits).
- Optional hook XP grants can award profession/general XP per trigger.
- `actions_per_trigger` can be `0` for pure quota/XP rules that do not consume hub-reset action budgets.
- Placeholder support in custom deny text:
- `%hook%`, `%profession%`, `%current%`, `%limit%`, `%cost%`
- `%required_profession_level%`, `%required_general_level%`, `%required_membership_hours%`
- `%window_used%`, `%window_limit%`, `%window_seconds%`, `%detail%`

## Why These Hooks Were Prioritized

These events are strong candidates for server-owner profession policy because they are:

- player-attributed,
- server-authoritative,
- and cancelable before vanilla completes the interaction.

That combination preserves your core gameplay loop (actions -> hub contribution -> action recovery) without rollback hacks.

## High-Value Next Hooks (Research Backlog)

These were reviewed and are valuable, but need policy decisions before rollout:

- `CropGrowEvent.Pre/Post` (no direct player actor): useful for economy/environment controls.
- `BlockEvent.FluidPlaceBlockEvent` (no guaranteed player actor): useful for infra/terraform policy.
- `VanillaGameEvent` (broad telemetry bus): useful for advanced auditing rules.

## Server-Owner Configuration Patterns

Examples:

- `TOOL_TILL_SOIL|FARMER|1`
- `TOOL_STRIP_LOG|LUMBERJACK|1`
- `VILLAGER_INTERACT|CRAFTER|1`
- `ANIMAL_TAME|HUNTER|1|%profession% cap reached (%current%/%limit%).`
- `PLACE_SCAFFOLDING|TERRAFORMER|1|Restock hub materials before placing more scaffolding.`
- `ANVIL_USE|CRAFTER|0|window_hours=24|max_triggers=1|profession_xp=25`
- `PLACE_SCAFFOLDING|TERRAFORMER|1|min_membership_hours=48`
- `STAT_AWARD|HUNTER|0|stat_prefix=stat.minecraft:interact_with_furnace|general_xp=1`

## Source References Used

- NeoForge concepts (events): https://docs.neoforged.net/docs/1.21.4/concepts/events/
- NeoForge concepts (logical sides): https://docs.neoforged.net/docs/1.21.4/concepts/sides/
- NeoForge interaction pipeline: https://docs.neoforged.net/docs/1.20.6/items/interactionpipeline/
- NeoForge `21.1.228` sources (local Gradle cache), especially:
- `BabyEntitySpawnEvent`
- `AnimalTameEvent`
- `PlayerInteractEvent`
- `UseItemOnBlockEvent`
- `BonemealEvent`
- `BlockEvent` (including `BlockToolModificationEvent` and `FarmlandTrampleEvent`)
- `ItemAbilities`
