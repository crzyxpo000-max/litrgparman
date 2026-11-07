# Ascension Protocol — LitRPG Game Design Document

Nov 5, 2026 · @arman

## Vision and design pillars

Ascension Protocol is a single-player, fully text-based LitRPG for Android in which a mysterious System lands on Earth and rewrites humanity with levels, classes, and skills. Every choice shapes both the player's build and the fate of the world; no two playthroughs end the same way.

*This is the North American localization of the design. Story, choices, and mechanics are identical to the Turkish version; names, places, units, humor, and cultural references are adapted for a US/Canadian audience.*

**Mechanical DNA borrowed from the genre.** The story and cast are entirely original; only genre conventions are borrowed.

| Inspiration | Mechanic borrowed | How it shows up here |
| --- | --- | --- |
| Defiance of the Fall | Planet-wide integration event, "first-to-do-it" rewards, conceptual power path | Integration Day, "First" titles, the Essence system |
| He Who Fights With Monsters | A System interface with personality, detailed skill text, identity and morality dilemmas | Snarky System notifications, morality-axis choices |
| The Primal Hunter | Hidden classes and skills unlocked by behavior, grade evolution, professions separate from combat class | Hidden affinity tracking, F→S grades, separate Profession tree |
| Solo Leveling | A weak hero who levels up alone, gates and dungeons, daily quest with a penalty, a summoned army | Gate dungeons, Daily Quest, Summons/companion system |

**Design pillars**

1. **Choices have to matter.** Every major choice flips at least one persistent world flag; consequences sometimes land 10–30 in-game days later.
2. **Your build grows out of how you play.** You never pick a class from a menu. The System watches how you fight and what you choose, then makes you an offer.
3. **Numbers must be visible and satisfying.** Level-ups, skill evolutions, and new titles are the genre's real payoff; each gets its own on-screen moment.
4. **The world turns without you.** NPCs and factions pursue their own goals. If you don't save a town, that town can fall.
5. **Hand-written skeleton, dynamic details.** Main story beats are authored; the event engine, the Story Director, and seeds fill in everything between them; all text comes from pre-written content.

**Target:** 15–25 hours per playthrough, 8 endings + 1 secret ending, at least 3 distinct build paths, full English and Turkish support, Android phones and tablets.

## OP MC design

Ascension Protocol is an "OP MC" power fantasy: the hero starts weak but grows outside the rules everyone else plays by. This section overrides every other section; where they conflict, this one wins. Mechanics come from the genre; no text, names, classes, or skills are copied from the inspiring works.

### Core rule: the MC is an anomaly

- **Everyone gets the standard System. The MC gets a corrupted, privileged build.** On Integration Day, Echo, a broken fragment of the System, bonds to the player. This is canon in the prologue and the opening sequence: the player's window glitches mid-transfer, and one line shows up only for them.
- **Only the MC has the Daily Quest.** Nobody else knows it exists; telling an NPC about it is a [Reveal] action.
- **Hidden information:** the MC sees exact enemy stats, true levels, hidden quest rewards, and hidden System lines. High stats add private **passive insight** lines to scenes ("[PER] His level is fake. Real: 34.").
- **Guarantee:** every run, the MC earns one **Unique skill** and one **hidden class**; which ones depends on play style (the Affinity vector, morality, and flags). The third option of the Level 10 offer is always a hidden class. If no hidden class's condition is met, the hidden class closest to the strongest Affinity arrives as "Echo's suggestion."

### Power curve: every Grade is one loop

Each Grade (F→S) runs through four beats, and the content follows them in order:

1. **Struggle (short):** the world just got stronger; fights are real and tactical.
2. **Breakthrough:** an authored **awakening moment** triggered by conditions (e.g., winning under 10% HP, protecting someone at a steep cost). It unlocks a hidden skill or evolution path, built on a reusable **awakening trigger** mechanic.
3. **Domination (long, most of the playtime):** the MC clearly outclasses everything around them.
4. **New ceiling:** something from the next tier resets the scale.

The MC's stat growth is **exponential** (Daily Quest streaks, the Unique skill, title bonuses, class growth that scales with Grade); NPCs grow on a slower curve. Big numbers are part of the fun: damage numbers, level-up chains, and a **humanity percentile** on the Status screen ("Top 0.001%").

### New systems

- **Global Ranking:** a live leaderboard of humanity's strongest (the MC, Ash Wolfe, Marcus Stone, procedural hunters). NPCs climb it off-screen, and the System announces the changes. Guilds, the Ascended, and rivals react to the MC's rank.
- **Hidden vs. Revealed Power:** the MC has a **true level** and a **public level**. [Conceal] options keep Attention low and set up "they underestimated me" payoffs; [Reveal] options earn fear, respect, and recruitment offers but raise Attention. NPCs react to the public level until the reveal.
- **Anomaly Attention:** the main tension meter, replacing "you might lose." Fast growth and reveals raise it; thresholds trigger hunters, System "corrections," Ancient envoys, and rivals. A separate **Corruption** meter tracks dark and cursed skills.
- **Witnesses and Legend:** feats done in front of NPCs spread as rumors and turn into titles and faction reactions. The same feat reads differently per faction (the Haven Council fears a massacre; the Iron Guild admires it).
- **Gap Breaking:** the Grade-gap damage rule still applies to the world, but beating an enemy a Grade above you grants huge XP, a "Gap Breaker" title chain, and a shot at a Unique skill.
- **Army:** summons and followers that scale with the MC (shadow soldiers, tamed beasts, or a guild, depending on class), so the power fantasy works at scale in Acts 3–4.

### Re-tuned mechanics

