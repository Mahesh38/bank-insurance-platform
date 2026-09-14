# State backend bootstrap

One-time, human-operated creation of the Terraform state bucket and lock table in `ap-south-1`.

This bootstrap is intentionally **not** applied by CI. A human with account access runs it once,
then commits only the resulting backend config keys (never credentials).

## Creates

- S3 bucket `bip-tfstate-<account-id>-ap-south-1` — versioned, encrypted (SSE-KMS), public access blocked
- DynamoDB table `bip-tfstate-lock` — pay-per-request, for state locking

## After bootstrap

1. Copy `../backend.hcl.example` → `../envs/<env>/backend.hcl`
2. Fill bucket name
3. `terraform init -backend-config=envs/<env>/backend.hcl`
