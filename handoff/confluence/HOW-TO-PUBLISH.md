# How to publish the Lead Management page on Confluence

This folder is a **client / vendor hand-out**. It is written so you can copy it into Confluence and share it with bank developers and vendor developers who do not have (and should not need) any other internal documentation.

## What to publish

Publish **one** Confluence page from:

`Lead-Management-Build-Specification.md`

That file is the complete build specification: product behaviour, screens, flows, high-level design, low-level API contract, and the modules that sit before and after Lead.

Do **not** attach or link any other project documentation when you share this page.

## Suggested Confluence settings

| Setting | Value |
|---|---|
| Space | Bank Insurance Distribution (or the client project space) |
| Title | Digital Insurance Platform — Lead Management Build Specification |
| Labels | `lead`, `workforce-app`, `api`, `hld`, `lld` |
| Restrictions | Share with bank development, vendor development, and client product/QA. Do not grant access to any other internal knowledge base. |
| Parent | A new “Build specifications” parent page. Keep this page self-contained. |

## Paste steps (Confluence Cloud)

1. Create a blank page.
2. Use **Insert → Markup → Markdown** (or paste into the editor if your space auto-converts Markdown).
3. If mermaid diagrams do not render, insert each ` ```mermaid ` block with **Insert → Mermaid diagram** (or keep the ASCII versions that sit next to them).
4. Set the page title exactly as above.
5. Publish.

## What recipients can do with this page

They can start building the workforce application and the Lead APIs end to end: login, customer search, Save / resume, Start Onboarding, certified-SP assignment, dashboard, then the hand-off into suitability, quote, proposal, payment and issuance.

Visual layout may follow the client prototype:

https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&page-id=208%3A2982

If the prototype and this page disagree, **this page wins**.
