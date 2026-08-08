# one-silver-bullet

Integration knowledge base for building a bank insurance platform on **1Silverbullet (1SB)** middleware, with an architecture that remains replaceable by an in-house aggregator later.

## Start here

**On the team, or working with an AI agent on this repo?**
→ **[docs/governance/RUNBOOK.md](./docs/governance/RUNBOOK.md)** — the AIGEM operating manual.
Find your [role card](./docs/governance/RUNBOOK.md#6-role-cards) (one screen: what you do, how
often, what you own). Agents start at
[§8](./docs/governance/RUNBOOK.md#8-what-the-ai-agent-must-know-about-this-project).

**Triaging a new requirement, bug, or AI suggestion?**
→ **[docs/governance/README.md](./docs/governance/README.md)** — the governance model itself
(stage-fit and scope-fit triage, P1–P5 priority, dependency ordering, seven-board review gate,
drift control).

**Building workforce authentication and authorization?**
-> **[docs/authentication-authorization/README.md](./docs/authentication-authorization/README.md)**
(Phase 1 employee/insurer identity, token-hiding BFF, provider abstraction, Keycloak, RBAC + ABAC + relationship policies)

Local identity stack:

```bash
docker compose --env-file .env.identity -f docker-compose.identity.yml up --build
```

**Full-platform architecture review (AWS/Kubernetes microservices target state)?**
→ **[docs/architecture-review/README.md](./docs/architecture-review/README.md)**
(Service decomposition, sync/async communication, AWS/EKS infrastructure, data architecture, security/NFRs, delivery estimate)

**Building the 1SB integration service?**  
→ **[docs/1sb-insurance-integration/service-ssot/README.md](./docs/1sb-insurance-integration/service-ssot/README.md)**  
(PO + Architect decisions, product backlog, NFR/compliance map, link to full architecture)

**Full research pack:**  
→ [docs/1sb-insurance-integration/README.md](./docs/1sb-insurance-integration/README.md)

Includes:

- PO ↔ Architect design session & accepted decisions
- Product backlog with priorities and acceptance criteria
- Functional / NFR / compliance / shared-JAR map
- Full technical architecture (modules, APIs, data model, tests)
- 1SB API catalog and field guides
- Replaceable middleware rationale (Case 2: Service → LOB handler)

Upstream docs: https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api
