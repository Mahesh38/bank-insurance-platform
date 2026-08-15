# Story — S<xx>-E<nn>-S<nn> <Title>

```yaml
story:
  id: S08-E01-S02
  title: "Run unit and component tests on every pull request"
  epic: S08-E01
  stage: S08
  workstream: WS-3
  work_type: INFRA            # one of AIGEM's 16 canonical work types
  priority: P1                # AIGEM P1–P5 only — never a persona-local severity
  size: M                     # XS | S | M | L | XL (XL must be split or become a spike)
  owner_persona: "Amit / Engineering"
  status: READY
  traces_to: []               # requirement / obligation / gap / debt IDs
```

## 1. Story

> As a **<role>**, I need **<capability>**, so that **<outcome>**.

For technical enabler stories, drop the user-story form and state it directly. Forcing "As a
developer, I want a build pipeline" adds no information and obscures the actual dependency.

## 2. Acceptance criteria

Observable by someone who did not write them. Given/When/Then, or a checklist where that reads
more naturally.

```gherkin
AC1
  Given a pull request containing a failing unit test
  When the pipeline runs
  Then the pipeline fails and the merge is blocked

AC2
  Given a pull request with all tests passing
  When the pipeline runs
  Then a readable test report is published and the merge is permitted
```

- [ ] AC1
- [ ] AC2

**Every AC must be checkable without asking the author what they meant.** "The pipeline should be
fast" is not an AC; "PR feedback completes within 10 minutes at p95 over 20 runs" is.

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | | Amit |
| Component | | Amit |
| Integration | | Swapnali |
| E2E | | Swapnali |

Negative, boundary and error cases — not only the happy path:

- [ ]
- [ ]

## 4. Compliance and security impact

```yaml
security_impact: none           # none | low | medium | high
compliance_impact: none
controls_touched: []            # C1–C10 — any control touched means 100% branch coverage and a negative test
pii_involved: false
review_tier: T2
```

If a control from [`../07-SECURITY-COMPLIANCE-CANON.md §3`](../07-SECURITY-COMPLIANCE-CANON.md) is
touched, the story is at minimum T3, needs a negative test proving the control **blocks**, and
carries 100% branch coverage on the control path.

## 5. Definition of Ready

- [ ] Traces to a requirement, obligation, gap or debt ID
- [ ] Acceptance criteria observable and testable
- [ ] Security and compliance impact stated (`none` is a valid, deliberate answer)
- [ ] Dependencies identified and met or scheduled
- [ ] Test approach agreed at the right pyramid level
- [ ] Test data identified and PII-free
- [ ] Sized (not XL)

## 6. Definition of Done

- [ ] All AC demonstrably met, evidence linked
- [ ] Tests written at the agreed levels and **green in CI**
- [ ] Coverage thresholds held
- [ ] Static analysis and dependency scan clean, or findings triaged with IDs
- [ ] ArchUnit and boundary rules green
- [ ] Observability added: metrics, structured logs, correlation ID propagation
- [ ] No PII in logs — proven by test
- [ ] Audit events emitted where the action is regulated
- [ ] Documentation updated where behaviour changed
- [ ] Review-board conditions recorded and closed
- [ ] Demonstrable to the PO

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| | | |

## 8. Notes

Decisions taken during implementation, alternatives rejected, and anything the next person to
touch this needs. Not a changelog — the diff is the changelog.