- **Self-revealing UI:** the Skills, Bag, and Map tabs, and later the Anomaly and Root Access tabs, stay hidden until the System grants them, each with its own notification ("[Inventory granted]").
- **Passive System insight:** high stats add private lines to scenes (see above).
- **Flex options:** high stats unlock options like "[STR 40 · Trivial] Tear the door off its hinges" that solve problems in style. Retryable (white) and one-shot (red) checks stay, with visible odds, but they're used mostly for social and moral checks.
- **Opportunity hand:** during free time, three random event cards are dealt; the player picks one.
- **Two combat modes:**
  - **Real fights** (equals and bosses): tactical, with a stance each round (all-out / measured / defensive) and a Stamina commitment; narrated in prose with numbers on demand; ending in an **execution** finisher the player picks.
  - **Crush:** fodder the MC far outclasses auto-resolves in one stylish line. No grinding.
- **Companion reactions:** before big choices, companions weigh in with a one-liner based on personality and relationship. They also react to the MC's growing power with awe, worry, jealousy, or dependence. Devon's arc becomes "watching my best friend turn into a monster or a god."
- **Knowledge journal:** rumors, secrets, and true levels the MC learns unlock dialogue options; lies told to NPCs go into their memory and can come back to bite.
- **Marks:** on the MC, mutations and bloodline awakenings that carry a cost; on NPCs, permanent traits and scars from what the MC did to them.
- **Rivals can snatch "First" titles before the MC.**
- **Notifications:** rare, opt-in Android notifications between sessions ("[Ash Wolfe passed you on the Global Ranking.]", "[A Grade-A entity has noticed you.]"), each opening a small event. The notification permission is requested the proper way.

### Balance rules

- **The real choices are moral.** The MC gets powerful no matter what; the morality axes decide what kind of OP MC they become (protector, tyrant, god, liberator). Every important choice moves at least one axis.
- **Ironman deaths come from overreach** (provoking higher-tier entities while revealing everything), not from random fights.
- **The simulator** reports the MC/NPC power gap per act, the share of playtime spent in "domination" (target: 60%+), Attention over time, and the ending distribution.

## World and story framework

The game opens on Integration Day, when the System plugs Earth into the "Multiverse Network." Humanity has 365 days: at the end, Earth is either accepted as a full Layer or harvested.

**Integration Day (Day 0).** Blue windows flicker open across the sky and everyone is teleported into the Tutorial Layer. Thirty days of monsters, a first class offer, and the first moral tests follow. The share of humans who make it out of the tutorial (`dunya.hayatta_kalan_orani`, the survival rate) sets how strong humanity is in later acts.

**The System.** It looks neutral but runs its own agenda. Its notifications are dry and occasionally sarcastic, like a DMV clerk who knows something you don't. The game's central mystery: who wrote the System, and why? There is no single answer; the player's choices in Act 2 decide which truth turns out to be "real."

**Locations**

- **Tutorial Layer** — old-growth forest, a ruined city, a gutted dead mall, the first dungeon gates; Days 0–30.
- **The Integrated World** — on return, the player's hometown of Port Harlow (a fictional port city in the Pacific Northwest) and the surrounding county have turned into monster zones. Town Cores can be captured and run.
- **Gates** — dungeon portals ranked E through S. If one isn't closed in time, it triggers a "gate break" and monsters spill into the region.
- **The Spire** — a 100-floor vertical dungeon; every 10th floor reveals a story secret.
- **The Multiverse** — alien worlds and their politics, unlocked in Act 3.

**Factions**

| Faction | What they want | What they expect from you |
| --- | --- | --- |
| Haven Council | Protect civilians, govern by consensus | Defend towns, share resources |
| Iron Guild | A meritocracy run by the strongest | Clear gates, put down rivals |
| The Ascended | A cult that worships the System and sorts people into "chosen" and "fuel" | Rituals, sacrifice, total loyalty |
| The Quiet Network | Hidden researchers trying to reach the System's source code | Gather intel, run risky covert jobs |
| The Ancients | A multiverse race that wants to harvest Earth or make it a vassal | Trade, diplomacy, betrayal |

**The core conflict runs on three levels.** Outside, the Ancients are waiting for Day 365. Inside, the factions fight over how humanity should survive. Underneath it all sits the System itself: as the player rises, they discover whether it is a harvesting tool or a rescue protocol.

## System mechanics

Character power has five layers: stats, level and grade, class, skills, and titles. In the late game, Essence (conceptual power) sits on top of all of them.

### Stats

| Stat | Abbr. | What it does |
| --- | --- | --- |
| Strength | STR | Physical damage, carry weight, forcing some doors open |
| Agility | AGI | Initiative, dodge, crit chance |
| Endurance | END | Health (HP), physical resistance, Stamina |
| Intelligence | INT | Spell damage, mana pool, puzzle solutions |
| Wisdom | WIS | Mana regen, spell resistance, grasping Essence |
| Perception | PER | Accuracy, crit damage, spotting hidden items, traps, and lies |
| Willpower | WIL | Mental resistance, fear effects, using Essence |
| Charisma | CHA | Persuasion, relationship gains, companion capacity |

Each level grants automatic stat gains based on class + 3 free points. Derived values:

```latex
HP = 50 + 10 \cdot END + 5 \cdot L \qquad Mana = 30 + 8 \cdot INT + 3 \cdot WIS \qquad Stamina = 40 + 5 \cdot END + 5 \cdot AGI
```

```latex
XP_{needed}(L) = \lfloor 100 \cdot L^{1.8} \rfloor \qquad XP_{gained} = XP_{base} \cdot 1.15^{(L_{enemy} - L_{player})}
```

The level-gap bonus richly rewards taking down something stronger than you, which is the heart of the genre; the multiplier is clamped between 0.1 and 5.

### Grades and evolution

| Grade | Level range | Unlocked at evolution |
| --- | --- | --- |
| F | 1–24 | Base class (at Level 10) |
| E | 25–49 | First class evolution, second Profession branch |
| D | 50–99 | Essence slot 1, companion capacity +1 |
| C | 100–199 | Essence fusion, Spire floors 50+ |
| B | 200–399 | Multiverse travel |
| A | 400–799 | Path (a unique power built from two Essences) |
| S | 800+ | Final-act authority |

At every grade threshold, leveling stops and an **Evolution Quest** opens. It is a story event: the player picks one of two or three evolution paths, and that choice sets the class name, stats, and one new skill.

