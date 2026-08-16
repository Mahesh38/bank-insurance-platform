# 07 — Security & Compliance Canon

**Owners:** Deepali — Principal Security Architect (Board 4, veto) · Shailja S — Compliance &
Risk Head (Board 6, veto)
**Both hold block authority no aggregate overrides. Deadlock resolution:
[PERSONA-AUTHORITY-MATRIX §15.1](../governance/PERSONA-AUTHORITY-MATRIX.md).**

---

## 1. The division that prevents most arguments

| | Deepali (Security) | Shailja (Compliance) |
|---|---|---|
| Owns | The **security property** and residual risk | The **regulatory outcome** and evidence obligation |
| Says | "This must fail closed, with these controls" | "This is permissible only if consent is captured this way" |
| Never says | What the regulation means | Which cryptographic mechanism to use |

Most apparent Security ⊥ Compliance conflicts are a required *outcome* mistaken for a required
*implementation*. Separate the two first; the majority dissolve.

---

## 2. Security activities by stage

| Stage | Security activity | Output | Gate-bearing |
|---|---|---|---|
| S02 | Data classification, PII inventory, regulatory security obligations | Classification matrix | ✅ |
| S05 | Authentication and consent UX review; disclosure wording | UX security review note | ✅ |
| S06 | Data ownership and access-scope model | Ownership matrix | — |
| S07 | **Threat model** per trust boundary (STRIDE); crypto and key standard; network topology | Threat model, crypto standard | ✅ |
| S08 | SAST, SCA, secret scanning in CI; secure coding standard; SBOM | Pipeline security gates | ✅ |
| S09 | Landing zone security, KMS hierarchy, secrets management, network segmentation | IaC security baseline | ✅ |
| S10 | Third-party security assessment per integration; partner trust contract | Assessment record | ✅ |
| S11 | Security testing of the slice; authorization negative tests | Test evidence | ✅ |
| S12 | **Penetration test**; security regression; vulnerability remediation | Pentest report + remediation plan | ✅ **human sign-off** |
| S14 | Production security review; incident readiness | Go-live security sign-off | ✅ **human sign-off** |
| S15 | Continuous scanning, periodic re-assessment, threat-model refresh | Attestation | ✅ |

### Threat modelling cadence

Re-run when any of these is true — not on a calendar:

- a new trust boundary appears (a new service, a new partner, a new public endpoint);
- authentication or authorization logic changes;
- a new class of regulated data enters the system;
- network topology or public exposure changes;
- a security incident reveals an unmodelled path.

---

## 3. The non-negotiable controls for this platform

Derived from the IRDAI/RBI obligations in the
[business problem statement](../context/business-problem-statement.md). Each is **non-waivable**
and each needs an automated test (Rule QN-1).

| # | Control | Required behaviour | Test that proves it | Status |
|---|---|---|---|---|
| C1 | **Suitability hard-gate** | Quote endpoints return `403` without a valid suitability evaluation ID | Negative test: quote without suitability → 403 | 🔴 not implemented |
| C2 | **Consent evidence** | Append-only capture of statement text, version, CIF, OTP txn ID, timestamp, IP; retrievable for 7 years | Capture test + retrieval-after-retention test | 🔴 not implemented |
| C3 | **Attribution** | `distributorId` and SP licence ID injected server-side; caller-supplied values rejected | Injection test: caller sends `distributorId` → rejected | 🟡 partial in adapter |
| C4 | **Payment device isolation** | Premium payment executes only on the customer's device; never on an RM device | Journey test: RM-initiated payment → link to customer device only | 🔴 not implemented |
| C5 | **PII masking** | No PAN, Aadhaar, phone, email or health field in any log at any level | Log-scan test across all levels | 🟡 converter exists, unproven |
| C6 | **Data residency** | All data, backups, logs and archives in AWS India regions | IaC assertion + region attestation | 🔴 unverified |
| C7 | **7-year retention** | Raw payloads and audit logs retained with immutability (S3 Object Lock) | Retention config test + immutability test | 🔴 not implemented |
| C8 | **Audit completeness** | Every regulated action emits an attributable, immutable event | Per-journey audit-completeness test | 🟡 partial |
| C9 | **Maker-checker** | Bulk and privileged changes require a second authorised party | Negative test: self-approval → rejected | 🟡 WS-2 scope |
| C10 | **Encryption** | TLS 1.3 in transit; KMS CMK AES-256 at rest | Config test + scan | 🟡 partial |

