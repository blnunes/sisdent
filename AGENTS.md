# Quality requirements

- For every behaviour change, add or update focused automated tests in the same task. Exercise success, validation/error, and relevant boundary paths.
- Before reporting a Java change as complete, call the `quality-gate.verify_quality` MCP tool. Do not claim completion while it reports a failure.
- Before reporting any code change as complete, call the `formatting-gate.verify_formatting` MCP tool. Do not claim completion while it reports a failure.
- Keep the JaCoCo bundle threshold at or above 80% instruction coverage and 60% branch coverage. Improve coverage when touching an uncovered area; never lower these limits.
- When `SONAR_TOKEN` is available, run `quality-gate.verify_quality` with `runSonar: true` before completing backend work. Fix Sonar findings instead of suppressing rules, except for a documented false positive approved by the team.
- Do not exclude new production code from JaCoCo or Sonar just to satisfy a quality gate.
- Use the configured formatter for every changed source file: Prettier for TypeScript, HTML, SCSS, and JSON; Ruff for Python; and 4-space indentation with lines up to 120 characters for Java. Do not leave tabs, trailing whitespace, or long unwrapped expressions in generated code.

## Code Review Rules

- Flag changed production behaviour that lacks a focused automated test.
- Flag changes that lower coverage, add broad analysis exclusions, or introduce Sonar suppressions without a written justification.
- Flag source changes that have not passed the applicable formatting check.
