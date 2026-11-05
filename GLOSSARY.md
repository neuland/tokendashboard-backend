# Tokendashboard

Collects token usage reported by the provider plugins (Claude, Copilot, OpenCode), prices it, estimates its CO₂, and shows the aggregates in the dashboard. One language for backend and frontend. The Claude plugin keeps its own glossary in its repository.

## Language

### Identity

**User**:
An anonymous plugin installation, identified by the plugin's random user ID and counted only to report active installations per provider. Not a person.
_Avoid_: Person

### Usage

**Usage Record**:
One reported piece of token usage for one model, the unit the backend stores and prices. What a single record covers is decided by each provider's plugin.
_Avoid_: Prompt, entry, event

**Session**:
The provider-side working session a usage record was reported under.
_Avoid_: Conversation, chat

**Cache read tokens**:
Input tokens served from the provider's prompt cache instead of being processed again.
_Avoid_: Cached tokens, cache hits

**Cache write tokens**:
Input tokens written to the provider's prompt cache for reuse, each with a cache lifetime where the provider reports one.
_Avoid_: Cache creation tokens

**Cache lifetime**:
How long a cache write stays reusable (5 minutes or 1 hour) and so which rate it is billed at. Claude cache writes reported without a lifetime are billed at the 1-hour rate.
_Avoid_: TTL, cache duration
