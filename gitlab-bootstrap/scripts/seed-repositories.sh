#!/usr/bin/env bash
# GLM-001 M5.4-M5.8 — seed the greenfield projects.
#
# Seeds structure only: directory skeletons, CI placeholders, CODEOWNERS
# placeholders, ignore files. Never content, never a generated README in a
# project that receives migrated history (IMP-11 #1).
set -euo pipefail
echo "Greenfield projects to seed (baseline sections 3.4-3.8):"
cat <<'PLAN'
  contracts          openapi/ asyncapi/ schemas/ compatibility-tests/ codegen/{flutter,java,node}/
  infrastructure     terraform/environments/{dev,sit,uat,preprod,prod,dr}/ terraform/modules/ policies/
  gitops             applications/ environments/{dev,sit,uat,preprod,prod,dr}/ clusters/{ap-south-1,ap-south-2}/ deployment-windows/
  ci-components      templates/ tests/
  security-policies  policies/ (from .gitleaks.toml + .trivyignore, re-expressed as policy-as-code)

  Every project also gets: .gitlab-ci.yml placeholder, CODEOWNERS placeholder
  (group-based per C-ARC-5), .gitignore.

  NOTE C-ARC-5: on CE a CODEOWNERS file has NO enforcement (CR-016). Seed it so
  ownership is recorded and an upgrade is a flag change — but never record
  section 6.4 as satisfied by its presence.
PLAN
