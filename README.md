RealCiv (NeoForge 1.21.1)
==========================

RealCiv is a server-first civilization/economy progression mod.
Players do not pick a class for passive bonuses. They progress by contributing useful goods to a civilization hub, earning contribution karma and unlocking rights through participation.

Core Player Loop
----------------

1. Gather and fight within your current profession limits (farmer/miner/terraformer/lumberjack/fisher/hunter/warrior/explosives_expert/crafter).
2. Contribute goods to your civilization's Community Hub.
3. Gain profession XP, general XP, and contribution karma.
4. Unlock better tools and higher limits.
5. Rent/own legal land, build inside legal zones, and participate in civic governance.

How Players Participate
-----------------------

New citizen flow:

1. Check your status with `/realciv profile`.
2. Join a civilization with `/realciv civ join <name>` (or found one if approved).
3. Gather resources and contribute them at the Community Hub.
4. Use earned contribution karma for land and progression.

Important rule behaviors:

- If you hit an action limit, you must contribute relevant resources to hub to reset that profession action counter (except warrior, which levels from enemy player kills directly).
- Profession action restoration is item- and quantity-based (configured per profession in `config/realciv/hub/*_resets.txt` by default), not a full reset from any single cheap item.
- On death, action counters can be partially/fully refunded by `progression.deathActionRefundPercent` to prevent hard lockouts after item loss.
- Timed recovery can auto-reset stale action counters after inactivity via `progression.staleActionResetEnabled` and `progression.staleActionResetMinutes`.
- Tool tiers are level-gated (wood/stone/iron/diamond/netherite).
- Player-vs-player combat is diplomacy-gated: WAR allows cross-civ PvP, ALLY/NEUTRAL block it, and same-civ PvP is controlled by the civilization's friendly-fire toggle.
- Regulated explosives require a designated Explosives Expert role and respect per-level action caps.
- Regulated redstone placement can require a designated Redstoner role per civilization.
- Explosion block damage on claimed land is war/ownership-aware: non-war cross-civ grief is blocked.
- Wilderness (true public) land is never buildable.
- COMMUNITY land allows any member of that civilization to build and break.
- CIVIC land is leadership-managed (mayor/manager).
- PRIVATE land is owner-managed.
- If `land.blockUnclaimedBuilding=true`, even wilderness breaking is denied.
- First mayor Community Hub placement auto-claims starter CIVIC land around the hub.
- Only the owning civilization mayor (or admin) can move that civilization's Community Hub.
- Optional carry caps can block picking up/crafting above configured thresholds.

In-Game Civic Blocks
--------------------

- Community Hub (`realciv:community_hub`)
  - Right-click: deposit UI.
  - Sneak + right-click: stock/withdraw UI.
- Census Block (`realciv:census_block`)
  - Opens a census management UI (members, invites, join requests, manager actions).
- Tax Block (`realciv:tax_block`)
  - Private plot upkeep information and prepay flow.
- Land Wand (`realciv:land_wand`)
  - Left-click block: set selection `pos1` (chunk).
  - Right-click block: set selection `pos2` (chunk).
  - Right-click in air: tries FTB Chunks claim map first; falls back to the legacy RealCiv map if FTB map context is unavailable.
  - Sneak-right-click: draw chunk boundaries.

Command Guide
-------------

This section explains what each command is for, not just the syntax.

Civilization setup and membership:

- `/realciv civ list`: Shows all civilizations currently registered on the server.
- `/realciv civ info [player]`: Shows which civilization a player belongs to, and core civ details.
- `/realciv civ found <name>`: Player-led civ creation. Creates the civilization, assigns founder as mayor, and grants mayor starter kit (Hub/Census/Tax/Land Wand). Requires founder approval if enabled.
- `/realciv civ join <civ>`: Joins directly if invited; otherwise creates a join request for mayor/manager approval.
- `/realciv civ leave`: Leaves your current civilization and moves you to default unaligned civ.
- `/realciv civ create <name>`: Admin scaffolding command. Creates civ record only. Does not auto-assign mayor and does not move players.
- `/realciv civ rename <civ> <name>`: Admin rename of existing civilization display name.
- `/realciv civ delete <civ>`: Admin deletion of a civilization record.
- `/realciv civ assign <player> <civ>`: Admin force-assigns a player to a civilization.
- `/realciv civ diplomacy show [civ]`: Shows diplomacy state and same-civ friendly-fire status for a civilization.
- `/realciv civ diplomacy set <other-civ> <ally|neutral|war>`: Mayor/admin sets your civilization's diplomacy toward another civ (shared relation).
- `/realciv civ diplomacy set <civA> <civB> <ally|neutral|war>`: Admin override variant to set diplomacy between any two civilizations.
- `/realciv civ pvp show [civ]`: Shows whether intra-civ PvP (friendly fire) is enabled.
- `/realciv civ pvp friendlyfire <on|off>`: Mayor/admin toggles same-civilization PvP for sparring/friendly fights.
- `/realciv civ explosives show [civ]`: Shows designated explosives experts and civ cap usage.
- `/realciv civ explosives add <player>`: Mayor/admin designates a same-civ player as an explosives expert (subject to server cap).
- `/realciv civ explosives remove <player>`: Mayor/admin removes explosives expert designation.
- `/realciv civ redstoner show [civ]`: Shows designated redstoners and civ cap usage.
- `/realciv civ redstoner add <player>`: Mayor/admin designates a same-civ player as a redstoner (subject to server cap).
- `/realciv civ redstoner remove <player>`: Mayor/admin removes redstoner designation.