### Class: an offer born from behavior

No class menu. Every action quietly adds points to a hidden **Affinity** vector: Melee, Ranged, Magic, Stealth, Warding, Summoning, Crafting, Shadow. At Level 10, the System offers three classes: one from each of the top two affinities plus one rare or hidden class unlocked by special flags.

Hidden class examples:

- **Gate Eater** — solo a gate boss five or more levels above you.
- **System Glitch** — find three glitch events and talk to Echo.
- **The Mourner** — after a companion dies, go three days without fighting anyone.
- **Hunter's Eye** — kill 50 enemies with a first-strike crit.

Class is separate from the **Profession** tree: Alchemist, Blacksmith, Runesmith, Chef, Cartographer. Professions level on their own and produce gear, potions, and maps.

### Skills

- **Rarity:** Common → Rare → Epic → Legendary → Mythic → Unique. A Unique skill exists in only one person per playthrough, and NPCs can claim it first.
- **Use-based growth:** every skill has levels 1–10 and earns XP as you use it.
- **Branching evolution:** at level 10 a skill evolves down one of two paths (example: *Shadow Step* → *Void Step* or *Shadow Hunt*).
- **Fusion:** two maxed skills + a condition → a new skill (example: *Flame Arrow* + *Wind Step* + 100 shots fired on the move → *Comet Shot*).
- **Cursed skills:** dark choices grant strong skills with side effects (example: *Soul Siphon* heals you as you deal damage, but costs a permanent −1 CHA per grade).

### Titles, Essence, and Legacies

- **Titles** grant stat bonuses and change how NPCs react. "First" titles go to whoever does something first on the whole planet (*First Gate Closer*, *First Blood of the Tutorial*). Negative titles exist too (*Kinslayer*: Haven Council reputation capped at −50).
- **Essence:** starting at D grade, the player grasps Essences through experience: Fire, Space, Death, Warding, Ruin, Time, the Hunt, Echo. At A grade, two Essences merge into the player's own **Path**. The Path decides the final-act ultimate skill and some ending conditions.
- **Legacies:** hidden quests can pass down a dead Ancient's legacy. It grants huge power with strings attached: the memory of whoever left it talks to the player and pushes its own goals.

### Daily Quest and economy

