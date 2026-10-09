# Design summary

The full brief is [`HANDOFF.md`](HANDOFF.md) (version 1.0, 2026-10-09). This page is the short version
the code is built against.

**Premise.** Elias Vale, former soldier and field systems engineer, repairs power and communication in the
Meridian Corridor, where people kept their technology but lost trust in the systems connecting them. The
theme is transmission: knowledge, responsibility and care pass through people the way messages pass
through relays.

**Feel.** Lonely, wet and industrial in the field; small and warm at home. Original assets only; S.T.A.L.K.E.R.
is an atmosphere reference, never a source.

**Core loop.** Receive a request → travel → inspect → diagnose → configure or repair → verify → report
honestly → observe consequences → keep or renegotiate commitments.

**Rules that shape the code.**

- The server is authoritative, also in single-player. Clients send intentions; the server validates and
  returns snapshots.
- Infrastructure is a small pure-Java model with stable IDs, separate from rendering. Power, configuration,
  reachability and policy are distinct facts.
- Nothing happens to a world until an operator explicitly creates a scenario.
- History records only successful, concrete acts. No morality score, no affection meter. Pauses and menus
  never cost the player anything.
- Every claimed feature must be observable in the world and verified; compiling is not completion.

**Working canon used so far.** Generator 6 units; pump 5; local transmitter 1; regional relay 3. Nia Serrat
isolated the supply when fuse F1 overheated. The relay antenna was knocked off its bearing in a storm.
