locals {
  web_ports = {
    http  = 80
    https = 443
  }

  web_backends = {
    for pair in setproduct(keys(var.vms), keys(local.web_ports)) :
    "${pair[0]}-${pair[1]}" => { vm = pair[0], port_name = pair[1] }
  }
}

resource "oci_core_public_ip" "nlb" {
  compartment_id = var.compartment_ocid
  lifetime       = "RESERVED"
  display_name   = "lutiano-nlb-ip"

  lifecycle {
    prevent_destroy = false
  }
}

resource "oci_network_load_balancer_network_load_balancer" "main" {
  compartment_id                 = var.compartment_ocid
  display_name                   = "lutiano-nlb"
  subnet_id                      = oci_core_subnet.public.id
  is_private                     = false
  is_preserve_source_destination = false

  reserved_ips {
    id = oci_core_public_ip.nlb.id
  }
}

resource "oci_network_load_balancer_backend_set" "web" {
  for_each = local.web_ports

  name                     = each.key
  network_load_balancer_id = oci_network_load_balancer_network_load_balancer.main.id
  policy                   = "FIVE_TUPLE"
  is_preserve_source       = false

  health_checker {
    protocol = "TCP"
    port     = each.value
  }
}

resource "oci_network_load_balancer_backend" "web" {
  for_each = local.web_backends

  network_load_balancer_id = oci_network_load_balancer_network_load_balancer.main.id
  backend_set_name         = oci_network_load_balancer_backend_set.web[each.value.port_name].name
  port                     = local.web_ports[each.value.port_name]
  target_id                = oci_core_instance.vm[each.value.vm].id
}

resource "oci_network_load_balancer_listener" "web" {
  for_each = local.web_ports

  name                     = each.key
  network_load_balancer_id = oci_network_load_balancer_network_load_balancer.main.id
  default_backend_set_name = oci_network_load_balancer_backend_set.web[each.key].name
  port                     = each.value
  protocol                 = "TCP"
}