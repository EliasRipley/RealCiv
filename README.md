# RealCiv (NeoForge 1.21.1)

RealCiv is a server-first civilization/economy progression mod. Players progress by contributing useful goods to a civilization hub, earning contribution karma and unlocking rights through participation — not by picking a class for passive bonuses.

## Description

### Core Player Loop

1. Gather and fight within your current profession limits (farmer, miner, terraformer, lumberjack, fisher, hunter, warrior, explosives expert, crafter, enchanter, brewer, trader).
2. Contribute goods to your civilization's Community Hub.
3. Gain profession XP and contribution karma (hub deposit general XP is optional by config).
4. Unlock better tools and higher limits.
5. Rent/own legal land, build inside legal zones, and participate in civic governance.

### Key Behaviors

- If you hit an action limit, you must contribute relevant resources to the hub to reset that profession action counter.
- Warrior kill XP is configurable: by default it is awarded directly from enemy player kills, but server owners can require hub registration via `progression.warriorRequireHubRegistration=true`.
- Profession action restoration is item- and quantity-based (configured per profession in `config/realciv/hub/*_resets.txt`), not a full reset from any single cheap item.
- On death, action counters can be partially/fully refunded by `progression.deathActionRefundPercent` to prevent hard lockouts after item loss.
- Timed recovery can auto-reset stale action counters after inactivity via `progression.staleActionResetEnabled` and `progression.staleActionResetMinutes`.
- Tool tiers can be profession-gated (default) and/or general-level-gated (optional).
- PvP is diplomacy-gated: WAR allows cross-civ PvP, ALLY/NEUTRAL block it, and same-civ PvP is controlled by the civilization's friendly-fire toggle.
- Cross-civ territory entry is allowed by default; diplomacy/config mainly govern interaction rights (build/break/PvP), not whether players can physically enter.
- Regulated explosives require a designated Explosives Expert role and respect per-level action caps.
- Regulated redstone placement can require a designated Redstoner role per civilization.
- Explosion block damage on claimed land is war/ownership-aware: non-war cross-civ grief is blocked.
- Wilderness (public) land is never buildable. COMMUNITY land allows any member to build and break. CIVIC land is leadership-managed. PRIVATE land is owner-managed.
- If `land.blockUnclaimedBuilding=true`, even wilderness breaking is denied.
- First mayor Community Hub placement auto-claims starter CIVIC land around the hub.
- Only the owning civilization mayor (or admin) can move that civilization's Community Hub.
- Optional carry caps can block picking up/crafting above configured thresholds.

## Player Instructions

### Getting Started

1. Check your status with `/realciv profile`.
2. Join a civilization with `/realciv civ join <name>` (or found one if approved).
3. Gather resources and contribute them at the Community Hub.
4. Use earned contribution karma for land and progression.

### In-Game Civic Blocks

- **Community Hub** (`realciv:community_hub`):
  - Right-click: deposit UI.
  - Sneak + right-click: stock/withdraw UI.
- **Census Block** (`realciv:census_block`): Right-click opens the Census dashboard (members, invites, join requests, manager actions).
- **Civic Control Console** (`realciv:civic_control_console`): Right-click opens the Civilization Control Panel.
- **Profession Ledger** (`realciv:profession_ledger`): Right-click opens the Profession Ledger dashboard.
- **War Table** (`realciv:war_table`): Right-click opens the Diplomacy Table dashboard. Leadership can draft war terms and handle incoming requests.
- **Tax Block** (`realciv:tax_block`): Right-click opens the Tax Office dashboard for upkeep and prepay.
- **Land Wand** (`realciv:land_wand`):
  - Left-click block: set selection pos1 (chunk).
  - Right-click block: set selection pos2 (chunk).
  - Right-click in air: tries FTB Chunks claim map; falls back to RealCiv map.
  - Sneak + right-click: draw chunk boundaries.

### Territory Notifications

- Players receive chat updates when crossing territory boundaries.
- CIVIC: *"You've now entered \<civilization name\>'s territory."*
- PRIVATE: *"You've now entered \<player name\>'s private plot in \<civilization name\>."*
- Wilderness: *"You've entered true public wilderness."*

