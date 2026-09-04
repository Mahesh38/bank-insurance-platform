variable "name" {
  description = "Group name."
  type        = string
}

variable "path" {
  description = "URL path segment."
  type        = string
}

variable "parent_id" {
  description = "Numeric id of the parent group."
  type        = number
}

variable "description" {
  description = "Group description."
  type        = string
  default     = ""
}

variable "visibility" {
  description = "Visibility level. Baseline section 14: every group in this estate is private."
  type        = string
  default     = "private"

  validation {
    condition     = var.visibility == "private"
    error_message = "Every group in this estate is private (baseline section 14 acceptance criteria)."
  }
}
