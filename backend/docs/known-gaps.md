# Token Coverage

We know the numbers in this dashboard undercount actual usage. We don't know by how much, overall or per plugin — this document names the known mechanisms instead of putting a
number on something we can't measure.

## Claude

A few known gaps in how the Claude plugin captures usage:

- If a turn is interrupted (e.g. Ctrl+C), it isn't captured directly at that moment. Recovery happens later, at session end, but only within a bounded window. The actual point of
  interruption may lie further back in the session than what gets recovered. So some interrupted turns are picked up, others aren't, depending on how far back they happened.
- Subagents started by an interrupted turn are not covered by that recovery at all. They never get captured, then or later.
- If the process is killed outright (e.g. `SIGKILL`), whatever hadn't been written to the local queue yet is lost. Entries already queued are not lost, they get sent in the next
  session.
- A subagent that itself starts further subagents (nested subagents) isn't captured.
- A subagent still running when its parent turn ends isn't captured for that turn.

## GitHub Copilot

Capture for Copilot relies on an event that fires once a session shuts down in the normal, single-run case. When a session is resumed rather than started fresh, that event doesn't
fire again. Usage from the resumed part of the session will go uncounted.

The impact depends entirely on how often actual users resume sessions. Fixing this fully also needs a change on backend side (deduplication logic), not just the plugin.

## OpenCode

We don't run OpenCode ourselves, and have never tested its coverage under real usage. We have no basis for judging how complete or incomplete it is.

## What this means for the numbers

Every total in this dashboard is a lower bound, not an exact figure. Some usage is definitely missing, we just don't know how much. We know some of the gaps for Claude and Copilot,
and we assume there are more. As for OpenCode, we have absolutely no idea.