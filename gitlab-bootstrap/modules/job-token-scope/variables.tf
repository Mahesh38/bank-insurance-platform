variable "target_project_id" {
  description = "Project whose job-token scope is being widened."
  type        = string
}
variable "allowed_project_ids" {
  description = "Projects permitted to reach the target with their CI job token."
  type        = list(string)
  default     = []
}
