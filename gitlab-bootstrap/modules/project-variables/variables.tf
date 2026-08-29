variable "project_id" { type = string }
variable "variables" {
  type = list(object({
    key               = string
    value             = string
    protected         = optional(bool, false)
    environment_scope = optional(string, "*")
    secret            = optional(bool, false)
  }))
  default = []
}
