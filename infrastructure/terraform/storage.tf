data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.compartment_ocid
}

locals {
  storage_buckets = toset([
    "lutiano-develop-products",
    "lutiano-develop-seller-documents",
    "lutiano-production-products",
    "lutiano-production-seller-documents",
  ])
}

resource "oci_objectstorage_bucket" "files" {
  for_each = local.storage_buckets

  compartment_id = var.compartment_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = each.key
  access_type    = "NoPublicAccess"
  storage_tier   = "Standard"

  lifecycle {
    prevent_destroy = true
  }
}