### Common Workflows

1. **Founding a new player-run civ:** Admin uses `mayor approval add` → player uses `civ found <name>` → mayor uses `town claim` → citizens use `plot claim`.
2. **Expanding town legally:** Contribute to hub → collective contribution karma grows → mayor claims adjacent chunks with `town claim`.
3. **Giving citizens build rights:** Mayor claims civic chunks, then uses `town allot <player>` for private plots.

## Commands

### Civilization Setup & Membership

| Command | Description |
|---------|-------------|
| `/realciv civ list` | Lists all registered civilizations. |
| `/realciv civ info [player]` | Shows a player's civilization and core details. |
| `/realciv civ title show [civ]` | Shows the leadership title for your civ. |
| `/realciv civ title set <title>` | Sets your civilization's leadership title (e.g. King, Consul). |
| `/realciv civ title reset` | Resets title back to Mayor. |
| `/realciv civ governance show [civ]` | Shows governance model (autocratic/council/democratic). |
| `/realciv civ governance set <mode>` | Sets governance model metadata. |
| `/realciv civ role list [civ]` | Lists custom roles, permissions, and members. |
| `/realciv civ role create <roleId> [displayName]` | Creates a custom management role. |
| `/realciv civ role rename <roleId> <displayName>` | Renames a custom role. |
| `/realciv civ role delete <roleId>` | Deletes a custom role. |
| `/realciv civ role permission list <roleId>` | Shows permission keys for a role. |
| `/realciv civ role permission add <roleId> <permission>` | Grants a permission key to a role. |
| `/realciv civ role permission remove <roleId> <permission>` | Revokes a permission key from a role. |
| `/realciv civ role member list <roleId>` | Lists members assigned to a role. |
| `/realciv civ role member add <roleId> <player>` | Assigns a civ member to a role. |
| `/realciv civ role member remove <roleId> <player>` | Removes a civ member from a role. |
| `/realciv civ found <name>` | Player-led civ creation. Grants mayor starter kit. |
| `/realciv civ join <civ>` | Joins a civilization (or requests to join). |
| `/realciv civ leave` | Leaves your current civilization. |
| `/realciv civ create <name>` | Admin-only: creates civ record without assignments. |
| `/realciv civ rename <civ> <name>` | Admin-only: renames a civilization. |
| `/realciv civ delete <civ>` | Admin-only: deletes a civilization. |
| `/realciv civ assign <player> <civ>` | Admin-only: force-assigns a player to a civ. |

### Diplomacy & War

| Command | Description |
|---------|-------------|
| `/realciv civ diplomacy show [civ]` | Shows diplomacy state and friendly-fire status. |
| `/realciv civ diplomacy set <other> <state>` | Leadership/admin diplomacy action (ally/neutral/war). |
| `/realciv civ diplomacy accept <other>` | Accepts a pending diplomacy request. |
| `/realciv civ diplomacy reject <other>` | Rejects a pending diplomacy request. |
| `/realciv civ war show [civ]` | War-focused diplomacy view. |
| `/realciv civ war declare destruction <other> [submission] [land]` | Sends a destruction-war declaration. |
| `/realciv civ war declare pvp <other> <killTarget> [submission] [land]` | Sends a PvP war declaration. |
| `/realciv civ war accept <other>` | Accepts a pending WAR declaration. |
| `/realciv civ war reject <other>` | Rejects a pending WAR declaration. |
| `/realciv civ war resign <other>` | Resigns an active war (counts as loss). |
| `/realciv civ pvp show [civ]` | Shows intra-civ PvP (friendly fire) status. |
| `/realciv civ pvp friendlyfire <on/off>` | Toggles same-civ PvP. |

### Economy & Hub

