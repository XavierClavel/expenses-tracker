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
