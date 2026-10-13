# KimchiBaguette

## Team Introduction
Our team's goal is to diversify the player and enemy ship systems in Space Invaders. We aim to implement selectable player ships with unique stats, varied enemy movement and attack patterns, and a bonus system (such as extra lives and shields) as well as a penalty system (such as obstacles).

## Members
| Name | Role | GitHub |
| --- | --- | --- |
| Julie Znaor | Team leader & Enemy Ship&Pattern Developer | [Julie Znaor](https://github.com/JulieZnaor) |
| Felicie Pene | Weapon Developer & projectile developer 1| [Felicie Pene](https://github.com/feliciepene) |
| Lena Matejic | player ship Developer | [Lena Matejic](https://github.com/Lenamtj) |
| BRAMI Édouard | Enemy Movement Developer | [BRAMI Édouard](https://github.com/EdouardBrami) |
| Yongwon Kim | Collaborator & Player Status&Buff/Debuff 1 | [Yongwon Kim](https://github.com/YitWub/YitWub) |
| Chaemin Oh | Boss System Developer 1| [Chaemin Oh](https://github.com/Littlecocoa/Littlecocoa) |
| Jiye Kwon | Player Status&Buff/Debuff 2| [Jiye Kwon](https://github.com/kwon-jiye/kwon-jiye.git) |
| Yoon-ah Ji | Boss System Developer 2 | [Yoon-ah Ji](https://github.com/jiyoonah/jiyoonah) |
| Junah noh | Weapon Developer & projectile developer 2| [Junah noh](https://github.com/noh1216) |


## Team Requirements
We are developing the **Player & Enemy Ship Variety** system of the Space Invaders game. Our team is responsible for giving players a choice of ships with distinct stats, giving enemies varied movement and attack patterns (including bosses), and adding bonus and penalty elements that change the moment-to-moment gameplay.

## Detailed Requirements
The five requirements below are the scope our team commits to for the initial
implementation. The figures are the values fixed in our code, and each requirement
states the condition under which we treat it as complete. This project has no
automated test suite, so every completion condition is written so that it can be
checked by building and running `engine.Core` by hand.

**R1. Selectable player ships with distinct stats.**
Three ships are selectable before the game starts: Standard (speed 2, 750 ms between
shots, 3 lives), Speedster (speed 4, 400 ms, 2 lives) and Heavy Tank (speed 1,
1000 ms, 5 lives, shield-capable).
*Complete when* the selection screen offers all three, the chosen ship carries its own
speed, fire interval and life count into the game screen, and only the shield-capable
ship can hold a shield.

**R2. Enemy ships using several attack methods.**
Three projectile types — Normal (one shot), Rapid (two shots 100 ms apart) and Multi
(three shots in one volley) — and a shooting cooldown held per enemy ship instead of
per formation, so that enemies fire independently of each other.
*Complete when* each projectile type is observable in play, and two enemies in the same
formation can fire at different times.

**R3. Enemy ships with distinct movement trajectories.**
At least two movement patterns in addition to the baseline formation sweep, with
irregular tempo and direction rather than a fixed step.
*Complete when* two formations of the same size visibly follow different paths, and
neither of them repeats a constant step interval.

**R4. Bosses with difficulty levels and varied projectile attacks.**
Two boss types (1000 HP slow, 800 HP fast), each with three phases driven by remaining
HP and three attack patterns (single shot, multi shot, spread shot), plus one special
attack reserved for the final phase. Bosses appear on stages 3, 5 and 7.
*Complete when* a boss stage cannot be cleared while its boss is alive, the attack
pattern changes as the boss loses HP, and a defeated boss stops moving and attacking
and does not respawn.

**R5. Bonus and penalty elements.**
Bonus elements: a 3-second shield that absorbs one hit, and an extra life. Penalty
element: an obstacle that blocks player movement or fire.
*Complete when* each element can be obtained or encountered in play, the shield expires
on its own after 3 seconds, and the penalty element has an effect the player can
observe without reading the log.

## Dependencies on Other Teams
The items below are the support our own requirements depend on, and that we therefore
request from the teams named.

* **Team 3 (Level Design System)**: We ask the Level Design team to let each level's
  configuration declare which enemy ship types appear in a wave and which stages spawn
  a boss, so that enemy types and bosses are placed by level data instead of being
  hardcoded in the game screen. Without this, R2, R3 and R4 cannot be arranged into a
  difficulty curve across the levels, and the stages 3, 5 and 7 named in R4 stay fixed
  in our own code.
* **Team 8 (Gameplay HUD)**: We ask the Gameplay HUD team for HUD elements that show
  per-ship state: a remaining health and shield gauge, the icon of the selected ship,
  and a special ability gauge. Without this, the stat differences in R1 and the shield
  duration in R5 are not observable on screen, so neither completion condition can be
  checked in play.
* **Team 5 (Visual Effect System)**: We ask the Visual Effect team for per-ship and
  per-boss visual effects: engine thrust particles bound to the selected ship, and
  boss-specific hit and explosion animations. Without this, the three ships in R1 and
  the two boss types in R4 are visually indistinguishable, and the phase changes in R4
  have no visible feedback.

## Git Workflow
The team's Git workflow — branch strategy, commit rules, pull request and review
rules, merge strategy and the overall process diagram — is documented separately
in [`docs/KimchiBaguette_Git_Workflow.md`](../docs/KimchiBaguette_Git_Workflow.md).
