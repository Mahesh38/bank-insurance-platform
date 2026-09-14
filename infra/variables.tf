variable "aws_region" {
  type        = string
  description = "Primary AWS region. Must be an India region (ap-south-1 or ap-south-2)."
  default     = "ap-south-1"

  validation {
    condition     = contains(["ap-south-1", "ap-south-2"], var.aws_region)
    error_message = "S09-G9 / residency: aws_region must be ap-south-1 (primary) or ap-south-2 (DR)."
  }
}

variable "environment" {
  type        = string
  description = "Environment name: dev | uat | prod"
  validation {
    condition     = contains(["dev", "uat", "prod"], var.environment)
    error_message = "environment must be one of: dev, uat, prod."
  }
}
