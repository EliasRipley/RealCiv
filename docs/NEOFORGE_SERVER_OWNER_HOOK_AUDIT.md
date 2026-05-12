# RealCiv NeoForge Server-Owner Hook Audit (MC 1.21.1 / NeoForge 21.1.228)

This is a practical audit of NeoForge gameplay hooks for turning RealCiv into a server-owner-driven civilization framework.

Audit method:
- Source of truth: local NeoForge sources JAR for `21.1.228`.
- Focus: hooks useful for role/profession policy, gating, quotas, and progression.
- Bias: player-attributed + server-authoritative + pre-action-cancellable hooks.

## 1) Best Hooks For Server-Owner Rules (Tier A)

These are the strongest hooks for configurable profession behavior.

1. `BlockEvent.BreakEvent`
- Why it matters: hard-stop mining/logging/terraform actions before break resolves.
- Typical policies: per-block caps, profession level gates, territory/role restrictions.

2. `BlockEvent.EntityPlaceEvent` (+ `EntityMultiPlaceEvent`)
- Why it matters: hard-stop placement before world mutation completes.
- Typical policies: wilderness placement quotas, protected infrastructure placement, role-only blocks.

3. `BlockEvent.BlockToolModificationEvent`
- Why it matters: catches strip/till/path/scrape/wax-off tool actions with item ability context.
- Typical policies: builder/farmer/lumberjack workflows without custom item patches.

4. `BlockEvent.FarmlandTrampleEvent`
- Why it matters: direct anti-grief or role-based trample logic.
- Typical policies: civilian vs outsider trample rights, penalties, cooldowns.

5. `PlayerInteractEvent.RightClickBlock`
- Why it matters: early gate for opening/using blocks (anvil, villagers via block interactions, etc.).
- Typical policies: workstation usage limits, role authorization.

6. `PlayerInteractEvent.RightClickItem`
- Why it matters: early gate for item-initiated actions.
- Typical policies: restricted items by role/level (totems, skulls, crystals, specialty tools).

7. `PlayerInteractEvent.EntityInteractSpecific` / `EntityInteract`
- Why it matters: pre-action entity interaction control.
- Typical policies: shearing, villager interaction access, mounted-entity workflows.

8. `UseItemOnBlockEvent`
- Why it matters: phase-aware right-click pipeline control with explicit cancellation result.
- Typical policies: prevent specific right-click actions while allowing others in same interaction chain.

9. `BonemealEvent`
- Why it matters: direct farm acceleration control.
- Typical policies: growth economy throttling and profession-bound fertilization.

10. `AttackEntityEvent`
- Why it matters: pre-hit entity combat control.
- Typical policies: boss attack gates, PvE/PvP profession restrictions.

11. `LivingDeathEvent`
- Why it matters: post-damage but cancellable death control.
- Typical policies: hard kill locks for progression milestones.

12. `BabyEntitySpawnEvent`
- Why it matters: explicit breeding gate with responsible player context.
- Typical policies: rancher profession quotas and breeding permits.

13. `AnimalTameEvent`
- Why it matters: pre-tame control.
- Typical policies: tamer/outlander progression and species ownership control.

14. `ItemFishedEvent`
- Why it matters: direct fish catch control.
- Typical policies: fisher quotas and biome-weighted fishing progression.

15. `ItemEntityPickupEvent.Pre`
- Why it matters: pickup deny/allow at server side.
- Typical policies: carry-cap, embargo items, contraband loops.

16. `ItemTossEvent`
- Why it matters: controlled discard behavior.
- Typical policies: anti-exploit loops around drop/pickup economies.

17. `PlayerXpEvent.PickupXp` / `.XpChange` / `.LevelChange`
- Why it matters: direct xp flow control.
- Typical policies: role XP budgets, profession-linked XP tax/decay systems.

18. `StatAwardEvent`
- Why it matters: high-signal action telemetry with cancellation support.
- Typical policies: stat-driven progression without per-block hardcoding.

