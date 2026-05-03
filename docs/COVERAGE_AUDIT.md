# RealCiv Coverage Audit (NeoForge 1.21.1)

This mod now uses a research-backed coverage model based on the actual Minecraft 1.21.1 runtime data files (registries, tags, and vanilla loot tables), not guessed hardcoded lists.

## Rule Layers

1. `hub.rewardRules` (exact item rules)  
2. `hub.tagRewardRules` (tag rules, ordered, first match wins)

`hub.tagRewardRules` supports:
- `ITEM_TAG|<tag_id>|<profession>|<credits>|<profession_xp>|<general_xp>`
- `BLOCK_TAG|<tag_id>|<profession>|<credits>|<profession_xp>|<general_xp>`

## Default Profession Taxonomy

- `realciv:farmer_contributions`
- `realciv:miner_contributions`
- `realciv:lumberjack_contributions`
- `realciv:hunter_contributions`
- `realciv:crafter_contributions`

These are in:
- `data/realciv/tags/item/*.json`

## Vanilla Tag Integration

Defaults also include vanilla tags for broad block coverage:
- `minecraft:mineable/pickaxe` -> `MINER`
- `minecraft:logs` -> `LUMBERJACK`
- `minecraft:bamboo_blocks` -> `LUMBERJACK`

## In-Game Coverage Audit

Use:

`/realciv hub coverage [page]`

This prints:
- exact rule count
- tag rule count
- covered vs total item count
- miner/lumberjack block coverage counts
- paged unmatched item IDs

Use this to tune the config before production testing.