| Command | Description |
|---------|-------------|
| `/realciv profile [player]` | Shows level, profession XP, action counters, and karma. |
| `/realciv profession focus show [player]` | Shows specialization focus. |
| `/realciv profession focus set <profession>` | Sets your specialization focus. |
| `/realciv profession focus clear` | Clears your specialization focus. |
| `/realciv profession focus assign <player> <profession>` | Leadership/admin assigns focus. |
| `/realciv profession focus remove <player>` | Leadership/admin clears focus. |
| `/realciv profession xp add <player> <profession> <amount>` | Admin adds profession XP. |
| `/realciv profession xp reduce <player> <profession> <amount>` | Admin reduces profession XP. |
| `/realciv profession xp set <player> <profession> <value>` | Admin sets profession XP exactly. |
| `/realciv profession level add <player> <profession> <amount>` | Admin increases profession level (maps to threshold XP). |
| `/realciv profession level reduce <player> <profession> <amount>` | Admin decreases profession level (maps to threshold XP). |
| `/realciv profession level set <player> <profession> <value>` | Admin sets profession level exactly (clamped to configured caps). |
| `/realciv hub open` | Opens Community Hub stock/withdraw UI. |
| `/realciv hub stock [page]` | Chat listing of hub inventory. |
| `/realciv hub quota [page]` | Shows your personal withdrawal limits. |
| `/realciv hub quota player <player> [page]` | Leadership/admin view of another player's quota. |
| `/realciv hub withdraw <item> <count> [target]` | Withdraws from hub against your quota. |
| `/realciv hub logs [count]` | Leadership/admin audit log view. |
| `/realciv hub coverage [page]` | Admin diagnostics for reward coverage. |
| `/realciv hub export-items <namespace>` | Admin export of item IDs to config file. |
| `/realciv credit add <player> <amount>` | Admin adds contribution karma. |
| `/realciv credit reduce <player> <amount>` | Admin reduces contribution karma. |
| `/realciv credit set <player> <amount>` | Admin sets exact karma balance. |
| `/realciv credit collective add <civ> <amount>` | Admin adds collective contribution karma to a civilization treasury. |
| `/realciv credit collective reduce <civ> <amount>` | Admin reduces collective contribution karma from a civilization treasury. |
| `/realciv credit collective set <civ> <amount>` | Admin sets civilization collective contribution karma exactly. |

### Land & Town

| Command | Description |
|---------|-------------|
| `/realciv town info` | Shows town claim counts and expansion costs. |
| `/realciv town map [radius]` | Chunk map in chat for nearby area. |
| `/realciv town claim` | Leadership claims current chunk as CIVIC. |
| `/realciv town unclaim` | Leadership unclaims current town chunk. |
| `/realciv town allot <player> [days]` | Converts a town chunk into a private plot. |
| `/realciv plot claim [days]` | Claims current chunk as private land. |
| `/realciv plot unclaim` | Removes private ownership from current chunk. |
| `/realciv land info` | Shows zoning and permissions for current chunk. |
| `/realciv land rent [days]` | Compatibility command for plot claiming. |
| `/realciv land zone <type> [owner] [days]` | Leadership/admin direct zoning override. |
| `/realciv land grant <player> [days]` | Grant current chunk as private. |
| `/realciv land revoke` | Clears zoning on current chunk. |
| `/realciv land manager add <player>` | Grants civic manager role. |
| `/realciv land manager remove <player>` | Revokes civic manager role. |
| `/realciv land wand [player]` | Gives a land wand. |
| `/realciv land selection info` | Shows current wand selection. |
| `/realciv land selection clear` | Clears wand selection. |
| `/realciv land zone-selection <type> [owner] [days]` | Bulk zone selected area. |
| `/realciv land clear-selection` | Bulk clear selected zoning. |
| `/realciv land visualize [radius]` | Visual boundary debug. |
| `/realciv land ftb-mode [mode]` | Sets personal FTB map claim mode. |
| `/realciv land gui` | Opens FTB Chunks claim map. |

### Census & Mayor