Progress and economy visibility:

- `/realciv profile [player]`: Shows level, profession XP state, action counters, and contribution karma for a player.
- `/realciv profession focus show [player]`: Shows the active specialization focus for yourself or (mayor/admin) another player.
- `/realciv profession focus set <profession>`: Sets your own specialization focus.
- `/realciv profession focus clear`: Clears your own specialization focus.
- `/realciv profession focus assign <player> <profession>`: Mayor/admin assigns specialization focus for a member in their civilization.
- `/realciv profession focus remove <player>`: Mayor/admin clears specialization focus for a member in their civilization.
- `/realciv hub open`: Opens the Community Hub stock/withdraw UI for your civilization.
- `/realciv hub stock [page]`: Chat listing of hub inventory for your civilization.
- `/realciv hub quota [page]`: Shows your personal withdrawal limits and remaining quota by item.
- `/realciv hub quota player <player> [page]`: Mayor/admin view of another player's quota.
- `/realciv hub withdraw <item> <count>`: Withdraws from hub against your personal quota.
- `/realciv hub withdraw <item> <count> <target>`: Mayor/admin withdraw to a target player (still quota-bound unless admin bypass).
- `/realciv hub logs [count]`: Mayor/admin audit log view for deposit/withdraw/governance actions.
- `/realciv hub coverage [page]`: Admin diagnostics for reward coverage and contribution mapping.
- `/realciv hub export-items <namespace>`: Admin export of all registered non-air item IDs for a mod namespace to `config/realciv/exports/<namespace>_items.txt` (one item id per line).

Town and private land claiming (chunk model):

- `/realciv town info`: Shows town claim counts, collective contribution karma, and current expansion costs.
- `/realciv town map [radius]`: FTB-style chunk map in chat for nearby area.
- `/realciv town claim`: Mayor/admin claims current chunk as CIVIC town land. First claim can be anywhere. Later claims must be adjacent to existing town CIVIC chunks.
- `/realciv town unclaim`: Mayor/admin unclaims current town chunk.
- `/realciv town allot <player> [days]`: Mayor/admin converts a town-owned chunk into a private plot for a citizen.
- `/realciv plot claim [days]`: Citizen claims current chunk as private land when it is within/adjacent to town CIVIC land and otherwise valid.
- `/realciv plot unclaim`: Owner, mayor, or admin removes private ownership from current chunk.
- `/realciv land info`: Shows zoning and permissions for current chunk, including whether you can build/break.
- `/realciv land rent [days]`: Compatibility command for private plot claiming/renewal using same core rules.

Land governance and staff controls:

- `/realciv land zone <community|civic|private> [owner] [days]`: Mayor/admin direct zoning override for current chunk (`public` still works as an alias).
- `/realciv land grant <player> [days]`: Mayor/admin shortcut to grant current chunk as private to a player.
- `/realciv land revoke`: Mayor/admin clears zoning on current chunk.
- `/realciv land manager add <player>`: Mayor/admin grants civic manager role.
- `/realciv land manager remove <player>`: Mayor/admin revokes civic manager role.
- `/realciv land wand [player]`: Gives land wand (or gives to target if admin).
- `/realciv land selection info`: Shows current wand selection bounds.
- `/realciv land selection clear`: Clears wand selection.
- `/realciv land zone-selection <community|civic|private> [owner] [days]`: Mayor/admin bulk zone selected chunk area (`public` still works as an alias).
- `/realciv land clear-selection`: Mayor/admin bulk clear selected zoning.
- `/realciv land visualize [radius]`: Visual boundary debug for nearby claimed chunks.
- `/realciv land ftb-mode [auto|civic|private]`: Mayor/admin sets personal FTB map claim mode. Non-mayors always claim PRIVATE plots.
- `/realciv land gui`: Opens FTB Chunks claim map with RealCiv rules (fallback to legacy RealCiv map if FTB map cannot open).

