variable "cluster_name" {
  default = "stream-ci-empty"
}
variable "region" {
  default = "us-central1"
}

variable "project" {
  default = "spring-cloud-dataflow-148214"
}
variable "disk_size" {
  default = 10
}

variable "node_count" {
  default = 1
}

variable "max_cluster_cpu" {
  default = 96
}
variable "max_cluster_memory" {
  default = 192
}

variable "machine_type" {
  default = "e2-standard-4"
}
variable "kubernetes_version" {
  default = "1.23"
}
variable "gcp_creds_json_file" {
}
variable "node_port" {
  default = "30000"
}
variable "service_account" {
  default = "scdf-gke-admin@spring-cloud-dataflow-148214.iam.gserviceaccount.com"
}
variable "port_name" {
  default = "http"
}
variable "pods_per_node" {
  default = "50"
}
provider "google" {
  project     = var.project
  region      = var.region
  credentials = var.gcp_creds_json_file
}

resource "google_compute_network" "vpc" {
  auto_create_subnetworks         = false
  delete_default_routes_on_create = false
  description                     = "Compute Network for GKE nodes"
  name                            = "${var.cluster_name}-vpc"
}

resource "google_compute_subnetwork" "scdf" {
  name          = "${var.cluster_name}-subnet"
  ip_cidr_range = "10.255.0.0/20"
  region        = var.region
  network       = google_compute_network.vpc.id
  secondary_ip_range {
    range_name    = local.cluster_secondary_range_name
    ip_cidr_range = "10.0.0.0/20"
  }

  secondary_ip_range {
    range_name    = local.services_secondary_range_name
    ip_cidr_range = "10.64.0.0/20"
  }
}

resource "google_container_node_pool" "nodes" {
  name       = "${var.cluster_name}-node-pool"
  cluster    = google_container_cluster.primary.id
  node_count = var.node_count
  node_config {
    machine_type    = var.machine_type
    service_account = var.service_account
    oauth_scopes    = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/trace.append",
      "https://www.googleapis.com/auth/monitoring",
    ]

    labels = {
      env = var.cluster_name
    }
    disk_size_gb = var.disk_size
    disk_type    = "pd-ssd"
    image_type   = "UBUNTU_CONTAINERD"
    tags         = ["stream-ci-node", var.cluster_name, "${var.cluster_name}-node"]
  }
  management {
    auto_upgrade = false
    auto_repair  = true
  }
  #  autoscaling {
  #    min_node_count = 1
  #    max_node_count = var.node_count
  #  }
  timeouts {
    create = "20m"
    update = "10m"
  }
}

resource "google_container_cluster" "primary" {
  name                      = var.cluster_name
  location                  = "${var.region}-f" # zonal cluster
  network                   = google_compute_network.vpc.name
  subnetwork                = google_compute_subnetwork.scdf.name
  initial_node_count        = 1
  remove_default_node_pool  = true
  default_max_pods_per_node = var.pods_per_node
  release_channel {
    channel = "UNSPECIFIED"
  }
  addons_config {
    http_load_balancing {
      disabled = false
    }
    horizontal_pod_autoscaling {
      disabled = false
    }
  }
  ip_allocation_policy {
  }
}

resource "google_compute_firewall" "stream-ci-fw" {
  name    = "${var.cluster_name}-firewall"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "22", "6080", "10250", "9443", "30000-32767"]
  }

  target_tags   = ["${var.cluster_name}-node"]
  source_ranges = ["0.0.0.0/0"]
}


data "google_container_cluster" "information" {
  name     = google_container_cluster.primary.name
  location = google_container_cluster.primary.location

  depends_on = [
    google_compute_network.vpc
  ]
}

provider "kubernetes" {
  host                   = google_container_cluster.primary.endpoint
  token                  = data.google_client_config.primary.access_token
  cluster_ca_certificate = base64decode(google_container_cluster.primary.master_auth.0.cluster_ca_certificate)
  config_path            = "~/.kube/config"
  client_key             = base64decode(google_container_cluster.primary.master_auth.0.client_key)
}

locals {
  cluster_secondary_range_name  = "${var.cluster_name}-cluster-secondary-range"
  services_secondary_range_name = "${var.cluster_name}-services-secondary-range"
}

data "google_client_config" "primary" {}
