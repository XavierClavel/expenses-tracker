# Project conventions

## Attribution

Never add yourself as a co-author. Do not append a `Co-Authored-By: Claude ...`
trailer to commit messages, and do not add Claude Code attribution footers to
commit messages or pull request bodies. Commits and pull requests in this
repository are authored by the human committer alone.

## Commits

Conventional Commits, with the module as the scope:

```
feat(backend): expose per-account latest-year interest amount
fix(app): fixed wrong graph values for transfers + interests variation %
chore(k8s): restructure manifests with kustomize
```

Scopes in use: `backend`, `app`, `k8s`. Omit the scope for repo-wide changes.

## Versioning

Every change to a module ships with a version bump for that module, in the same
commit or pull request:

- `backend/` → `version` in the root `build.gradle.kts`.
- `app/` → `versionName` **and** `versionCode` (+1) in `app/app/build.gradle.kts`.

A change touching both modules bumps both. Changes limited to `k8s/`, CI or docs
need no bump.

Pick the increment from the commit type: `fix`/`chore` → patch, `feat` → minor,
breaking API change → major.

CI deploys only when the version's tag doesn't exist yet (`v<version>` for the
backend, `android-v<versionName>-<versionCode>` for the app), so an unbumped
change merges but never ships.
