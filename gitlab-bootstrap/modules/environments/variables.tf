variable "project_id" {
  description = "Project the environments belong to."
  type        = string
}

variable "environments" {
  description = "Environment model, baseline section 9.3."
  type = list(object({
    name               = string
    deployment         = string
    intent             = string
    protected          = bool
    approvals_required = number
  }))
}

variable "capabilities" {
  description = "Capability flags from the root locals (C-ARC-6)."
  type = object({
    protected_environments = bool
  })
}
