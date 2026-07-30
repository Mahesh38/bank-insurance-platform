# Email draft — Product team → 1SB (onboarding / env access)

**From:** Bank Product / Bancassurance  
**To:** 1SB Relationship Manager / Onboarding  
**Cc:** Bank Engineering Lead, Platform/Security (optional)  
**Subject:** Request — Dedicated API access & Life product matrix for distributor BCIBL (Dev / UAT / Prod)

---

Hi Team,

We are onboarding **distributor ID `BCIBL`** to integrate with the 1Silverbullet Insurance Gateway for our bank’s Life insurance distribution (RM-assisted / B2B).

We already have the following from your side / our setup discussions:

- Demo API base URL: `https://demo.api.1silverbullet.tech`
- Distributor ID: `BCIBL`
- Channel defaults: Sales Channel `Online`, Channel Type `B2B`
- Agent IDs (please confirm applicability): `109337`, Alternate `8925`
- Sample product: Insurer `ICICI` / Manufacturer ID `ICICI` / Product Code `E38` / GIFT Select / Product Type `LifeSave` / `nonParticipating`

To start and complete integration cleanly across **Dev, UAT, and Production**, please share / arrange the following.

### 1) Dedicated credentials (per environment)

Please create **API Key + API Secret dedicated to our partner (BCIBL)** — we do not want to continue on another partner’s UAT credentials long-term.

| Environment | API Key | API Secret | Notes |
|-------------|---------|------------|-------|
| Dev / Demo  | &lt;please issue&gt; | &lt;please issue&gt; | May align with demo host |
| UAT         | &lt;please issue&gt; | &lt;please issue&gt; | Required for our UAT |
| Production  | &lt;please issue&gt; | &lt;please issue&gt; | Required before go-live |

Please confirm the correct **base URL** for each environment if different from the demo host above.

### 2) Life product & insurer matrix (multi-product)

Our LOB focus is **Life Insurance**, with subtypes **Term** and **Saving** (more Life subtypes later as needed).

Please provide the **full list of insurers and products enabled for BCIBL** in each environment (Excel/CSV is fine), including at minimum:

- Insurer code / Insurer name  
- Manufacturer ID  
- Product code / Product name  
- Product type (e.g. LifeTerm, LifeSave, …)  
- Subtype / plan flags (e.g. participating / nonParticipating, ULIP indicators if any)  
- Environment (Demo / UAT / Prod)  
- Any restrictions (Single Quote only, BI flags, etc.)

We already have **ICICI / E38 / GIFT Select (LifeSave)**. Please also enable / list at least one **LifeTerm** product for BCIBL so we can cover both Life subtypes.

### 3) Distribution / agent validation

Please confirm:

- Agent ID `109337` and Alternate Agent ID `8925` — valid for which insurers/environments?  
- Allowed values for Sales Channel, Channel Type, and Type of Sale (`assisted` / `nonassisted`) for B2B journeys  
- Whether `distributorId` for Production remains `BCIBL` or differs  

### 4) Network / security (Production)

For **UAT**, we understand IP whitelisting is **not** required.  
For **Production**, please share:

- IP whitelist onboarding process and lead time  
- Where to send our egress CIDRs  
- Confirmation format once whitelisting is complete  

### 5) Integration support artefacts

If available, please share:

- Latest API docs / changelog relevant to Life Term & Life Save  
- Sample request/response for: quote (single + multi), quote poll, proposal form, proposal submit, payment URL, application status  
- Postman collection (if any)  
- UAT/Prod support contact and escalation path  

### 6) Timeline ask

Please confirm:

1. ETA for **dedicated UAT credentials** for BCIBL  
2. ETA for **Life product matrix** (Term + Saving) for Demo/UAT  
3. ETA / process owner for **Prod credentials + IP whitelist**  

Happy to schedule a short working call with our engineering team if that helps unblock faster.

Thanks and regards,  
&lt;Product Owner Name&gt;  
&lt;Bank / Bancassurance&gt;  
&lt;Email / Phone&gt;  
Distributor ID: **BCIBL**

---

## Internal notes (do not paste into email)

- Partner UAT keys may be used briefly for spikes only; rotate when BCIBL keys arrive.  
- Secrets: local/dev via properties; UAT/Prod → AWS Secrets Manager when ready.  
- Inbound bank auth (JWT/mTLS) is internal — not part of 1SB ask.  
- Tracking board is internal.  
- After 1SB replies, update `config/catalog/products.example.yaml` and CONFIRM-01/02.
