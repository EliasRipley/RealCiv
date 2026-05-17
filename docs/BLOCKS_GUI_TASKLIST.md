# Blocks GUI Task List

Date: 2026-05-17
Scope: Census block, Civic Control Console, and shared RealCiv block GUI behavior.

## Goals
- Make screen exit behavior intuitive: one `Esc` press closes the GUI.
- Keep players on the same sub-screen/tab after actions.
- Replace hidden click gestures with explicit, visible controls.
- Add a structured, modular table-style layout across block menus.
- Add a leadership-facing ration editor flow for `rationed` resource policy.

## Phase 1: Navigation and Screen Lifecycle (P0)

### 1.1 Reuse existing open screens instead of opening a new one every action
- Files:
  - `src/main/java/com/realciv/realciv/network/RealCivNetwork.java`
- Problem:
  - Open payload handlers always do `new ...Screen(...).openGui()`.
  - In FTB `BaseScreen`, each new screen stores the current one as `prevScreen`, causing multi-`Esc` close chains.
  - This also resets local UI state (tabs/pages/toggles) because a fresh screen instance is created.
- Tasks:
  - Add `openOrRefresh...` handlers per screen type.
  - If current screen is same type, call `.refresh(snapshot)` on it.
  - Only call `.openGui()` when not already on that screen type.
- Acceptance:
  - Clicking any action in Census/Control Panel/Tax/Diplomacy/Hub Stock does not increase required `Esc` presses.
  - Control Panel tab remains where player left it (for example, `Governance` or `Roles`).

### 1.2 Add explicit close button in top-right header
- Files:
  - `src/main/java/com/realciv/realciv/client/RealCivScreen.java`
- Tasks:
  - Add a fixed `X`/`Close` button in header area.
  - Hook to existing screen close path (`closeGui(true)`).
- Acceptance:
  - Players can close block GUI with either `Esc` or header close button.

## Phase 2: Census Interaction Safety and Clarity (P0)

### 2.1 Replace left/right-click member actions with explicit buttons
- Files:
  - `src/main/java/com/realciv/realciv/client/ModernCensusScreen.java`
  - `src/main/java/com/realciv/realciv/network/RealCivNetwork.java`
- Problem:
  - Hidden gestures (`Left: kick`, `Right: toggle manager`) are non-discoverable and unsafe.
- Tasks:
  - Convert member rows to table rows with explicit action cells:
    - `Kick` button (right side after member details).
    - `Manager` toggle button (`Promote`/`Demote` or `Manager: On/Off`).
  - Remove destructive action from simple row-click.
  - Add a confirmation dialog for `Kick`.
- Acceptance:
  - Single left-click on row never kicks.
  - Manager toggle is visible and works with left-click.
  - Kick requires explicit button action and confirmation.

### 2.2 Permission feedback for blocked actions
- Files:
  - `src/main/java/com/realciv/realciv/network/RealCivNetwork.java`
- Tasks:
  - Where actions currently `return` silently on permission failure, send an explicit deny message.
- Acceptance:
  - User always gets feedback when action was denied by role/permission.

## Phase 3: Civic Ration Policy UX (P1)

### 3.1 Add leadership ration editor entrypoint in Civic UI
- Files:
  - `src/main/java/com/realciv/realciv/client/ModernCivControlPanelScreen.java`
- Tasks:
  - In `Economy` tab, when resource policy is `RATIONED` and player has distribution permission:
    - Show `Edit Rations` button.
  - Button opens dedicated ration editor screen.
- Acceptance:
  - Leadership can discover and open ration configuration directly from Civic UI.

### 3.2 Implement ration editor (ghost-item/table model, no item consumption)
- Files (new and existing):
  - `src/main/java/com/realciv/realciv/client/*` (new ration screen)
  - `src/main/java/com/realciv/realciv/network/RealCivPayloads.java`
  - `src/main/java/com/realciv/realciv/network/RealCivNetwork.java`
  - `src/main/java/com/realciv/realciv/data/CivSavedData.java` (reuse existing allowance methods)
- Tasks:
  - Build a table-like editor with rows: `Item`, `Daily Amount`, `Set/Clear`.
  - Use item picker/ghost slot semantics (store item id and amount only).
  - Wire to existing backend methods:
    - `setHubDailyAllowanceLimit(...)`
    - `clearAllHubDailyAllowanceLimits(...)`
  - Add packets for set/clear actions from GUI.
- Acceptance:
  - Editing rations does not consume/store actual inventory items.
  - Entries persist and affect withdrawal behavior in `RATIONED` mode.

## Phase 4: Role Creation UX (P1)

### 4.1 Make Create Role visibly succeed/fail in GUI flow
- Files:
  - `src/main/java/com/realciv/realciv/network/RealCivNetwork.java`
  - `src/main/java/com/realciv/realciv/client/ModernCivControlPanelScreen.java`
- Problem:
  - Current role create action can appear to "do nothing" when permission denied or tab resets.
- Tasks:
  - Keep current tab via Phase 1 screen refresh fix.
  - Add explicit deny messages when lacking `manage_governance`.
  - Add visible success cue in Roles tab (`Custom roles` count refresh and temporary status row/message).
- Acceptance:
  - Clicking `Create Role` always results in clear feedback (created or denied).

## Phase 5: Shared Table/Module Refactor (P2)

### 5.1 Extract reusable table components for block screens
- Files:
  - `src/main/java/com/realciv/realciv/client/RealCivScreen.java`
  - `src/main/java/com/realciv/realciv/client/*Screen.java`
- Tasks:
  - Create reusable helpers for:
    - header row rendering
    - fixed-width columns
    - inline action button cells
    - row hover state
  - Migrate Census first, then Hub Stock and Diplomacy.
- Acceptance:
  - Table-like layout logic is shared and consistent.
  - Future block screens can add rows/actions with minimal duplication.

## Suggested Execution Order
1. Phase 1 (navigation lifecycle)
2. Phase 2 (census safety)
3. Phase 4 (role creation UX)
4. Phase 3 (ration editor)
5. Phase 5 (modular refactor)

## Quick Win Checklist (first implementation batch)
- [ ] Phase 1.1 done
- [ ] Phase 1.2 done
- [ ] Phase 2.1 done
- [ ] Phase 2.2 done
- [ ] Phase 4.1 done

