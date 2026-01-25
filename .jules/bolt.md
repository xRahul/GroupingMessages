## 2026-01-25 - Reflection Caching vs Algorithm Cost
**Learning:** Caching reflective method calls for `StringMetric` (SimMetrics library) provided negligible performance improvement (<1%).
**Reason:** The cost of the Levenshtein distance calculation itself (O(N*M)) vastly dominates the cost of reflection (O(1) overhead). Optimizing the reflection call is micro-optimization in this context.
**Action:** Always profile the "work" being done before optimizing the "access" to that work.

## 2026-01-25 - Regex vs Manual Iteration
**Learning:** Replacing multiple `String.replaceAll` and `split` calls with a single-pass manual character iteration reduced execution time by ~4x (1420ms -> 350ms) and significantly reduced garbage generation.
**Action:** For hot-path string cleaning, prefer manual char iteration over regex chains.
