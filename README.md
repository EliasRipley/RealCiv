RealCiv (NeoForge 1.21.1)
==========================

RealCiv is a server-first civilization/economy progression mod.
Players do not pick a class for passive bonuses. They progress by contributing useful goods to a civilization hub, earning social credit and unlocking rights through participation.

Core Player Loop
----------------

1. Gather within your current profession limits (farmer/miner/lumberjack/hunter/crafter).
2. Contribute goods to your civilization's Community Hub.
3. Gain profession XP, general XP, and social credit.
4. Unlock better tools and higher limits.
5. Rent/own legal land, build inside legal zones, and participate in civic governance.

How Players Participate
-----------------------

New citizen flow:

1. Check your status with `/realciv profile`.
2. Join a civilization with `/realciv civ join <name>` (or found one if approved).
3. Gather resources and contribute them at the Community Hub.
4. Use earned social credit for land and progression.

Important rule behaviors:

- If you hit an action limit, you must contribute relevant resources to hub to reset that profession action counter.
- Tool tiers are level-gated (wood/stone/iron/diamond/netherite).
- Building/breaking outside legal land is blocked.
- Optional carry caps can block picking up/crafting above configured thresholds.

In-Game Civic Blocks
--------------------

- Community Hub (`realciv:community_hub`)
  - Right-click: deposit UI.
  - Sneak + right-click: stock/withdraw UI.
- Census Block (`realciv:census_block`)
  - Civic roster/role management entrypoint for mayor/admin workflows.
- Tax Block (`realciv:tax_block`)
  - Private plot upkeep information and prepay flow.
- Land Wand (`realciv:land_wand`)
  - Left-click block: set selection `pos1` (chunk).
  - Right-click block: set selection `pos2` (chunk).
  - Sneak-right-click (or command visualize): draw chunk boundaries.

Command Guide
-------------

Player commands:

- `/realciv profile [player]`
- `/realciv civ info [player]`
- `/realciv civ list`
- `/realciv civ found <name>` (requires founder approval unless disabled)
- `/realciv civ join <civ>`
- `/realciv civ leave`
- `/realciv land rent [days]`
- `/realciv land info`
- `/realciv hub open`
- `/realciv hub stock [page]`
- `/realciv hub quota [page]`
- `/realciv hub withdraw <item> <count>`
- `/realciv hub withdraw <item> <count> <target>` (mayor/admin override)
- `/realciv tax status`
- `/realciv tax pay [cycles]`
- `/realciv census members [page]`

Mayor/admin governance commands:

- `/realciv land zone <public|civic|private> [owner] [days]`
- `/realciv land grant <player> [days]`
- `/realciv land revoke`
- `/realciv land manager add <player>`
- `/realciv land manager remove <player>`
- `/realciv land wand [player]`
- `/realciv land selection info`
- `/realciv land selection clear`
- `/realciv land zone-selection <public|civic|private> [owner] [days]`
- `/realciv land clear-selection`
- `/realciv land visualize [radius]`
- `/realciv hub quota player <player> [page]`
- `/realciv hub logs [count]`
- `/realciv mayor show [civ]`
- `/realciv mayor withdrawrate <player>`
- `/realciv mayor withdrawrate set <player> <percent>`
- `/realciv mayor withdrawrate clear <player>`
- `/realciv census manager add <player>`
- `/realciv census manager remove <player>`
- `/realciv census mayor <player>`
- `/realciv census mayor clear`

Admin commands:

- `/realciv civ create <name>`
- `/realciv civ rename <civ> <name>`
- `/realciv civ delete <civ>`
- `/realciv civ assign <player> <civ>`
- `/realciv mayor set <player> [civ]`
- `/realciv mayor clear [civ]`
- `/realciv mayor approval add <player>`
- `/realciv mayor approval remove <player>`
- `/realciv mayor approval list`
- `/realciv credit add <player> <amount>`
- `/realciv credit set <player> <amount>`
- `/realciv hub coverage [page]`

Notes:

- `civ found` creates the civ and sets founder as mayor.
- `civ create` is admin scaffolding and does not auto-assign a mayor.
- Mayors receive a starter civic kit (Hub/Census/Tax/Land Wand).

Configuration
-------------

File: `config/realciv-common.toml`

Key areas:

- Profession limits:
  - `profession.farmerLimits`
  - `profession.minerLimits`
  - `profession.lumberjackLimits`
  - `profession.hunterLimits`
  - `profession.crafterLimits`
- Level thresholds:
  - `progression.professionXpThresholds`
  - `progression.generalXpThresholds`
- Hub rewards:
  - `hub.rewardRules`
  - `hub.tagRewardRules`
  - `hub.defaultPersonalWithdrawalPercent`
- Tool unlock gates:
  - `tools.requiredLevel.*`
- Land and upkeep:
  - `land.rentCost`
  - `land.rentDays`
  - `land.upkeepCost`
  - `land.upkeepIntervalDays`
  - `land.upkeepGraceDays`
  - `land.blockUnclaimedBuilding`
  - `land.wandVisualizeRadiusChunks`
  - `land.wandMaxSelectionChunks`
- Civilization defaults:
  - `civ.defaultId`
  - `civ.defaultName`
  - `civ.requireFounderApproval`
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

Reward rule format:

`item_id|profession|credits|profession_xp|general_xp`

Example:

`minecraft:wheat|FARMER|1.0|2|1`

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