Mayor and census governance:

- `/realciv census members [page]`: Shows civilization member list.
- `/realciv census requests [page]`: Lists pending join requests.
- `/realciv census invites [page]`: Lists active invitations.
- `/realciv census invite <player>`: Sends a join invitation to a player.
- `/realciv census uninvite <player>`: Revokes an invitation.
- `/realciv census approve <player>`: Approves a request/invite and admits player.
- `/realciv census deny <player>`: Denies/clears request or invite.
- `/realciv census remove <player>`: Removes a member from the civilization.
- `/realciv census manager add <player>`: Mayor/admin promotes manager via census controls.
- `/realciv census manager remove <player>`: Mayor/admin removes manager.
- `/realciv census mayor <player>`: Mayor/admin sets current civ mayor.
- `/realciv census mayor clear`: Mayor/admin clears mayor assignment.
- `/realciv mayor show [civ]`: Shows current mayor for civ.
- `/realciv mayor set <player> [civ]`: Admin mayor assignment command.
- `/realciv mayor clear [civ]`: Admin mayor removal command.
- `/realciv mayor withdrawrate <player>`: Shows per-player withdrawal percent override.
- `/realciv mayor withdrawrate set <player> <percent>`: Mayor/admin sets player-specific withdrawal allowance.
- `/realciv mayor withdrawrate clear <player>`: Mayor/admin removes override and returns player to default withdraw percent.
- `/realciv mayor approval add <player>`: Admin approves player for `/realciv civ found`.
- `/realciv mayor approval remove <player>`: Admin revokes founder approval.
- `/realciv mayor approval list`: Admin list of approved founders.

Tax and upkeep:

- `/realciv tax status`: Shows your private plot count, upkeep due, and delinquency state.
- `/realciv tax pay [cycles]`: Prepays private plot upkeep cycles using your contribution karma.

Credits and economic moderation:

- `/realciv credit add <player> <amount>`: Admin adds contribution karma to player.
- `/realciv credit set <player> <amount>`: Admin sets exact contribution karma balance.

Common workflows:

1. Founding a new player-run civ: `mayor approval add` (admin) -> `civ found <name>` (player) -> `town claim` (mayor) -> citizens `plot claim`.
2. Expanding town legally: contribute to hub -> collective contribution karma grows -> mayor uses `town claim` on adjacent chunks.
3. Giving citizens build rights: mayor claims civic chunks, then uses `town allot <player>` for private plots.

Territory notifications:

- Players receive chat updates when crossing territory boundaries.
- CIVIC entry message example: `You've now entered <civilization name>'s territory.`
- PRIVATE entry message example: `You've now entered <player name>'s private plot in <civilization name>.`
- Wilderness message example: `You've entered true public wilderness.`

Configuration
-------------

File: `config/realciv-common.toml`

NeoForge profession-hook research reference: `docs/NEOFORGE_EVENT_PROFESSION_MATRIX.md`
Server-owner hook audit reference: `docs/NEOFORGE_SERVER_OWNER_HOOK_AUDIT.md`

Key areas:

- Profession limits:
  - `profession.farmerLimits`
  - `profession.minerLimits`
  - `profession.terraformerLimits`
  - `profession.lumberjackLimits`
  - `profession.fisherLimits`
  - `profession.hunterLimits`
  - `profession.warriorLimits`
  - `profession.explosivesExpertLimits`
  - `profession.crafterLimits`
  - `profession.eventHookRules`
  - `profession.breakActionCostOverrides`
- Level thresholds:
  - `progression.professionXpThresholds`
  - `progression.generalXpThresholds`
  - `progression.deathActionRefundPercent`
  - `progression.staleActionResetEnabled`
  - `progression.staleActionResetMinutes`
  - `specialization.singleProfessionLockEnabled`
  - `specialization.xpDecayEnabled`
  - `specialization.xpDecayRate`
  - `progression.warriorXpPerPlayerKill`
  - `progression.warriorGeneralXpPerPlayerKill`
  - `progression.warriorRequireHubRegistration`
  - `progression.explosivesExpertXpPerUse`
  - `progression.explosivesExpertGeneralXpPerUse`
  - `combat.warriorHomeDefenseNoActionCost`