- **Daily Quest:** every in-game day the System assigns a world-grounded task (haul the camp's water, walk the border, close 3 gates; never gym rep counts). Finish it for +1 free stat point (plus one more for every 7-day streak); skip it and risk being pulled into the Stopped Hour (DECISIONS #98). **Only the MC has the Daily Quest** (see OP MC design).
- **Currency:** Credits (System shop, trading) and Essence Crystals (evolutions, skill fusion).
- **Gear:** 8 slots, rarity colors, set bonuses; upgraded through Professions.

## Combat system

Combat is turn-based but leans into being text: the player doesn't just tap "attack," they read the room and the enemy's weak spot and pick creative actions.

**Turn flow**

1. Roll initiative; the highest goes first.
2. The active side takes one **main action** + one **minor action** (item, reposition, a quick line of dialogue).
3. Status effects tick, cooldowns count down.
4. Bosses check phase thresholds (70% / 40% / 15% HP).
5. Repeat until one side drops, flees, or surrenders.

**Main actions:** Attack · Skill · Defend · Analyze (PER check; reveals stats and weak spots) · Environment (use terrain tags: bring the ceiling down, electrify the water) · Talk (persuade, threaten, or bargain with intelligent enemies) · Flee.

**Formulas**

```latex
Initiative = AGI + 0.5 \cdot PER + d20
```

```latex
Hit = 75\% + (PER_{attacker} - AGI_{defender}) \cdot 1.5\% \quad (\text{clamped } 5\% \text{ to } 95\%)
```

```latex
Damage = (Base_{skill} + Stat_{main} \cdot Mult_{skill}) \cdot \frac{100}{100 + Resist} \cdot (1 \pm 0.1)
```

```latex
Crit_{chance} = 5\% + 0.2 \cdot AGI \qquad Crit_{mult} = 1.5 + 0.005 \cdot PER
```

Grade gaps add a brutal multiplier: damage ×2 per grade up, ×0.5 per grade down. That keeps the genre feeling that you basically can't beat someone a grade above you head-on, and pushes weaker players toward traps, terrain, and weak spots.

**Status effects**

| Effect | What it does | Typical source |
| --- | --- | --- |
| Bleed | 3% of max HP per turn | Blades, claws |
| Poison | Flat damage per turn, healing −50% | Spiders, alchemy |
| Burn | Damage per turn, defense −10% | Fire magic |
| Stun | No action for 1 turn | Crits, shield bash |
| Fear | WIL check; on a fail, can't attack | Boss roar |
| Blind | Accuracy −40% | Smoke, flash |
| Haste | Two main actions per turn | Skills, potions |
| Shield | Absorbs a set amount of damage | Warding skills |

**Enemy design.** Every enemy has a behavior pattern (aggressive, cautious, pack, skittish), a weak spot, and a story hook. Bosses unlock new skills at phase changes and trigger a short story beat. Smart enemies remember you: one that gets away can come back stronger.

**Death.** In Normal mode the System grants one **Second Chance** per grade (the price: a random skill loses 2 levels, or you lose a title). After that, death reloads your last save. In **Ironman Mode**, death is permanent and the run locks into the Harvest ending with the world as it stood. Deaths should come from overreach, not random fights: fodder far below the MC resolves in **Crush** mode, and the real danger is raising Attention and provoking higher-tier entities. NPC deaths are permanent in every mode.

## Dynamic story engine

The story isn't a fixed branching tree; it is made of events that run on a single **World State**. Every choice changes that state, and a **Story Director** reads it to pick the next event. That's how thousands of different playthroughs emerge without hand-writing hundreds of branches.

&#91;embedded content: story engine loop · 8 parts\]

NPC agendas and ripening seeds write into the World State every day; the Director decides only by reading that state; every line the player reads comes from pre-written content.

### World State (single source of truth)

- **Flags:** true/false values such as `deniz_kurtarildi` (Devon rescued), `liman_mahallesi_dustu` (the harbor district fell), `yanki_ile_konusuldu` (spoke with Echo). Data IDs are shared with the Turkish build and stay as they are.
- **Variables:** `gun` (day), survival rate, gate break count, Ancient threat (0–100).
- **Faction reputation:** −100…+100 per faction, plus a faction-to-faction relationship matrix.
- **Morality axes** (shift with the player's actions, −100…+100):
  - Mercy ↔ Cruelty
  - Order ↔ Freedom
  - Humanity ↔ Power (alignment with the System)
- **Region states:** who controls each region, its danger level, its open gates.
- **Seeds:** a queue of delayed consequences (below).

### Event structure

Each event is a data file: preconditions, weight, tags, text, and options. Each option has immediate effects and optional seeds. Options can be gated: a stat, skill, title, item, or relationship threshold unlocks new ones ("\[PER 30\] Catch the lie").

### Seed and harvest (delayed consequences)

Big choices don't hit right away; they hit later. A choice drops a seed into the queue: "in 12–20 days, if this is still true, fire this event." Example: leave an injured stranger behind in the tutorial, and on Day 40 that person shows up as an Ascended commander who remembers exactly what you did.

### Chapter structure: base and missions

The main story runs on a proven structure (details in `docs/CHAPTERS.md`, decisions DECISIONS #130–#139):

- **Spine (branch and merge):** 17 chapters plus a finale (3 in Act 1, 6 each in Acts 2 and 3, 2 in Act 4). Every chapter
  ends in a **bottleneck** scene everyone plays, whose content changes with the world state.
- **Calendar (Persona-style):** each chapter has a main operation and a deadline. The days before it are free; the player
  picks when to go in (world events are dated: the ship lands, the invasion starts). Missing a deadline doesn't end the game;
  an overflow hits (danger, survivor rate, a permanent flag) and the operation starts anyway.
- **Loop (Mass Effect 2-style):** at **base** (the Tutorial camp in Act 1, then the Haven) come character scenes, seed
  payoffs, NPC thresholds, Global Ranking announcements, companion reactions and the opportunity hand. A **mission** is a
  closed run of scenes that opens with travel and an arrival scene; inside it only the mission's own events play. Every
  event has a location and never fires anywhere else; an event set elsewhere shows up at base as a mission card.
- **Chapter rhythm:** Briefing → Preparation → Main operation → Aftermath → Bottleneck.
- **Dated operations** (world events) can't be entered early; the objective line counts down ("6 days until the ship
  arrives"). Free days go to **preparation** (scouting, fortifying, rallying allies): each leaves a flag and changes the
  operation (fewer losses, an extra option or a better start). A side mission is never cut off on operation day, and the
  game saves to a separate slot right before the operation.
- **Training / skipping time:** quiet days at base can be spent on one focus: a stat, clearing Gates, your profession,
  or time with a companion. During preparation you pick anywhere from 1 day up to 2 days before the operation; after it,
  up to the bottleneck day, and once the chapter is done, up to the next chapter's opening. Days still pass one at a
  time (NPCs, Gates, deadlines). Gains are below the Daily Quest and normal play: 1 stat point every 3 days (the Daily
  Quest gives a free point every day) and 25% / 50% / 100% of a level's XP per day in Act 1 / 2 / 3–4 (normal play
  levels faster in every act); Gates give danger −3 a day and 1 Essence Crystal every 3 days; profession gives 5 profession XP a day; a companion gives +1 trust a
  day (max +6) and +1 bond every 2 days (max +2). Skipped days' Daily Quests carry no penalty (the streak neither grows
  nor breaks). There's a 25% chance a pool event interrupts; a newly due seed or a ready main-arc node stops the skip.
  An uninterrupted skip ends with a short summary scene for that focus.
- The UI always shows a "Chapter N · Objective · Days left" line; every seeded event opens with a reminder line; a short
  recap appears when you come back after a long break.

### Story Director

At every step the Director filters the eligible events, then scores them:

```latex
Score = Weight \cdot Tension_{fit} \cdot Freshness \cdot Personal_{bond} \cdot Priority_{mainarc}
```

- **Tension fit:** events that match the target tension curve (rising, peak, breather) get a boost; Anomaly Attention is the main source of tension. Three hard fights in a row are followed by a campfire scene.
- **Freshness:** recently seen tags are penalized.
- **Personal bond:** events featuring NPCs the player is close to get a boost, so the story orbits the people the player cares about.
- **Main arc priority:** required story beats always win when their time comes.

## Dynamic characters

NPCs aren't scripted puppets; they are agents with their own goals, fears, and memories. Whatever the player does, every NPC advances their own agenda one step per in-game day. Death, betrayal, and faction switches are permanent.

### NPC model

- **Personality:** 5 traits, 0–10 — Courage, Ambition, Conscience, Loyalty, Suspicion.
- **Values:** where they stand on the morality axes. The player's actions are judged against these: a high-Conscience NPC who sees you execute a prisoner loses trust; a high-Ambition NPC respects it.
- **Relationship axes** (−100…+100): Trust, Respect, Fear, Devotion. Four axes instead of one "friendship" meter create layered states like "scared of you, but respects you."
- **Goals:** a prioritized list ("find my sister," "take over the Guild"). When a goal is met or becomes impossible, a new one is picked.
- **Memory:** a log of key events involving the player, each with an emotional weight. Small moments fade; big ones (saving a life, betrayal) stick. Dialogue and event conditions draw on this memory.
- **Their own growth:** NPCs level up, learn skills, and evolve grades too. Neglect an NPC and they get stronger some other way.

### Threshold events

| Condition | Result |
| --- | --- |
| Trust < −40 and Ambition ≥ 7 | A betrayal seed is planted; betrayal event within 5–15 days |
| Devotion ≥ 70 | Personal quest unlocks; completing it grants a shared skill or title |
| Fear ≥ 60 and Courage ≤ 3 | Runs away, or obeys the player blindly |
| Respect ≥ 60, Trust ≤ 0 | Becomes a rival and challenges the player to a duel |
| Gap between their values and the player's axes > 120 | Switches factions or leaves the group |

### Main cast

| Character | Role | Possible fates |
| --- | --- | --- |
| Devon Hale | Your oldest friend, who wakes up next to you in the tutorial; Warding affinity | Loyal companion · Tragic rival who joins the Ascended · Hero who dies saving you |
| Marcus Stone | Iron Guild leader, retired Marine gunnery sergeant | Mentor · Strongman the player topples · Allied general in the final battle |
| Dr. Ellie Sawyer | ER physician who leads the Haven Council | Council chair and key to the peaceful ending · Killed in a Guild coup · Romance arc |
| Brother Vance | Prophet of the Ascended; a former self-help podcaster who claims the System talks to him | Exposed as a fraud · Turns out to be right · The player's door to the Ascension ending |
| Echo | A glitched fragment of the System; bonds to the player on Integration Day and gives them a corrupted, privileged System; a voice only the player can hear | Guide to the System's origin · A virus trying to take over the player · Deleted |
| Ash Wolfe | A mysterious rival leveling at the same pace as the player | Lifelong rival · Multiverse companion · The human who reaches S grade instead of you |
| Miles Kincaid | Quiet Network researcher, ex-cybersecurity engineer | Key to the Codebreaker ending · An informant sold to the Ancients |
| Envoy Thessar | The Ancients' envoy to Earth | Partner in the Vassal ending · An ally who betrays his own race · Final boss |

Each main character has 3–5 "arc beats"; which ones fire depends on the relationship axes and the World State. Secondary NPCs (traders, town mayors, gate hunters) use a lighter version of the same model and can be generated procedurally with North American names.

## Structure: acts and endings

The game has four acts and resolves on Day 365. The ending isn't decided by a single choice but by the World State built up over the whole run; the big decision in the final act only lets the player choose among the endings they've earned.

### Acts

| Act | Days | Grade | Key story beats |
| --- | --- | --- | --- |
| 1 — The Tutorial | 0–30 | F | Waking up, first kill, Level 10 class offer, meeting Devon, tutorial boss, first "First" title |
| 2 — The Integrated World | 31–180 | E–C | Coming home to Port Harlow, choosing a faction, capturing a Town Core, first gate break, Echo appears, first piece of the System's truth |
| 3 — The Multiverse | 181–330 | C–A | The Ancient envoy, Spire floors 50+, multiverse travel, forming a Path, the great betrayal |
| 4 — The Harvest | 331–365 | A–S | Factions take their final positions, the Ancient invasion, the System's core, the final decision |

Act changes are driven by time and events: if the player dawdles, the world doesn't wait, and Act 3 events start on Day 180 regardless.

### Endings

| Ending | Summary | Key conditions |
| --- | --- | --- |
| The Guardian | Earth is accepted as a full Layer and the player becomes humanity's shield | Mercy ≥ 40, Haven Council rep ≥ 50, survival rate ≥ 40%, Ancients defeated |
| The Iron Crown | The player becomes Earth's absolute ruler | Order ≥ 50, Power ≥ 30, Iron Guild control or Marcus overthrown |
| Ascension | The player merges with the System and becomes a god; humanity becomes fuel | Power ≥ 70, Ascended questline complete, Brother Vance alive |
| Codebreaker | The player rewrites the System and sets the rules | Quiet Network questline complete, Echo trust ≥ 60, Miles alive |
| Liberation | The System is destroyed; every level resets to zero, humanity is weak but free | Freedom ≥ 60, Humanity ≥ 50, System core reached |
| Vassal | A deal with the Ancients: Earth survives, under their thumb | Devotion with Thessar ≥ 50, Ancient threat ≥ 70 |
| Lone Hunter | Every companion is dead or gone; the player heads into the multiverse alone | Active companions = 0, player at S grade |
| The Harvest | Bad ending: humanity never gets strong enough and Earth is harvested | No ending conditions met by Day 365, or an Ironman death |
| The Loop (secret) | The player learns the System is a loop and starts a new Integration; unlocks New Game+ | Every Spire floor + Echo's final secret + at least one previously finished run |

**Ending presentation.** The ending screen tells the story of the world, not just the hero: what happened to each main NPC, where each faction landed, and the player's 5 most important choices with their long-term impact (epilogue cards in the style of classic RPG slideshows).

## UI and player experience on Android

The game is built for one-handed play in portrait on an Android phone and should feel like reading a page of a LitRPG novel: narration flows as prose, while System notifications arrive on their own cards with color, haptics, and a short animation. On tablets the text column is centered and a side panel keeps the status summary open.

**Visual identity: "Broken Window."** The System is a flawless glass window: clipped corners, a single ice-blue accent, faint scan lines. Headings and numbers use an angular display face, the System's voice is monospaced, and narration is set in a serif built for long reading. The hero is an error the window can't measure, and the Echo is that error's voice: its lines don't sit inside the window but bleed in as an inverted, shifted strip. Every big moment gets its own screen. On a level up the window opens and the number counts up; on an evolution the new grade letter takes the old one's place; a Unique skill turns the window white-gold; on the Global Ranking the old rank is struck through. When a limit breaks (your true level comes out, or a fight ends in a Crush), the screen briefly crashes and the System is forced to print the difference between `public` and `true`. The dark theme's background is pure black.

**Sample screen (phone, Story tab)**

```text
┌──────────────────────────────────┐
│ Day 14 · Dead Mall           ☰   │
│ HP ████████░░ 182/240            │
│ MP ██████░░░░  60/95             │
├──────────────────────────────────┤
│ Something clicks above the food  │
│ court. Devon raises the shield,  │
│ but you catch it: the sound is   │
│ coming from the shattered        │
│ skylight, not the floor.         │
│                                  │
│ ┌─ SYSTEM ─────────────────────┐ │
│ │ [Perception] check passed.   │ │
│ │ Hidden enemy: Skylight Spider│ │
│ │ Lv. 17 · Rare · Weak: Fire   │ │
│ └──────────────────────────────┘ │
├──────────────────────────────────┤
│ [ Warn Devon and fall back     ] │
│ [ Flame Arrow at the skylight  ] │
│ [ PER 25 · Cut the anchor line ] │
│ [ Stay quiet. Use Devon as bait] │
├──────────────────────────────────┤
│ Story  Status  Skills  Bag     ⋯ │
└──────────────────────────────────┘
```

**System notification types:** level-up, skill gained/evolved, title, quest, warning, hidden-condition hint, and Echo's "corrupted" notifications (glitched text, scrambled letters). Rarity colors: gray, green, blue, purple, orange, red, gold. Big moments get a short haptic buzz and an optional sound effect.

**Navigation.** A bottom navigation bar: Story · Status (stats, grade, titles, Essence) · Skills · Bag (inventory, gear) · More (map, Daily Quest, relationships, quests, saves, settings). In combat, the option list becomes an action grid (Attack, Skill, Defend, Analyze, Environment, Talk, Flee) with target selection. Long-press a skill or enemy to open its detail card.

**Pacing and accessibility.** The typewriter effect can be toggled; tapping the screen finishes the line instantly. Level-ups and evolutions get a full-screen card and a short animation. Long fights have an "auto-turn" option. Font size, light/dark theme, high contrast, and TalkBack (screen reader) are supported.

**Saving.** Auto-save at the end of each in-game day and whenever the app goes to the background, plus 3 manual slots; Ironman Mode has a single auto slot. Saves are JSON in the app's private storage and back up to the player's Google account through Android Auto Backup. A save holds the full World State, seed queue, NPC memories, and RNG seed; the same save always produces the same outcome.

### Localization (English / Turkish)

- Language defaults to the device language on first launch and can be switched mid-game in Settings. Saves are language-independent because the World State only stores IDs.
- UI strings live in Android resources (`values/strings.xml` English, `values-tr/strings.xml` Turkish); every player-facing content field is stored as `{tr, en}`.
- **The English build is a full North American culturalization, not a translation.** Names of people, places, and factions change, as do units (miles, pounds, °F), humor, idioms, and cultural references. Story, choices, and mechanics stay identical.
- Content text never hard-codes names; it uses tokens (`{npc:deniz_arman}`), so the same event says "Deniz" in Turkish and "Devon" in English.
- English copy reads like native genre fiction; System notifications keep the bracketed LitRPG style ("\[Skill evolved: Shadow Step → Void Step\]").
- Helpers handle grammar: Turkish suffixes by vowel harmony; English a/an, plurals, and pronouns.

**Localization reference (TR → EN, North America)**

| Type | Turkish | English (North America) |
| --- | --- | --- |
| Game title | Yükseliş Protokolü | Ascension Protocol |
| City | Kıyıkent (Aegean coast, fictional) | Port Harlow (Pacific Northwest, fictional) |
| Character | Deniz Arman · Kaya Demir · Elif Sarı · Vahit | Devon Hale · Marcus Stone · Dr. Ellie Sawyer · Brother Vance |
| Character | Aras Kurt · Mert Kılıç · Yankı · Elçi Thessar | Ash Wolfe · Miles Kincaid · Echo · Envoy Thessar |
| Faction | Sığınak Konseyi · Demir Lonca · Yükselenler · Sessiz Ağ · Kadimler | Haven Council · Iron Guild · The Ascended · The Quiet Network · The Ancients |
| Place | Öğretici Katman · Harabe Çarşı · Kasaba Çekirdeği · Kule · Kapı | Tutorial Layer · Dead Mall · Town Core · The Spire · Gate |
| Concept | Sistem · Kademe · Öz · Yol · Eğilim · Meslek | the System · Grade · Essence · Path · Affinity · Profession |
| Concept | Günlük Görev · Durmuş Saat · İkinci Şans · Demir Mod | Daily Quest · The Stopped Hour · Second Chance · Ironman Mode |
| Stats | GÜÇ · ÇEV · DAY · ZEK · BİL · ALG · İRD · KAR | STR · AGI · END · INT · WIS · PER · WIL · CHA |
| Rarity | Sıradan · Nadir · Epik · Efsanevi · Mitik · Benzersiz | Common · Rare · Epic · Legendary · Mythic · Unique |
| Hidden class | Kapı Yiyen · Sistem Hatası · Yas Tutan · Avcı'nın Gözü | Gate Eater · System Glitch · The Mourner · Hunter's Eye |
| Skill | Gölge Adımı · Boşluk Adımı · Alev Oku · Kuyruklu Yıldız Atışı · Ruh Emme | Shadow Step · Void Step · Flame Arrow · Comet Shot · Soul Siphon |
| Title | İlk Kapı Kapatan · Öğreticinin İlk Kanı · Kardeş Katili | First Gate Closer · First Blood of the Tutorial · Kinslayer |
| Essence | Ateş · Uzay · Ölüm · Koruma · Yıkım · Zaman · Avlanma · Yankı | Fire · Space · Death · Warding · Ruin · Time · the Hunt · Echo |
| Ending | Koruyucu · Demir Taht · Yükseliş · Kod Kırıcı · Özgürlük | The Guardian · The Iron Crown · Ascension · Codebreaker · Liberation |
| Ending | Vassal · Yalnız Avcı · Hasat · Döngü | Vassal · Lone Hunter · The Harvest · The Loop |
| Currency | Kredi · Öz Kristali | Credits · Essence Crystals |
| Units | 10 km koşu · 100 şınav · kg · °C | 6-mile run · 100 push-ups · lb · °F |

## Technical architecture and data schema

Recommended stack: **Kotlin + Jetpack Compose** (native Android UI, Material 3) + **kotlinx.serialization** and **kaml** (saves and YAML content) + **JUnit 5** (tests). Minimum Android 8.0 (API 26), targeting whatever API level Google Play currently requires. The game engine is a pure Kotlin module with no Android dependency; Compose only displays it. Code and content are fully separate: adding an event, NPC, skill, or enemy means adding a YAML file, not writing code.

One repository serves both languages. Data IDs and YAML keys are shared with the Turkish build and stay in Turkish ASCII (`on_kosullar`, `secenekler`); only player-facing text differs.

**Folder structure**

```text
yukselis-protokolu/
├── CLAUDE.md                  # standing project rules for Claude Code
├── docs/GDD.md                # Turkish design doc (source of truth for mechanics and story)
├── docs/GDD.en.md             # this document (source of truth for English names and terms)
├── engine/                    # pure Kotlin module, no Android dependency
│   └── src/main/kotlin/…/     # core/ (state, rng, clock, save) · system/ · combat/
│                              # story/ · npc/ · i18n/
├── app/                       # Android app (Jetpack Compose)
│   └── src/main/
│       ├── kotlin/…/ui/       # story/ status/ skills/ inventory/ combat/ settings/ + ViewModels
│       ├── res/values/        # strings.xml (English)
│       └── res/values-tr/     # strings.xml (Turkish)
├── content/                   # events/ npcs/ skills/ classes/ titles/ enemies/ items/ regions/
│                              # (every text field tr + en; validated and copied to assets at build)
└── tools/simulator/           # automated playthrough and balance simulator (JVM)
```

**Sample event file**

```yaml
id: ogretici_yarali_yabanci
perde: 1
on_kosullar:
  gun: { min: 3, max: 20 }
  bolge: harabe_carsi
  bayrak_yok: [yabanci_karsilasildi]
agirlik: 5
etiketler: [ahlak, karsilasma, gerilim_orta]
metin:
  tr: "Çöken bir duvarın altından bir el uzanıyor. {npc:deniz_arman} sana dönüyor: 'Sürü geliyor!'"
  en: "A hand claws out from under a collapsed wall. {npc:deniz_arman} spins toward you: 'The pack's coming!'"
secenekler:
  - metin:
      tr: Duvarı kaldırıp yabancıyı kurtar
      en: Lift the wall and pull the stranger free
    kosul: { stat: { str: 18 } }
    etkiler:
      ahlak: { merhamet: +10 }
      bayrak: [yabanci_kurtarildi]
      npc_olustur: { sablon: yabanci_kapi_avcisi, iliski: { guven: 30 } }
  - metin:
      tr: Onu bırak ve kaç
      en: Leave them and run
    etkiler:
      ahlak: { merhamet: -15 }
      bayrak: [yabanci_birakildi]
    tohum:
      olay: yukselen_komutan_intikam
      gecikme_gun: { min: 12, max: 20 }
      kosul: { bayrak: [yabanci_birakildi] }
```

In English this reads: leave the stranger behind (Mercy −15) and a seed schedules the "Ascended commander's revenge" event 12–20 days later.

**Sample NPC file**

```yaml
id: deniz_arman
ad: { tr: Deniz Arman, en: Devon Hale }
kisa_ad: { tr: Deniz, en: Devon }
kisilik: { cesaret: 7, hirs: 4, vicdan: 8, sadakat: 9, supheci: 3 }
degerler: { merhamet: 60, duzen: 10, insanlik: 70 }
baslangic_iliski: { guven: 50, saygi: 20, korku: 0, baglilik: 40 }
hedefler:
  - { id: kardesini_bul, oncelik: 10 }
  - { id: oyuncuyu_koru, oncelik: 7 }
egilim: { koruma: 8, yakin_dovus: 5 }
yay_dugumleri: [deniz_ilk_kavga, deniz_kardes_ipucu, deniz_yukselenler_teklifi, deniz_son_secim]
```

**Architecture rules**

- The engine (`engine`) is pure Kotlin with no Android dependency: it's tested with JUnit on a desktop and can later be ported to iOS or PC.
- All randomness goes through a single seeded RNG, so tests and saves are reproducible.
- The World State is a single immutable data class; events change it only through effect functions. The UI observes it through a ViewModel as a StateFlow.
- The condition language (`on_kosullar`, `kosul`) is a small, safe DSL; no script execution.
- Content text never names people; it uses tokens: `{npc:deniz_arman}`, `{yer:harabe_carsi}`, and `{npc:deniz_arman|e}` for Turkish suffixes ("Deniz'e" in Turkish, "Devon" in English).
- A `validateContent` Gradle task checks every YAML file before build: undefined flags, unreachable events, broken references, undefined name tokens, missing or empty translations.

## Development roadmap and Claude Code prompt

Build the game in 9 phases, with a playable build at the end of each, not in one shot. The most important milestone is Phase 6: a vertical slice where the Tutorial Layer is playable start to finish on a phone, in both languages. Don't move on to later acts until the mechanics have been tested there.

**How to use this**

1. Install Android Studio (it includes the Android SDK and emulator) and Claude Code.
2. Export the Turkish tab as `docs/GDD.md` and this English tab as `docs/GDD.en.md` (Markdown) into an empty project folder.
3. Run `git init` in that folder, start Claude Code, and paste the main prompt below. It will present a plan first; approve it.
4. After each phase, try the app on the emulator or on your own phone via USB debugging, give feedback, then move to the next phase prompt. Running `/clear` between phases keeps quality up on a long project; durable knowledge lives in CLAUDE.md and docs/.

**Main prompt (first message)**

```text
You are a senior Android game developer and narrative designer. In this repo we are building "Ascension Protocol" (Turkish title: "Yükseliş Protokolü"), a single-player, text-based LitRPG for Android. We use mechanics in the style of Defiance of the Fall, He Who Fights With Monsters, The Primal Hunter, and Solo Leveling, but the story and characters are entirely original. The full design lives in docs/GDD.md (Turkish, source of truth) and docs/GDD.en.md (English, localized for North America). Read both before any work and stay faithful to the design. If mechanics or story conflict, docs/GDD.md wins; for English names and terms, docs/GDD.en.md wins.

CORE GOAL: Every choice must be able to change the course of the story and its ending. The story is not a fixed branching tree; it emerges dynamically from a single World State, conditional events, delayed consequences (seeds), a Story Director, and NPCs with their own goals.

TECHNICAL RULES
- Kotlin, Gradle (Kotlin DSL), Jetpack Compose + Material 3, kotlinx.serialization, kaml (YAML), kotlinx.coroutines, JUnit 5. Min Android 8.0 (API 26), target the API level Google Play currently requires.
- Two main Gradle modules: engine (pure Kotlin, no Android dependency, all game logic) and app (Compose UI, ViewModels, save file access). The UI observes engine state via StateFlow; game logic never leaks into app.
- Code and content are separate: events, NPCs, skills, classes, titles, enemies, items, and regions are YAML under content/, validated and copied to assets at build time. Never hard-code story text.
- All randomness goes through one seeded RNG. The World State is a single immutable data class changed only through effect functions. Write a small, safe DSL for conditions; no script execution.
- TWO LANGUAGES: English and Turkish. UI strings in res/values/strings.xml (English) and res/values-tr/strings.xml (Turkish). Every player-facing content field is a {tr, en} map. Language defaults to the device language on first launch and can be changed mid-game in Settings; saves are language-independent.
- The English build is a full North American culturalization, not a translation: names of people, places, and factions, units (miles, lb, °F), humor, idioms, and cultural references change; mechanics and story stay identical. Use the localization reference table in docs/GDD.en.md.
- Content text never hard-codes names; it uses tokens: {npc:deniz_arman}, {yer:harabe_carsi}, and {npc:deniz_arman|e} for Turkish suffixes. Write a resolver that picks names per language and Turkish suffixes by vowel harmony, plus English a/an, plural, and pronoun helpers.
- Data IDs and YAML keys are shared across languages and follow the examples in the GDD (Turkish ASCII). Kotlin identifiers are in English.
- Use the folder structure from the GDD's "Technical architecture" section.
- When a design detail is ambiguous, make the assumption that best fits the GDD and log it briefly in docs/DECISIONS.md; don't stop me with unnecessary questions.
- At the end of every phase: ./gradlew test and ./gradlew assembleDebug pass, the app launches on the emulator, update CLAUDE.md (architecture, commands, rules), make a meaningful git commit, and tell me 5 things to test on my phone.

PHASES
1. Core: Gradle project (engine + app), CLAUDE.md, World State model, RNG, game clock (day/act), save/load (JSON in app storage, 3 slots + auto-save, save on background, Android Auto Backup, Ironman Mode), i18n foundation (strings.xml, {tr, en} content, name tokens, grammar helpers, language picker), Compose skeleton: Story screen (flowing text, typewriter effect, tap to finish, option buttons), System notification cards with rarity colors, bottom navigation (Story, Status, Skills, Bag, More), light/dark theme.
2. System mechanics: 8 stats and derived values, XP/level formulas, F→S grades and Evolution Quests, hidden Affinity vector, Level 10 class offer (2 affinities + 1 hidden class), Profession tree, skills (rarity, use-based leveling, branching evolution, fusion, cursed skills), titles and "First" titles, Essence and Path, Daily Quest and penalty. Status and Skills screens; full-screen card, animation, and haptics for level-ups and evolutions.
3. Combat: turn flow, the GDD's initiative/hit/damage/crit formulas, grade-gap multiplier, status effects, Analyze/Environment/Talk/Flee actions, enemy behavior patterns, boss phases, escaped enemies returning, Second Chance. Combat screen: action grid, target selection, long-press detail cards.
4. Story engine: event loader, condition DSL, gated options (like [PER 25]), effects, seed queue, Story Director scoring (tension curve, freshness, personal bond, main-arc priority), faction reputation and faction relationship matrix, 3 morality axes, region states and gate breaks, validateContent Gradle task (undefined flags, unreachable events, broken references, undefined name tokens, missing or empty translations).
5. NPC system: personality, values, 4 relationship axes (Trust, Respect, Fear, Devotion), fading memory, goals and daily agenda simulation, the GDD's threshold events (betrayal, personal quest, rivalry, departure), NPC leveling, permanent death. Relationships screen.
6. Vertical slice, Act 1 content in both languages: the full Tutorial Layer (Days 0–30). At least 40 events, the first arc beats for Devon, Echo, and Ash, 15 enemies, 1 boss, 30 skills, 8 base + 4 hidden classes, 15 titles. The game must be playable start to finish on a phone in both languages.
7. Acts 2–4 and endings: remaining factions, character arcs, the Spire, the multiverse, 8 endings + the secret Loop ending, ending screen and epilogue cards (every NPC's fate, factions, the 5 most important choices).
9. Balance and release prep: in tools/simulator, write a JVM tool that uses the engine module to simulate 1,000 playthroughs with random and heuristic strategies; report the ending distribution, level curve, and death rates, and fix imbalances. Then prepare a signed release build (AAB), app icon, and Google Play store listings (EN-US and TR).

NOW: Read docs/GDD.md and docs/GDD.en.md end to end. First produce a plan for the full architecture and all 9 phases and get my approval. After approval, implement Phase 1 only and stop.
```

**Short prompt for later phases**

```text
Read docs/GDD.md, docs/GDD.en.md, CLAUDE.md, and docs/DECISIONS.md. Implement Phase <N> faithfully to the GDD. Show a short plan first, then build it. When done, run ./gradlew test and ./gradlew assembleDebug, update CLAUDE.md, commit, and tell me 5 things to test. My playtest notes: <your notes>
```

**Content-writing prompt** (speeds up Phases 6 and 7)

```text
Following the schema under content/ and the character personalities in the GDD, write 10 new events for <region/act>. Every event needs at least 3 options: at least one gated (stat, skill, title, or relationship), at least one that plants a seed, and at least one that shifts an NPC relationship axis. Fill every text field in both Turkish and English. The English must read like native North American genre fiction, not a translation (units, idioms, cultural references). Never write names into text; use the {npc:...} and {yer:...} tokens. Then run validateContent and confirm there are no unreachable events.
```