| Command | Description |
|---------|-------------|
| `/realciv census members [page]` | Shows member list. |
| `/realciv census requests [page]` | Lists pending join requests. |
| `/realciv census invites [page]` | Lists active invitations. |
| `/realciv census invite <player>` | Sends a join invitation. |
| `/realciv census uninvite <player>` | Revokes an invitation. |
| `/realciv census approve <player>` | Admits a player. |
| `/realciv census deny <player>` | Denies/clears request or invite. |
| `/realciv census remove <player>` | Removes a member from the civ. |
| `/realciv census manager add <player>` | Promotes a manager. |
| `/realciv census manager remove <player>` | Removes a manager. |
| `/realciv census mayor <player>` | Sets current civ mayor. |
| `/realciv census mayor clear` | Clears mayor assignment. |
| `/realciv mayor show [civ]` | Shows current mayor. |
| `/realciv mayor set <player> [civ]` | Admin-only mayor assignment. |
| `/realciv mayor clear [civ]` | Admin-only mayor removal. |
| `/realciv mayor withdrawrate <player>` | Shows player withdrawal percent override. |
| `/realciv mayor withdrawrate set <player> <percent>` | Sets player-specific withdrawal allowance. |
| `/realciv mayor withdrawrate clear <player>` | Removes withdrawal override. |
| `/realciv mayor approval add <player>` | Admin approves player for civ founding. |
| `/realciv mayor approval remove <player>` | Admin revokes founder approval. |
| `/realciv mayor approval list` | Admin lists approved founders. |

### Tax & Upkeep

| Command | Description |
|---------|-------------|
| `/realciv tax status` | Shows plot count, upkeep due, and delinquency. |
| `/realciv tax pay [cycles]` | Prepays private plot upkeep using karma. |

### Explosives & Redstone

| Command | Description |
|---------|-------------|
| `/realciv civ explosives show [civ]` | Shows designated explosives experts and cap usage. |
| `/realciv civ explosives add <player>` | Designates a player as explosives expert. |
| `/realciv civ explosives remove <player>` | Removes explosives expert designation. |
| `/realciv civ redstoner show [civ]` | Shows designated redstoners and cap usage. |
| `/realciv civ redstoner add <player>` | Designates a player as redstoner. |
| `/realciv civ redstoner remove <player>` | Removes redstoner designation. |

## Permissions

Custom governance permission keys (assign with `/realciv civ role permission add <roleId> <permission>`):

| Permission | Description |
|------------|-------------|
| `manage_governance` | Manage civ governance settings |
| `manage_diplomacy` | Handle diplomacy and war declarations |
| `manage_friendly_fire` | Toggle intra-civ PvP |
| `manage_profession_focus` | Assign/remove profession focus for members |
| `manage_explosives` | Manage explosives expert designations |
| `manage_redstoners` | Manage redstoner designations |
| `manage_town_claims` | Claim/unclaim CIVIC town chunks |
| `manage_land_zoning` | Zone/rezoning land and private unclaims |
| `manage_land_managers` | Add/remove civic managers |
| `manage_ftb_mode` | Choose claim mode on FTB map |
| `manage_census` | Manage membership, invites, and requests |
| `police_members` | Enforce civ rules on members |
| `manage_census_roles` | Manage census-related roles |
| `manage_leadership` | Manage leadership assignments |
| `manage_withdraw_rates` | Set withdrawal percentage overrides |
| `manage_hub_distribution` | Configure hub daily allowances |
| `manage_hub_withdrawals` | Manage hub withdrawals |
| `view_hub_quotas` | View player withdrawal quotas |
| `view_hub_logs` | View hub audit logs |
| `manage_upkeep` | Manage tax and upkeep settings |

## Server Owner Instructions

### Configuration

File: `config/realciv-common.toml`

#### Profession Limits

- `profession.useLinearLimitFormulas` — Linear or config-list-based action limits
- `profession.*LimitBase` / `profession.*LimitPerLevel` — Per-profession limits
- `profession.dailyActionCaps` — Daily caps per profession (opt-in)
- `profession.minerBlockActionCaps` / `profession.minerDailyBlockActionCaps` — Miner block-specific caps
- `profession.toolTierRequirements` — Tool tier gates by profession
- `profession.eventHookRules` — Hook-to-profession mapping rules
- `profession.breakActionCostOverrides` — Override action costs per block break

#### Level Thresholds

