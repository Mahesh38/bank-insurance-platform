variable "project_id" {
  description = "Project to govern."
  type        = string
}

variable "protection" {
  description = "Main-branch protection, baseline section 6.2."
  type = object({
    branch                       = string
    allow_force_push             = bool
    push_access_level            = string
    merge_access_level           = string
    code_owner_approval_required = bool
  })
}

variable "approval_rules" {
  description = "Risk-based approval rules, baseline section 6.3. Declared on every tier; applied only where capabilities allow."
  type = list(object({
    name               = string
    approvals_required = number
  }))
  default = []
}

variable "capabilities" {
  description = "Capability flags from the root locals. Drives what is applied versus declared (C-ARC-6)."
  type = object({
    merge_request_approval_rules = bool
    code_owner_approval          = bool
  })
}