## 2) Good Tracking Hooks (Tier B)

Useful for progression/accounting; weaker for prevention because many are post-action.

1. `TradeWithVillagerEvent`
- Server-side and player-attributed.
- Not cancellable; best for XP/accounting and sanctions.

2. `PlayerEvent.ItemCraftedEvent`
- Strong for crafter loops and output accounting.

3. `PlayerEvent.ItemSmeltedEvent`
- Strong for smith/smelter progression accounting.

4. `PlayerEnchantItemEvent`
- Strong for enchanter progression accounting.

5. `PlayerBrewedPotionEvent`
- Strong for alchemist progression accounting.

6. `AnvilRepairEvent`
- Fired when output is taken from anvil result slot.
- Best paired with pre-gating (`RightClickBlock`) for hard limits.

7. `LivingDropsEvent`
- Cancellable and useful for loot governance after kill resolution.

## 3) System/World Hooks (Tier C)

Powerful but less directly profession-attributed.

1. `CropGrowEvent.Pre/Post`
- Great for global economy pressure and region-based food policy.
- No direct player actor in event context.

2. `EntityJoinLevelEvent`
- Can block entity joins (e.g., boss spawn controls), but player attribution must be inferred.

3. `ExplosionEvent.Start/Detonate`
- Strong for explosives policy and claim safety filtering.

## 4) What This Means For RealCiv Server Owners

Current RealCiv now supports a broader subset (break/place/fish/hunt/explosives/crafting + configurable hooks including villager trade, anvil, smelt, enchant, brew, toss, and stat award).
To become a full framework, the next high-value expansions are:

1. Selector-based level gates
- `block/tag/entity/item -> required profession + min level`.
- Enables "no iron before Miner 5", "dragon at Hunter 100".

2. Time-window quotas
- `window (per day/per hour) + budget + refill model`.
- Enables "use anvil once per day", "wilderness placement per day".
Status: foundational support implemented in hook rules (`window_*` + `max_triggers`).

3. Player-age progression policies
- first-seen/server-membership-time-aware unlock rules.
- Enables "Outlander after 48h, +2/day every 24h".
Status: foundational support implemented in hook rules (`min_membership_hours`).

4. Dynamic profession definitions
- data-defined professions instead of enum-only profession list.
- Enables custom professions (Outlander, Smithy) without Java changes.

## 5) Example Mapping Against Common Server Requests

1. Miner tier + ore gate
- Hooks: `BreakEvent`, `PlayerEvent.HarvestCheck`, tool/interaction gates.

2. Wilderness Outlander budget
- Hooks: `EntityPlaceEvent` + first-seen timestamp + daily quota counters.

3. Smithy once/day
- Hooks: `RightClickBlock` (anvil pre-gate) + `AnvilRepairEvent` (accounting/XP).

4. Boss kill restrictions
- Hooks: `AttackEntityEvent` + `LivingDeathEvent` + optional summon-item gates.

## Source References

- NeoForge docs: Events (concepts): https://docs.neoforged.net/docs/concepts/events/
- NeoForge docs: Interaction pipeline: https://docs.neoforged.net/docs/1.21.1/items/interactionpipeline/
- NeoForge sources JAR (local): `neoforge-21.1.228-sources.jar`
- Event classes reviewed include `BlockEvent`, `PlayerInteractEvent`, `UseItemOnBlockEvent`, `BonemealEvent`, `AttackEntityEvent`, `LivingDeathEvent`, `LivingDropsEvent`, `BabyEntitySpawnEvent`, `AnimalTameEvent`, `TradeWithVillagerEvent`, `PlayerEvent`, `PlayerXpEvent`, `PlayerEnchantItemEvent`, `AnvilRepairEvent`, `PlayerBrewedPotionEvent`, `StatAwardEvent`, `ItemEntityPickupEvent`, `ItemFishedEvent`, `ItemTossEvent`, `ExplosionEvent`, `EntityJoinLevelEvent`, `CropGrowEvent`.