- Hub rewards:
  - `hub.useProfessionRuleFiles`
  - `hub.professionRuleDirectory`
  - `hub.rewardRules`
  - `hub.tagRewardRules`
  - `hub.tagResetRules`
  - `hub.defaultPersonalWithdrawalPercent`
  - `economy.hubWithdrawCreditPenaltyPercent`
- Tool unlock gates:
  - `tools.requiredLevel.*`
- Land and upkeep:
  - `land.rentCost`
  - `land.rentCostAddedPerOwnedPrivate`
  - `land.rentDays`
  - `land.townClaimCost`
  - `land.townClaimCostAddedPerOwned`
  - `land.hubStarterAreaBlocks`
  - `land.upkeepCost`
  - `land.upkeepIntervalDays`
  - `land.upkeepGraceDays`
  - `land.blockUnclaimedBuilding`
  - `land.wandVisualizeRadiusChunks`
  - `land.wandMaxSelectionChunks`
  - `land.ftbMayorDefaultClaimMode` (`civic` or `private`)
- Civilization defaults:
  - `civ.defaultId`
  - `civ.defaultName`
  - `civ.maxExplosivesExpertsPerCivilization`
  - `civ.maxRedstonersPerCivilization`
  - `civ.requireFounderApproval`
- Redstone role controls:
  - `redstone.restrictedBlocks`
- Explosives controls:
  - `explosives.restrictedItems`
  - `explosives.blockNonPlayerDamageInClaims`
- Carry cap controls:
  - `carryCap.pickupEnabled`
  - `carryCap.craftEnabled`
  - `carryCap.professionMultipliers`
  - `carryCap.itemMaxOverrides`
- Admin/UI:
  - `admin.bypassRestrictions`
  - `admin.maxAuditLogs`
  - `ui.denyMessageCooldownTicks`
  - `ui.hubStockListLimit`

Per-profession hub files (default path):

- `config/realciv/hub/farmer_rewards.txt`
- `config/realciv/hub/miner_rewards.txt`
- `config/realciv/hub/terraformer_rewards.txt`
- `config/realciv/hub/lumberjack_rewards.txt`
- `config/realciv/hub/fisher_rewards.txt`
- `config/realciv/hub/hunter_rewards.txt`
- `config/realciv/hub/crafter_rewards.txt`
- `config/realciv/hub/farmer_resets.txt` (and equivalent for each profession)

If a file is missing, RealCiv generates a starter file from current legacy defaults.

Reward file line formats (profession implied by file):

- `minecraft:wheat|1.0|2|1` (item shorthand)
- `ITEM|minecraft:wheat|1.0|2|1`
- `ITEM_TAG|realciv:farmer_contributions|1.0|2|1`
- `BLOCK_TAG|minecraft:logs|1.0|2|1`

Reset file line formats (profession implied by file):

- `minecraft:wheat|1.0` (exact item shorthand, actions restored per item)
- `ITEM|minecraft:wheat|1.0`
- `ITEM_TAG|realciv:farmer_reset_items|1.0`
- `BLOCK_TAG|minecraft:logs|1.0`

Event hook rule format (in `profession.eventHookRules`):

