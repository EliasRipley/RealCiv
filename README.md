RealCiv (NeoForge 1.21.1)
==========================

RealCiv is a server-first civilization/economy progression mod.
Players do not pick a class for passive bonuses. They progress by contributing useful goods to a civilization hub, earning social credit and unlocking rights through participation.

Core Player Loop
----------------

1. Gather within your current profession limits (farmer/miner/terraformer/lumberjack/hunter/crafter).
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
- Public/wilderness land is break-only.
- Building is only allowed in CIVIC land (mayor/manager permission) or PRIVATE land (owner).
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
  - Sneak-right-click (or command visualize): draw chunk boundaries.

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

Progress and economy visibility:

- `/realciv profile [player]`: Shows level, profession XP state, action counters, and social credit for a player.
- `/realciv hub open`: Opens the Community Hub stock/withdraw UI for your civilization.
- `/realciv hub stock [page]`: Chat listing of hub inventory for your civilization.
- `/realciv hub quota [page]`: Shows your personal withdrawal limits and remaining quota by item.
- `/realciv hub quota player <player> [page]`: Mayor/admin view of another player's quota.
- `/realciv hub withdraw <item> <count>`: Withdraws from hub against your personal quota.
- `/realciv hub withdraw <item> <count> <target>`: Mayor/admin withdraw to a target player (still quota-bound unless admin bypass).
- `/realciv hub logs [count]`: Mayor/admin audit log view for deposit/withdraw/governance actions.
- `/realciv hub coverage [page]`: Admin diagnostics for reward coverage and contribution mapping.

Town and private land claiming (chunk model):

- `/realciv town info`: Shows town claim counts, treasury, and current expansion costs.
- `/realciv town map [radius]`: FTB-style chunk map in chat for nearby area.
- `/realciv town claim`: Mayor/admin claims current chunk as CIVIC town land. First claim can be anywhere. Later claims must be adjacent to existing town CIVIC chunks.
- `/realciv town unclaim`: Mayor/admin unclaims current town chunk.
- `/realciv town allot <player> [days]`: Mayor/admin converts a town-owned chunk into a private plot for a citizen.
- `/realciv plot claim [days]`: Citizen claims current chunk as private land when it is within/adjacent to town CIVIC land and otherwise valid.
- `/realciv plot unclaim`: Owner, mayor, or admin removes private ownership from current chunk.
- `/realciv land info`: Shows zoning and permissions for current chunk, including whether you can build/break.
- `/realciv land rent [days]`: Compatibility command for private plot claiming/renewal using same core rules.

Land governance and staff controls:

- `/realciv land zone <public|civic|private> [owner] [days]`: Mayor/admin direct zoning override for current chunk.
- `/realciv land grant <player> [days]`: Mayor/admin shortcut to grant current chunk as private to a player.
- `/realciv land revoke`: Mayor/admin clears zoning on current chunk.
- `/realciv land manager add <player>`: Mayor/admin grants civic manager role.
- `/realciv land manager remove <player>`: Mayor/admin revokes civic manager role.
- `/realciv land wand [player]`: Gives land wand (or gives to target if admin).
- `/realciv land selection info`: Shows current wand selection bounds.
- `/realciv land selection clear`: Clears wand selection.
- `/realciv land zone-selection <public|civic|private> [owner] [days]`: Mayor/admin bulk zone selected chunk area.
- `/realciv land clear-selection`: Mayor/admin bulk clear selected zoning.
- `/realciv land visualize [radius]`: Visual boundary debug for nearby claimed chunks.

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
- `/realciv tax pay [cycles]`: Prepays private plot upkeep cycles using your social credit.

Credits and economic moderation:

- `/realciv credit add <player> <amount>`: Admin adds social credit to player.
- `/realciv credit set <player> <amount>`: Admin sets exact social credit balance.

Common workflows:

1. Founding a new player-run civ: `mayor approval add` (admin) -> `civ found <name>` (player) -> `town claim` (mayor) -> citizens `plot claim`.
2. Expanding town legally: contribute to hub -> treasury grows -> mayor uses `town claim` on adjacent chunks.
3. Giving citizens build rights: mayor claims civic chunks, then uses `town allot <player>` for private plots.

Territory notifications:

- Players receive chat updates when crossing territory boundaries.
- CIVIC entry message example: `You've now entered <civilization name>'s territory.`
- PRIVATE entry message example: `You've now entered <player name>'s private plot in <civilization name>.`
- Wilderness message example: `You've entered public wilderness.`

Configuration
-------------

File: `config/realciv-common.toml`

Key areas:

- Profession limits:
  - `profession.farmerLimits`
  - `profession.minerLimits`
  - `profession.terraformerLimits`
  - `profession.lumberjackLimits`
  - `profession.hunterLimits`
  - `profession.crafterLimits`
  - `profession.breakActionCostOverrides`
- Level thresholds:
  - `progression.professionXpThresholds`
  - `progression.generalXpThresholds`
- Hub rewards:
  - `hub.rewardRules`
  - `hub.tagRewardRules`
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

