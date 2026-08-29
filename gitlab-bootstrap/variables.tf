variable "gitlab_base_url" {
  description = "Base URL of the GitLab instance, including the /api/v4 suffix the provider expects."
  type        = string
  default     = "https://gitlab-ce.au.bank.in/api/v4"

  validation {
    condition     = can(regex("^https://", var.gitlab_base_url))
    error_message = "gitlab_base_url must be https. Plain HTTP is never acceptable for the control plane."
  }
}

variable "gitlab_edition" {
  description = <<-EOT
    Licence tier of the target instance. Drives the capability flags in locals.tf.

    Confirmed 2026-08-29: the bank instance is Community Edition v19.1.2 (ASM-012).
    CE cannot enforce required merge-request approvals, CODEOWNERS approval, or
    protected environments — see CR-016.

    Per C-ARC-6 the modules still MODEL those controls; this flag decides whether
    they are APPLIED. An upgrade is a flag change, not a redesign.
  EOT
  type        = string
  default     = "ce"

  validation {
    condition     = contains(["ce", "premium", "ultimate"], var.gitlab_edition)
    error_message = "gitlab_edition must be one of: ce, premium, ultimate."
  }
}

variable "parent_group_id" {
  description = <<-EOT
    Numeric id of the EXISTING top-level `insurance` group. Confirmed by the bank
    on 2026-08-29 as 820 (ASM-013).

    This group is a prerequisite, never a managed resource. IMP-11 #3: import the
    parent, never create it. Nothing in this configuration is safe if the parent
    group appears under `create` in a plan.
  EOT
  type        = number
  default     = 820
}

variable "platform_governance_enabled" {
  description = <<-EOT
    Whether to create the ninth project, governance/platform-governance.

    Approval condition AC-2: the bank's GitLab/architecture authority must accept
    the Appendix C exception IN WRITING before M4.3 creates it. Until that
    acceptance is recorded, M4.3 creates EIGHT projects, not nine.

    Leave false. Flip only when the written acceptance exists (ASM-021).
  EOT
  type        = bool
  default     = false
}

variable "default_branch" {
  description = <<-EOT
    Default branch name for seeded projects.

    NOT set on projects that receive migrated history: baseline section 7 forbids
    protecting or defaulting a branch before it exists, and IMP-11 #2 records that
    Terraform will happily protect a non-existent branch and then drift forever.
  EOT
  type        = string
  default     = "main"
}
