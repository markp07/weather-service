# AGENTS

## Mission and Priorities
Priority order is strict and deterministic:
1. security
2. correctness
3. compatibility
4. clarity
5. speed

## Hard Rules
- Never commit secrets or private keys.
- Never log tokens or credentials.
- Deny access to secret environment files: `.env`, `.env.local`, `.env.*`.
- Allow access only to `.env.example` for environment template usage.
- Do not weaken authentication or security defaults.
- Do not edit generated or build outputs:
  - `**/target/**`
  - `**/.next/**`
  - `**/coverage/**`

## Branching Rules
- Allowed branch prefixes: `major/*`, `feature/*`, `fix/*`
- Branch name regex: `^(major|feature|fix)\/[a-z0-9][a-z0-9-]*$`
- Release intent mapping:
  - `major/*` -> `major`
  - `feature/*` -> `minor`
  - `fix/*` -> `patch`
- Optional backward-compatibility note:
  - `bugfix/*` branches may include an explicit compatibility note in PR description when used by external tooling.

## Commit Message Framework
- Conventional Commits are required.
- Allowed types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert`
- Header regex:
  - `^(feat|fix|docs|refactor|test|chore|build|ci|perf|revert)(\([a-z0-9-]+\))?(!)?: [^\s].*[^\.]$`
- Breaking changes must include footer:
  - `BREAKING CHANGE:`

## Machine-Readable Policy Authority
- `agents.schema.json` is normative for automation.
- `agents.policy.json` must validate against `agents.schema.json`.
- If prose conflicts with schema, follow schema and report the conflict.

## Versioning Rules
- Semantic Versioning format is required: `MAJOR.MINOR.PATCH`
- Canonical example: `1.0.0`
- Version regex: `^[0-9]+\.[0-9]+\.[0-9]+$`

## Agent Response Contract
All agent responses must include sections in this exact order:
1. Plan
2. Changes
3. Validation
4. Risks/Assumptions
5. Next Steps
