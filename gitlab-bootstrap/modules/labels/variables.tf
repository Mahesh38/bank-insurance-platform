variable "group_id" { type = string }
variable "labels" {
  type = list(object({
    name        = string
    color       = string
    description = optional(string, "")
  }))
}