- `progression.professionXpThresholds` / `progression.generalXpThresholds` — XP level-up curves
- `progression.deathActionRefundPercent` — Action refund on death
- `progression.staleActionResetEnabled` / `progression.staleActionResetMinutes` — Timed recovery
- `specialization.singleProfessionLockEnabled` / `specialization.xpDecayEnabled` / `specialization.xpDecayRate` — Specialization system
- `progression.warriorXpPerPlayerKill` / `progression.warriorGeneralXpPerPlayerKill` / `progression.warriorRequireHubRegistration`
- `combat.warriorHomeDefenseNoActionCost`

#### Hub Rewards

- `hub.useProfessionRuleFiles` — File-based or legacy list-based rewards
- `hub.professionRuleDirectory` — Directory for per-profession rule files
- `hub.depositGeneralXpEnabled`
- `hub.rewardRules` / `hub.tagRewardRules` / `hub.tagResetRules`
- `hub.defaultPersonalWithdrawalPercent`
- `economy.hubWithdrawCreditPenaltyPercent`

#### Tool Unlock Gates

- `tools.professionLevelGatesEnabled` / `tools.generalLevelGatesEnabled`
- `tools.requiredLevel.*`

#### Land & Upkeep

- `land.rentCost` / `land.rentCostAddedPerOwnedPrivate` / `land.rentDays`
- `land.townClaimCost` / `land.townClaimCostAddedPerOwned` / `land.townClaimScalingMode` / `land.townClaimGrowthFactor` / `land.townClaimMaxCost`
- `land.hubStarterAreaBlocks`
- `land.upkeepCost` / `land.upkeepIntervalDays` / `land.upkeepGraceDays`
- `land.blockUnclaimedBuilding`
- `land.allowNeutralCivBuildBreak` / `land.allowAllyCivBuildBreak` / `land.allowWarCivBuildBreak`
- `land.wandVisualizeRadiusChunks` / `land.wandMaxSelectionChunks`
- `land.ftbMayorDefaultClaimMode`

#### Civilization Defaults

- `civ.defaultId` / `civ.defaultName`
- `civ.maxExplosivesExpertsPerCivilization` / `civ.maxRedstonersPerCivilization`
- `civ.requireFounderApproval`
- `civ.war.defaultPvpKillTarget`

#### Explosives, Redstone, Carry Caps

- `explosives.restrictedItems` / `explosives.blockNonPlayerDamageInClaims`
- `redstone.restrictedBlocks`
- `carryCap.pickupEnabled` / `carryCap.craftEnabled` / `carryCap.professionMultipliers` / `carryCap.itemMaxOverrides`

#### Admin & UI

- `admin.bypassRestrictions` / `admin.maxAuditLogs`
- `ui.denyMessageCooldownTicks` / `ui.hubStockListLimit`

### Baseline Profession Defaults

Action caps are linear by default (`profession.useLinearLimitFormulas=true`):

| Profession | Formula |
|------------|---------|
| Farmer | `4 + (4 * level)` |
| Miner | `40 + (10 * level)` |
| Terraformer | `40 + (10 * level)` |
| Lumberjack | `8 + (4 * level)` |
| Fisher | `4 + (4 * level)` |
| Hunter | `1 + (2 * level)` |
| Warrior | `1 + (2 * level)` |
| Explosives Expert | `1 + (1 * level)` |
| Crafter | `64 + (64 * level)` |
| Enchanter | `1 + (1 * level)` |
| Brewer | `1 + (1 * level)` |
| Trader | `1 + (1 * level)` |

Profession daily caps, miner block caps, and hunter mob-level gates are opt-in. Default event hooks include `ITEM_ENCHANT|ENCHANTER|1`, `POTION_BREW|BREWER|1`, and `VILLAGER_TRADE|TRADER|1`. Tool tier gates are enabled by default with wood=0, stone/gold=2, iron=8, diamond=25, netherite=40.

Template-ready professions (Shepherd, Explorer, Treasure Hunter, Breeder, Smithy, Smelter) can be modeled via `profession.eventHookRules` while core data-model expansion is staged.

### Hub Reward File Format

Per-profession files live in `config/realciv/hub/` (e.g. `farmer_rewards.txt`, `miner_rewards.txt`). If missing, RealCiv generates starter files.