- Legacy format: `hook|profession|actions_per_trigger|optional custom deny message`
- Extended format: `hook|profession|actions_per_trigger|key=value|key=value|...`
- Common option keys:
- `min_profession_level`
- `min_general_level`
- `min_membership_hours`
- `window_seconds` / `window_minutes` / `window_hours`
- `max_triggers`
- `profession_xp`
- `general_xp`
- `stat_prefix` (for `STAT_AWARD`)
- `deny_message`
- Hook ids:
- `ANIMAL_BREED`: uses `BabyEntitySpawnEvent` (player-caused breeding)
- `ANIMAL_TAME`: uses `AnimalTameEvent` (player taming)
- `SHEAR_ENTITY`: uses `PlayerInteractEvent.EntityInteractSpecific` + `IShearable` target checks
- `SHEAR_BLOCK`: uses shearing paths from `BlockToolModificationEvent` and `UseItemOnBlockEvent`
- `PLACE_SCAFFOLDING`: uses `BlockEvent.EntityPlaceEvent` when placing `minecraft:scaffolding`
- `BONEMEAL_USE`: uses `BonemealEvent` on valid bonemeal targets
- `TOOL_STRIP_LOG`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.AXE_STRIP`)
- `TOOL_TILL_SOIL`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.HOE_TILL`)
- `TOOL_FLATTEN_PATH`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.SHOVEL_FLATTEN`)
- `TOOL_DOUSE_CAMPFIRE`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.SHOVEL_DOUSE`)
- `TOOL_SCRAPE_COPPER`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.AXE_SCRAPE`)
- `TOOL_WAX_OFF`: uses `BlockEvent.BlockToolModificationEvent` (`ItemAbilities.AXE_WAX_OFF`)
- `FARMLAND_TRAMPLE`: uses `BlockEvent.FarmlandTrampleEvent` (player-caused trampling)
- `VILLAGER_INTERACT`: uses `PlayerInteractEvent.EntityInteract` before villager interaction opens
- `VILLAGER_TRADE`: uses `TradeWithVillagerEvent` (post-trade accounting)
- `ANVIL_USE`: uses `PlayerInteractEvent.RightClickBlock` on anvils
- `ANVIL_REPAIR`: uses `AnvilRepairEvent` (post-action accounting)
- `ITEM_SMELT`: uses `PlayerEvent.ItemSmeltedEvent` (post-action accounting)
- `ITEM_ENCHANT`: uses `PlayerEnchantItemEvent` (post-action accounting)
- `POTION_BREW`: uses `PlayerBrewedPotionEvent` (post-action accounting)
- `ITEM_TOSS`: uses `ItemTossEvent` (cancelable)
- `STAT_AWARD`: uses `StatAwardEvent` (cancelable; can be filtered by `stat_prefix`)
- Multiple rules can target the same hook. Costs are aggregated per profession and applied atomically.
- Optional level gates, membership-age gates, per-window quotas, and per-trigger XP grants are supported.
- `actions_per_trigger` can be `0` for pure quota/XP rules that do not consume action budgets.
- Custom deny message placeholders (optional):
- `%hook%`, `%profession%`, `%current%`, `%limit%`, `%cost%`
- `%required_profession_level%`, `%required_general_level%`, `%required_membership_hours%`
- `%window_used%`, `%window_limit%`, `%window_seconds%`, `%detail%`

Examples:

- `ANIMAL_BREED|FARMER|1|%profession% limit reached (%current%/%limit%).`
- `ANIMAL_TAME|HUNTER|1`
- `SHEAR_ENTITY|FARMER|1`
- `SHEAR_BLOCK|TERRAFORMER|2`
- `PLACE_SCAFFOLDING|TERRAFORMER|1|You must restock at the hub before placing more scaffolding.`
- `BONEMEAL_USE|FARMER|1`
- `TOOL_TILL_SOIL|FARMER|1`
- `TOOL_STRIP_LOG|LUMBERJACK|1`
- `VILLAGER_INTERACT|CRAFTER|1`
- `ANVIL_USE|CRAFTER|0|window_hours=24|max_triggers=1|profession_xp=25`
- `PLACE_SCAFFOLDING|TERRAFORMER|1|min_membership_hours=48`
- `STAT_AWARD|HUNTER|0|stat_prefix=stat.minecraft:talked_to_villager|general_xp=1`

Legacy fallback:

- Set `hub.useProfessionRuleFiles=false` to keep using legacy list configs:
- `hub.rewardRules` format: `item_id|profession|credits|profession_xp|general_xp`
- `hub.tagRewardRules` format: `selector_type|tag_id|profession|credits|profession_xp|general_xp`
- `hub.tagResetRules` format: `selector_type|tag_id|profession|actions_per_item`

Carry-cap multiplier format:

`profession|multiplier`

Example:

`HUNTER|1.0`

Carry-cap item override format:

`item_id|max_count`

Example:

`minecraft:beef|1`

Build and Test
--------------

- Compile: `gradlew classes`
- Full build: `gradlew build`
- Run dedicated server: `gradlew runServer`
- Run local client: `gradlew runClient`

FTB Chunks Integration Notes
----------------------------

- RealCiv now hooks into FTB Chunks claim/unclaim events and enforces RealCiv zoning, adjacency, collective contribution karma costs, and role permissions.
- Non-mayors always claim PRIVATE via FTB map; mayor/admin can use `auto`, `civic`, or `private` via `/realciv land ftb-mode`.
- `auto` uses `land.ftbMayorDefaultClaimMode` from `config/realciv-common.toml`.
- RealCiv land rules are authoritative: `CIVIC` and `PRIVATE` permissions are always checked from RealCiv data, not accepted blindly from FTB defaults.
- RealCiv mirrors civilization membership and claimed chunks into FTB Teams/Chunks, so the FTB map is used as a RealCiv view/control surface rather than a separate source of truth.
- `/realciv land gui` (and Land Wand right-click in air) tries the FTB map first, then opens the RealCiv fallback map if FTB team context is unavailable.

