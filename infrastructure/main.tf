provider "azurerm" {}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-producer-${var.env}"
  location = "${var.location_app}"
}

module "servicebus-namespace" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-namespace.git"
  name                  = "${var.product}-servicebus-${var.env}"
  location              = "${var.location_app}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
}

module "servicebus-queue" {
  source = "git@github.com:hmcts/terraform-module-servicebus-queue.git"
  name = "${var.product}-servicebus-queue-${var.env}"
  namespace_name = "${module.servicebus-namespace.name}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
}

# save the queue's "listen" connection string to vault
resource "vault_generic_secret" "servicebus-listen-conn-string" {
  path = "secret/${var.env}/cc/send-letter/servicebus-listen-conn-string"

  data_json = <<EOT
    {
      "value": "${module.servicebus-queue.primary_listen_connection_string}"
    }
    EOT
}

module "db" {
  source              = "git@github.com:contino/moj-module-postgres.git"
  product             = "${var.product}"
  location            = "${var.location_db}"
  env                 = "${var.env}"
  postgresql_database = "letter_tracking"
  postgresql_user     = "letter_tracking"
}

module "send-letter-producer-service" {
  source              = "git@github.com:contino/moj-module-webapp?ref=master"
  product             = "${var.product}-producer"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  resource_group_name = "${azurerm_resource_group.rg.name}"

  app_settings = {
    S2S_URL                       = "http://betadevbccidams2slb.reform.hmcts.net:80"
    SERVICE_BUS_CONNECTION_STRING = "${module.servicebus-queue.primary_send_connection_string}"
    LETTER_TRACKING_DB_HOST       = "${module.db.host_name}"
    LETTER_TRACKING_DB_PORT       = "${module.db.postgresql_listen_port}"
    LETTER_TRACKING_DB_USER_NAME  = "${module.db.user_name}"
    LETTER_TRACKING_DB_PASSWORD   = "${module.db.postgresql_password}"
    LETTER_TRACKING_DB_NAME       = "${module.db.postgresql_database}"
  }
}
