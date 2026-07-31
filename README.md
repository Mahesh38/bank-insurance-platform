# one-silver-bullet

Integration knowledge base for building a bank insurance platform on **1Silverbullet (1SB)** middleware, with an architecture that remains replaceable by an in-house aggregator later.

## Start here

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
