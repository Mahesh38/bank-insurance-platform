variable "name" {
  description = "Project name."
  type        = string
}

variable "path" {
  description = "URL path segment."
  type        = string
}

variable "namespace_id" {
  description = "Numeric id of the owning group."
  type        = number
}

variable "description" {
  description = "Project description."
  type        = string
  default     = ""
}

variable "initialize_with_readme" {
  description = <<-EOT
    IMP-11 #1. MUST be false for any project receiving migrated history — a
    generated README commit forks the history before the mirror lands.
  EOT
  type        = bool
  default     = false
}

variable "set_default_branch" {
  description = <<-EOT
    IMP-11 #2. False for projects receiving migrated history: baseline section 7
    forbids defaulting or protecting a branch before it exists, and Terraform will
    protect a non-existent branch and then drift on every plan.
  EOT
  type        = bool
  default     = true
}

variable "default_branch" {
  description = "Default branch name, applied only when set_default_branch is true."
  type        = string
  default     = "main"
}

variable "container_registry" {
  description = "Enable the Container Registry. Confirmed available on the bank instance 2026-08-29."
  type        = bool
  default     = false
}

variable "packages" {
  description = "Enable the Package Registry. ASM-017 second half — availability unconfirmed."
  type        = bool
  default     = false
}

variable "pipeline_must_succeed" {
  description = <<-EOT
    S08-G2 mechanism. Available in CE, so it is never gated behind an edition flag.
    IMP-4: CE has no named-required-check analogue, so the gate is one required
    pipeline with every gating job allow_failure:false, and this setting is what
    makes it real.
  EOT
  type        = bool
  default     = true
}