**Reward file line format** (profession implied by file):
```
minecraft:wheat|1.0|2|1
ITEM|minecraft:wheat|1.0|2|1
ITEM_TAG|realciv:farmer_contributions|1.0|2|1
BLOCK_TAG|minecraft:logs|1.0|2|1
```

**Reset file line format:**
```
minecraft:wheat|1.0
ITEM|minecraft:wheat|1.0
ITEM_TAG|realciv:farmer_reset_items|1.0
BLOCK_TAG|minecraft:logs|1.0
```

### Event Hook Rule Format

Configured in `profession.eventHookRules`:

- Legacy: `hook|profession|actions_per_trigger|optional deny message`
- Extended: `hook|profession|actions_per_trigger|key=value|key=value|...`

Option keys: `min_profession_level`, `min_general_level`, `min_membership_hours`, `window_seconds`/`window_minutes`/`window_hours`, `max_triggers`, `profession_xp`, `general_xp`, `stat_prefix` (for STAT_AWARD), `deny_message`.

Full hook ID reference: see `docs/NEOFORGE_EVENT_PROFESSION_MATRIX.md`.

Examples:
```
ANIMAL_BREED|FARMER|1|%profession% limit reached (%current%/%limit%).
ANIMAL_TAME|HUNTER|1
SHEAR_ENTITY|FARMER|1
PLACE_SCAFFOLDING|TERRAFORMER|1|You must restock before placing more scaffolding.
TOOL_TILL_SOIL|FARMER|1
ANVIL_USE|CRAFTER|0|window_hours=24|max_triggers=1|profession_xp=25
PLACE_SCAFFOLDING|TERRAFORMER|1|min_membership_hours=48
STAT_AWARD|HUNTER|0|stat_prefix=stat.minecraft:talked_to_villager|general_xp=1
```

Legacy fallback: set `hub.useProfessionRuleFiles=false` to use legacy list configs (`hub.rewardRules`, `hub.tagRewardRules`, `hub.tagResetRules`).

### Carry-Cap Configuration

```
HUNTER|1.0                          (profession|multiplier)
minecraft:beef|1                    (item_id|max_count)
```

### FTB Chunks Integration

- RealCiv hooks into FTB Chunks claim/unclaim events and enforces RealCiv zoning, adjacency, costs, and role permissions.
- Non-leadership players always claim PRIVATE via the FTB map; leadership can use `auto`, `civic`, or `private` via `/realciv land ftb-mode`.
- Permission alignment: `manage_ftb_mode` controls claim mode choice, `manage_town_claims` for CIVIC actions, `manage_land_zoning` for private unclaims.
- `auto` mode uses `land.ftbMayorDefaultClaimMode` from config.
- RealCiv land rules and progression/combat policy remain authoritative over FTB defaults.
- Civ-backed FTB teams are normalized to allow PvP and explosions (so FTB team toggles don't block RealCiv war outcomes).
- Claim mirroring performs real FTB claims (not just validation), so mirrored claims appear correctly on the map.
- `/realciv land gui` (and Land Wand right-click in air) tries the FTB map first, then falls back to the RealCiv map.

Reference docs:
- `docs/NEOFORGE_EVENT_PROFESSION_MATRIX.md` — NeoForge hook-to-profession mapping
- `docs/NEOFORGE_SERVER_OWNER_HOOK_AUDIT.md` — Server-owner hook audit
- `docs/PROFESSION_BASELINE.md` — Profession baseline defaults

## Developer Instructions

### Build & Test

| Task | Command |
|------|---------|
| Compile | `gradlew classes` |
| Full build | `gradlew build` |
| Run dedicated server | `gradlew runServer` |
| Run local client | `gradlew runClient` |

### Architecture Reference

- `docs/COVERAGE_AUDIT.md` — Coverage/reward system technical reference
- `docs/BLOCKS_GUI_TASKLIST.md` — Active GUI implementation task list

## License

All Rights Reserved. Copyright (c) 2024-2026 Elias Ripley. See `LICENSE` for details.
