# Why we are building this

> **Teaching pack** · `SUG-20261005-cfp` · not a source of truth.
>
> **Confluence parent:** AU Bank Insurance Platform — Start here  
> **This page:** child #1

## The business problem

AU Small Finance Bank distributes Life, Health and General insurance under an IRDAI **Composite Corporate Agent** licence (CA0515). Today that distribution runs through the **AU Beema Portal**: the RM or customer starts in a bank screen, then is **redirected to the insurer**. From that moment the bank loses the sale.

That is not a UX complaint. It is a control failure:

- conversion, drop-off and underwriting outcomes are invisible
- payment and issuance cannot be reconciled in bank books in real time
- IRDAI attribution (Corporate Agent + Specified Person) is reconstructed after the fact
- the bank cannot change an aggregator later without rewriting the front door

## As-is vs what we are replacing it with

![As-is versus target](../diagrams/01-as-is-vs-target.png)

| | As-is (Beema redirect) | Target (this platform) |
|---|------------------------|-------------------------|
| Front door | Insurer website after a hop | AU Bank NIP-APP (Flutter), bank-branded |
| Suitability | Thin / optional in practice | **Hard gate** — no quote without it |
| Consent | Insurer-owned | Bank-owned, OTP on the **customer device**, immutable |
| Quotes | One insurer at a time, off-platform | Multi-insurer compare in-platform (Group A via 1SB) |
| Payment | Insurer checkout | AU Bank Payment Gateway, **customer's phone** |
| Sold | Guessed from proposal or payment | Issued + confirmed + **RECONCILED** + audit |
| Connectivity | Coupled to whoever the portal calls | Replaceable behind Integration Hub + adapter |

## Why the bank must own the journey

IRDAI corporate-agency rules require:

1. **Attribution** of every policy to the registered Corporate Agent and the certified Specified Person.
2. **Need analysis / suitability** before a product is offered.
3. **Consent** that can be reconstructed years later.
4. **RBI payment isolation** — insurance premium is not taken on a bank employee's device.

A redirect model cannot prove those four things. A bank-owned journey can.

## What "good" looks like for the programme

From the product vision ([`02-product-vision-and-outcomes.md`](../../../au-bank-insurance-platform/02-product-vision-and-outcomes.md)):

- The RM never leaves the AU Bank journey to complete a Life sale.
- The customer sees AU Bank branding and explicit OTP / payment steps.
- A second line of business reuses the **same** orchestration, not a fork.
- Changing 1Silverbullet (or adding a second aggregator later) does not rewrite NIP-APP.

## What this is not for

We are not rebuilding CBS/CIF, not building a claims system, not becoming an insurer, and not using 1SB's UI as the long-term front door.

**Next child:** [What we are building now (R0)](./02-what.md)

Source: [`docs/context/business-problem-statement.md`](../../../context/business-problem-statement.md) §1–§3.
