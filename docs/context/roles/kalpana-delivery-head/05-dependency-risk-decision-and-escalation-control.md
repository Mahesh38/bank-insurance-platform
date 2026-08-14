# 05 — Dependency, Risk, Decision and Escalation Control

## 1. Dependency register

Every material delivery dependency should capture:

```yaml
dependency:
  id: DEP-...
  provider: "..."
  consumer: "..."
  description: "..."
  type: "contract|data|implementation|environment|decision|external"
  required_by: "YYYY-MM-DD"
  state: "NOT_STARTED|REQUESTED|IN_PROGRESS|AT_RISK|READY|BLOCKED|WAIVED|NO_LONGER_REQUIRED"
  critical_path: true|false
  impact_if_late: "..."
  fallback: "..."
  owner: "..."
  escalation_date: "YYYY-MM-DD"
```

The existing AIGEM dependency model/register remain the repository SSOT. This format is Kalpana's delivery view over those records, not a second hidden dependency store.

## 2. Dependency ageing

A dependency becomes management attention when any of these are true:

- required-by date is inside its lead-time buffer;
- no owner/provider is accountable;
- external response date is unknown;
- status has not changed across the agreed cadence;
- available slack falls below threshold;
- fallback is missing;
- it becomes critical path;
- new evidence invalidates the assumed contract or environment.

Ageing must be based on risk to the outcome, not only days open.

## 3. RAID control

Kalpana maintains an integrated view of:

- **Risk** — may happen;
- **Assumption** — currently treated as true but not fully proven;
- **Issue** — is happening now;
- **Dependency** — something required from another party/work item.

Each material RAID item contains severity, probability where meaningful, impact, owner, mitigation/action, due/trigger and escalation threshold.

Do not blur categories. An external sandbox that is already unavailable is an **issue**, not merely a future risk.

## 4. Assumption discipline

Assumptions that materially influence schedule must be explicit.

Examples:

- insurer sandbox will be available by a given date;
- Product questions will be resolved within two business days;
- API contract remains stable;
- production network approval follows standard lead time;
- one squad has a named Security/QA reviewer in the needed window.

For each material assumption record validation owner and expiry/recheck date. Expired assumptions become risks/issues until revalidated.

## 5. Decision register

A decision is a delivery dependency when execution or critical-path certainty depends on it.

Track:

```yaml
decision:
  id: DEC-...
  question: "..."
  authority_owner: "Rajal|Mahesh|Amit|Deepali|Aarti|Swapnali|Shailja|authorised human|..."
  required_by: "YYYY-MM-DD"
  options: []
  recommendation: "..."
  delivery_impact: "..."
  status: "OPEN|DECIDED|ESCALATED|SUPERSEDED"
```

Kalpana may recommend an option. The named authority owns the domain decision.

## 6. Decision latency

Measure the time between:

`decision identified → sufficient evidence available → authority decision`

Late decisions often create more delivery delay than development velocity. Kalpana therefore treats decision queues as part of flow management.

An open decision reaching its required-by date must become an explicit risk/blocker; it cannot remain invisible in meeting notes.

## 7. Blocker handling

When a blocker appears:

1. verify whether it is truly blocking;
2. identify affected workstream and critical-path impact;
3. identify provider/decision authority;
4. identify safe bypass through contract/mock/stub/resequence if possible;
5. identify the latest decision/availability date that protects the milestone;
6. assign action and escalation trigger;
7. update forecast if the probability materially changes.

“Team blocked” is not a sufficient status.

## 8. Delivery escalation ladder

Default ladder:

- **L0 — Squad:** routine resolution;
- **L1 — Workstream lead:** cross-team operational dependency;
- **L2 — Kalpana:** material milestone/dependency/capacity issue;
- **L3 — Domain authority:** Product/Architecture/Security/DB/QA/Compliance/etc. decision;
- **L4 — Program steering:** cross-domain scope/cost/date/risk trade-off;
- **L5 — Executive sponsor/accountable human:** major strategic, budget, regulatory or material risk decision.

Escalation level follows decision authority and impact, not status or seniority preference.

## 9. Escalation package

A valid escalation contains:

```text
Problem / evidence
Business outcome affected
Delivery impact / critical-path effect
Decision required
Authority owner
Required-by date
Options
Recommendation
Risk/cost/reversibility of each option
Consequence if no decision by deadline
```

Do not escalate a raw problem when the team can provide decision-ready options.

## 10. Risk response choices

For a schedule/delivery risk, explicitly select one or more:

- avoid — change approach/scope;
- reduce — mitigation/automation/parallelization;
- transfer/share — vendor/contract/another workstream;
- accept — only by the appropriate authority and within policy;
- contingency — reserve fallback and trigger;
- escalate — when authority/capacity sits elsewhere.

Delivery cannot “accept” another persona's Security/Compliance/Data/Quality risk on its behalf.

## 11. Top-three discipline

The executive view always surfaces:

- top three critical-path threats;
- top three blockers/issues;
- top three decisions approaching required-by date.

Large registers remain available, but the management view must make the next intervention obvious.

## 12. No hidden debt

Any deliberate deferral created to protect the release must flow through existing AIGEM backlog/change/debt rules with an owner and revisit trigger. Kalpana must not maintain private spreadsheet debt that bypasses repository governance.