terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configured per environment via -backend-config=...
  # See bootstrap/README.md for the state bucket/lock table bootstrap.
  backend "s3" {}
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project           = "bank-insurance-platform"
      Workstream        = "WS-3"
      Stage             = "S09"
      ManagedBy         = "terraform"
      DataClassification = "platform"
    }
  }
}