> **Rule SC-1 — C1 through C10 are release-blocking at every stage from S11 onward.** A waiver on
> any of them is a legal decision, not a delivery decision, and belongs to Shailja's accountable
> human — never to Product, Architecture, Delivery, or an agent.

---

## 4. Security gates in the pipeline (S08 deliverable)

| Gate | Tool class | Fails build on | When |
|---|---|---|---|
| Secret scanning | Pre-commit + CI | Any detected credential | Every commit |
| SAST | Static analysis | New critical or high finding | Every PR |
| SCA / dependency | Vulnerability DB | Critical or high with a reachable path | Every PR |
| Container image scan | Image scanner | Critical OS or library CVE | Every image build |
| IaC scan | Policy-as-code | Public exposure, unencrypted store, over-broad IAM | Every IaC PR |
| Licence compliance | SBOM | Disallowed licence | Every build |
| DAST | Dynamic scan | New high finding | Nightly against dev |

**Severity: Deepali's `S0`–`S3` is security severity and never replaces AIGEM `P1`–`P5`.**

| S | Meaning | Remediation window |
|---|---|---|
| S0 | Critical, non-bypassable | Immediate; blocks release |
| S1 | High | Before the next release |
| S2 | Medium | Within two releases |
| S3 | Low / hardening | Backlog |

---

## 5. Compliance evidence model

The question Board 6 asks is *"can we defend this to a regulator?"* — and a regulator asks for
artefacts, not descriptions.

For each obligation in the regulatory registry:

```yaml
obligation:
  id: OBL-IRDAI-007
  source: "IRDAI corporate agency guidance — suitability before recommendation"
  requirement: "Product recommendation must be preceded by documented suitability analysis"
  control: C1 — suitability hard-gate
  implementation: "Quote API rejects requests without a valid suitability evaluation ID"
  evidence:
    - type: automated_test
      ref: "SuitabilityGateTest#quoteWithoutSuitabilityIsForbidden"
    - type: audit_sample
      ref: "quarterly log sample review, signed"
  owner: Shailja
  review_cadence: quarterly
  status: NOT_IMPLEMENTED
```

> **Rule SC-2 — An obligation with no named control, and a control with no automated evidence,
> are the same defect wearing different clothes.**

### Traceability chain

The chain a regulator will walk, end to end:

```
Regulation → Obligation → Control → Requirement → Story → Code → Test → CI run → Audit event
```

Any break in that chain is an audit finding. Today the chain breaks at **Control → Requirement**
for C1, C2, C4, C6 and C7, and at **Test → CI run** for everything, because there is no CI.

---

## 6. Risk acceptance

| Risk level | Who may accept |
|---|---|
| S3 / low | Deepali |
| S2 / medium, no regulatory impact | Deepali + Mahesh |
| S1 / high | Deepali's accountable human |
| S0 / critical, or any regulatory impact | **Accountable human risk owner only** — never a persona, never an agent, never by aggregate |

Every acceptance records: the risk, the business rationale, compensating controls, the named human
owner, an expiry date, and the remediation backlog ID. No open-ended acceptances.

---

## 7. Incident readiness

Required before S14 sign-off:

- [ ] Incident classification with a **security** dimension separate from availability
- [ ] Named containment authority and escalation path
- [ ] Evidence preservation procedure — logs and audit records before remediation
- [ ] Regulatory reporting decision tree with statutory timelines
- [ ] Customer notification criteria and templates
- [ ] Credential and key emergency revocation procedure, **exercised**
- [ ] Partner/1SB escalation contact with an out-of-hours path

> Availability must never outrank financial correctness. When payment state is uncertain during
> an incident, contain retry amplification and preserve evidence first; restoring throughput on
> an unreconciled money path turns an outage into a financial defect.
