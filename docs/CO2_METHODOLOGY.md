# CO₂ Methodology

## Derivation of CO₂ factors for Claude

The CO₂ factors used in this project are based on the study *How Hungry is AI? Benchmarking Energy, Water, and Carbon Footprint of LLM Inference* by Jegham et al. The study does
not directly measure the electricity consumption of commercial large language models. Instead, it estimates the energy consumption of standardised inference requests by combining
measured response times with an infrastructure model. This model incorporates publicly available information about data centres together with assumptions regarding the underlying
hardware, system utilisation, and request batching.

For our internal order-of-magnitude estimate, we use a factor of **840 g CO₂e per million output-equivalent tokens**.

This factor is derived from the study's long-context benchmark for Claude 3.7 Sonnet, consisting of 10,000 input tokens and 1,500 output tokens. For this scenario, Jegham et al.
estimate an energy consumption of 5.671 Wh. Using the emission factor of 0.287 kg CO₂e/kWh applied in the study, this corresponds to approximately 1.628 g CO₂e per request.

For our simplified token model, we weight one input token as one-twentieth of an output token. Under this assumption, the reference scenario corresponds to 2,000 output-equivalent
tokens, yielding approximately 814 g CO₂e per million output-equivalent tokens. Taking into account the uncertainty reported by Jegham et al. for the estimated energy consumption,
this corresponds to a range of approximately 771 to 857 g CO₂e per million output-equivalent tokens. For internal reporting, we round up to 840 g CO₂e per million output-equivalent
tokens, toward the upper end of that range.

The study neither reports nor models prompt-caching activities separately. Since our agentic coding workloads frequently reuse large repository contexts, we additionally account
for the cache-write and cache-read tokens reported by the CLI tools. These token categories are converted into output-equivalent tokens using our own approximation factors.
Cache-write tokens start from the pricing ratio and are weighted at 1.25 times the input-token factor, matching the API's pricing. For cache-read tokens we use a much lower factor
than the API's pricing ratio would suggest:
the API prices cache reads at 10% of the input-token cost, we use 1% of the input factor instead. This deviation is a deliberate choice, not a measurement: a cache read is
computationally closer to a memory lookup than to a full forward pass, and prices primarily reflect the provider's cost structure, not directly the energy consumed. We don't know
how large the actual difference is - 1% is a rough approximation, not a solid derivation. The real value could just as well be 5%, 10%, or 20%; we have no basis for narrowing it
down. For models other than Sonnet, we estimate relative CO₂ factors using the pricing ratios of the Claude API. This includes models such as Claude Opus, Claude Haiku, and Claude
Fable. We deliberately use a consistent pricing-based scaling instead of adopting the published estimates for individual models directly.

These values are approximations, not measurements. We have no measurement data of our own on the energy consumption of individual requests - providers don't publish that - and the
underlying study itself only estimates, with its own uncertainty depending on load, data centre, and model optimisations. Several assumptions in this derivation are choices we made
ourselves on top of that: the weighting of input against output tokens, the transfer to other models via pricing ratios, and in particular the weighting of cache-read tokens. For
none of these assumptions can we say whether it over- or underestimates actual consumption, let alone by how much. The factors presented here are intended for rough
order-of-magnitude orientation and comparison, not for formal greenhouse gas accounting.

### GPT / Copilot

No CO₂ factors for GPT models. Token counting infrastructure exists; CO₂ figures need to be derived and added separately.

### OpenCode

No CO₂ factors yet. CO₂ figures need to be derived and added separately.