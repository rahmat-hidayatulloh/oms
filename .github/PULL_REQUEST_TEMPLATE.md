# Pull Request

## Description

<!-- Brief description of the changes in this PR -->

## Type of Change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update
- [ ] CI/CD or infrastructure change

## Checklist

### Code Quality
- [ ] Code follows project conventions and style (Checkstyle 0 violations)
- [ ] No new SpotBugs findings
- [ ] No `@Autowired` field injection (constructor injection only)
- [ ] No TODO/FIXME/HACK left in code
- [ ] New code has Javadoc where applicable

### Testing
- [ ] Unit tests added/updated for new/changed code
- [ ] `mvnw clean verify` passes locally
- [ ] Integration tests pass (if Docker available)

### Security
- [ ] No hardcoded secrets or credentials
- [ ] Input validation added for new endpoints
- [ ] CORS/CSRF not weakened
- [ ] No new dependency vulnerabilities introduced

### Documentation
- [ ] README updated (if applicable)
- [ ] API documentation updated (if applicable)
- [ ] ADR created (if architectural decision)
- [ ] `docs/current/**` synced for any current-state code change (see `docs/optimization-workflow.md` step 5), or N/A
- [ ] `docs/index.md` updated if any doc was added/moved/renamed
- [ ] Superseded docs moved to `docs/history/` with a `Superseded by` banner
- [ ] Docs governance check passes (`python3 scripts/check-docs.py`)

## Related Issues

<!-- Link related issues: Closes #123, Fixes #456 -->

## Testing Evidence

<!-- Paste `mvnw clean verify` output or CI run link -->

## Screenshots (if applicable)

<!-- Add screenshots for UI changes -->
