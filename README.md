RealCiv (NeoForge 1.21.1 MVP)
=============================

RealCiv is a server-first civilization economy mod for NeoForge 1.21.1.
Players progress by contributing goods to a Community Hub instead of selecting RPG classes.

Implemented MVP systems
-----------------------

- Community Hub deposit block (`realciv:community_hub`) with 6-row deposit UI.
- Farmer, miner, lumberjack, hunter, and crafter action limits with progression scaling.
- Contribution rewards: profession XP, general XP, social credit.
- Tool usage gating by general level (wood/stone/iron/diamond/netherite).
- Plot-based chunk renting with social credit cost and rental expiry.
- Personal quota hub withdrawals with mayor/admin override controls.
- Server audit logging for deposits, withdrawals, mayor actions, and land rentals.
- Admin/gameplay commands for profile, land, credits, mayor, and hub management.

Core command reference
----------------------

- `/realciv profile [player]`
- `/realciv land rent`
- `/realciv land info`
- `/realciv hub stock`
- `/realciv hub logs [count]` (mayor/admin)
- `/realciv hub withdraw <item> <count>` (all players, subject to personal quota)
- `/realciv hub withdraw <item> <count> <target>` (mayor/admin override)
- `/realciv credit add <player> <amount>` (admin)
- `/realciv credit set <player> <amount>` (admin)
- `/realciv mayor show`
- `/realciv mayor set <player>` (admin)
- `/realciv mayor clear` (admin)
- `/realciv mayor withdrawrate <player>` (mayor/admin)
- `/realciv mayor withdrawrate set <player> <percent>` (mayor/admin)
- `/realciv mayor withdrawrate clear <player>` (mayor/admin)

Configuration
-------------

File: `config/realciv-common.toml`

Key sections:

- `profession.farmerLimits`
- `profession.minerLimits`
- `profession.hunterLimits`
- `profession.crafterLimits`
- `profession.lumberjackLimits`
- `progression.professionXpThresholds`
- `progression.generalXpThresholds`
- `hub.rewardRules`
- `hub.defaultPersonalWithdrawalPercent`
- `tools.requiredLevel.*`
- `land.rentCost`
- `land.rentDays`
- `admin.maxAuditLogs`

Reward rule format:

`item_id|profession|credits|profession_xp|general_xp`

Example:

`minecraft:wheat|FARMER|1.0|2|1`

Build and run
-------------

- `gradlew classes` to compile
- `gradlew runServer` for dedicated server testing
- `gradlew runClient` for local integrated testing
