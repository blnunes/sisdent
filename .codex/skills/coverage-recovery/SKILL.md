---
name: coverage-recovery
description: Recover a failing Sonar or coverage gate efficiently by measuring the deficit and targeting high-impact untested production paths before re-running remote analysis.
---

# Coverage recovery

Use this skill whenever a quality gate fails because coverage is below its required threshold.

Do not make one small test change and immediately rerun Sonar. First measure the gap to the gate and inspect the local coverage report. Rank production classes and components by coverage percentage, then by uncovered executable lines; prioritize low-coverage files with enough executable lines for a meaningful gain, especially when they are in the changed scope. Choose a coherent test batch whose expected covered lines comfortably exceeds the gap; target at least twice the required gain where practical.

When a low-coverage class or component is selected, inspect its uncovered branches and write focused tests around its public behavior. Cover meaningful success, validation/error, and relevant boundary paths. Do not write tests only to execute lines; keep assertions tied to observable behavior and avoid exclusions, suppressions, or threshold reductions.

Run local coverage after each coherent batch. If its expected gain is marginal, expand the batch before another remote Sonar analysis. For an 80% Sonar gate, target at least 81% local coverage in the affected scope.

If Sonar differs from local coverage, inspect its exact metric and changed-file/new-code scope before changing tests again. A sub-1-point gain that cannot clear the deficit is diagnostic information, not a successful recovery iteration.
