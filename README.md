# CHECKMATE — A Java Chess Game

**CSE-2208 · Object Oriented Programming Language Laboratory-II**
Abdus Salam Gifari (25524204145) · Nukhyai U Marma (25524204133)

A complete, rules-accurate chess game in Java: local two-player, an AI opponent,
LAN multiplayer, and a game-history database. Move generation is Perft-verified
to depth 5 (4,865,609 positions).

---

## Requirements

- **Java JDK 21** (or newer).
- **IntelliJ IDEA** (any edition) to build and run.
- **sqlite-jdbc** library — only needed to save game history (the game runs fine without it).

---

## Project layout

```
src/chess/
├── model/    rules engine  (Board, Piece + 6 subclasses, MoveGenerator, ...)
├── engine/   the AI         (Evaluator, SearchEngine)
├── ui/       Swing interface (GameFrame = run this, BoardPanel, StartMenu, ...)
├── net/      LAN multiplayer (Protocol, NetLink)
└── db/       database       (Database, GameRecord)
pieces/        piece images (PNG)  ← must sit next to src / the jar
```

The entry point is **`chess.ui.GameFrame`**.

---

## Running from IntelliJ

1. Open the project.
2. Make sure the `pieces/` image folder is at the project root (next to `src`).
3. Open `GameFrame.java` and click the green ▶ next to `main`.

### Enabling the database (optional but recommended)

1. **File → Project Structure → Libraries → + → From Maven…**
2. Enter: `org.xerial:sqlite-jdbc:3.46.1.3`
3. **OK → OK.** IntelliJ downloads it.
4. Run again. A file `checkmate.db` is created in the project folder — that is your database.

---

## Building a shareable JAR (for two-computer LAN)

1. **File → Project Structure → Artifacts → + → JAR → From modules with dependencies.**
2. Main Class: `chess.ui.GameFrame`. **OK.** (Choosing *with dependencies* bundles sqlite-jdbc.)
3. **Build → Build Artifacts → Build.** The jar appears under `out/artifacts/`.
4. Copy **the jar and the `pieces/` folder together** to the second computer.
5. On each computer (needs Java installed): double-click the jar, or `java -jar CHECKMATE.jar`.

---

## How to play

The app opens on a menu with three modes:

- **Pass & Play** — two players on one screen. A popup asks for a time control (Off / 5 / 10 / 15 min).
- **Play vs AI** — choose difficulty (Easy / Medium / Hard), your colour, and a time control.
- **Play with Friends (LAN)** — one player hosts, the other joins by IP.

**LAN steps:** both open the app → one picks *Start Hosting* and reads out the IP shown →
the other types that IP and clicks *Connect*. Same Wi-Fi, port **50505**. On the first host,
allow Java through the Windows firewall (private network).

**Test LAN on one computer:** run two instances (Run → Edit Configurations → Allow multiple
instances), host in one, join the other with `127.0.0.1`.

In-game controls: New Game (back to menu), Undo, Redo, Pause, Resign, Move List, Captured, Quit.
Undo/Redo/Pause are disabled in LAN games.

---

## Demo-day checklist

Show it in this order — it builds from correctness to features:

- [ ] **Local game** works with no network (always have this as a fallback).
- [ ] **Special rules on demand:** castling, en passant, promotion, a checkmate, a stalemate.
- [ ] **Perft** — run the console Perft to show the counts match (the correctness proof).
- [ ] **vs AI on Hard** — let the engine find a tactic or a mate; let the examiner lose.
- [ ] **Chess clock** — play a quick timed game to the flag.
- [ ] **LAN** — two machines (or two windows via `127.0.0.1`) playing live.
- [ ] **Resign** — show the resign flow and confirm dialog.
- [ ] **Game History** — open the database browser and show saved games with their moves.
- [ ] **Backup:** record a short video of the LAN game in case the demo network fails.

---

## Correctness (Perft)

| Depth | Correct value | Our engine |
|------:|--------------:|:----------:|
| 1 | 20 | ✓ |
| 2 | 400 | ✓ |
| 3 | 8,902 | ✓ |
| 4 | 197,281 | ✓ |
| 5 | 4,865,609 | ✓ |

All counts match — move generation is provably correct.
