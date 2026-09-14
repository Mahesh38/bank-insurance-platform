# S08-G9 — Pipeline feedback & flake rate

**Criterion:** Pipeline feedback under 10 minutes at p95; flaky failure rate under 1%.  
**Owner:** Shivanshi / SRE · **Verifier:** `ci` · **Evidence level:** E4

## How to re-measure

```bash
# Live refresh (needs `gh` auth):
python3 scripts/governance/measure-pipeline-feedback.py --fetch --write --assert

# Offline re-check of the committed snapshot (CI):
python3 scripts/governance/measure-pipeline-feedback.py \
  --from-file scripts/governance/evidence/S08-G9-pipeline-feedback.json --assert
```

## Snapshot (committed JSON)

See [`S08-G9-pipeline-feedback.json`](./S08-G9-pipeline-feedback.json).

| Check | Requirement | Measured |
|---|---|---|
| S08-VT-08 PR feedback p95 | < 10 min across ≥ 20 runs | **~2.8 min** (gate uses max of PR-20 / all-PR / all-concluded p95) |
| S08-VT-09 flake rate | < 1% across ≥ 50 runs | **0.00%** (0 flaky failures / 50 most recent concluded) |
| S08-G9 sample | ≥ 50 runs | **351** concluded (`success`/`failure`) of **401** API total |

**Flake definition:** a `failure` on a `head_sha` that also has ≥1 `success` in the sample (same commit green and red). Legitimate red builds on broken commits are not flakes.

**Workflow:** `.github/workflows/application-ci.yml` (`Application CI`).
