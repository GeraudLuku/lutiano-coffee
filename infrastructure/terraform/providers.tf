terraform {
  required_version = ">= 1.6"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 6.0"
    }
  }

  backend "s3" {
    bucket = "lutiano-bucket-20261124-1711"
    key    = "lutiano/terraform.tfstate"
    region = "af-casablanca-1"
    endpoints = {
      s3 = "https://axtejniknqcl.compat.objectstorage.af-casablanca-1.oraclecloud.com"
    }
    use_path_style              = true
    skip_region_validation      = true
    skip_credentials_validation = true
    skip_requesting_account_id  = true
    skip_metadata_api_check     = true
    skip_s3_checksum            = true
  }
}

provider "oci" {
  region              = var.region
  auth                = "SecurityToken"
  config_file_profile = "lutiano-terraform"
}