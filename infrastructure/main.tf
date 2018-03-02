provider "azurerm" {}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.microservice}-${var.env}"
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
  path = "secret/${var.vault_section}/cc/send-letter/servicebus-listen-conn-string"

  data_json = <<EOT
    {
      "value": "${module.servicebus-queue.primary_listen_connection_string}"
    }
    EOT
}

module "db" {
  source              = "git@github.com:contino/moj-module-postgres.git?ref=feature/specify-db-name"
  product             = "${var.product}"
  location            = "${var.location_db}"
  env                 = "${var.env}"
  postgresql_database = "letter_tracking"
  postgresql_user     = "letter_tracking"
}

module "send-letter-producer-service" {
  source              = "git@github.com:contino/moj-module-webapp?ref=master"
  product             = "${var.product}-${var.microservice}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  subscription        = "${var.subscription}"

  app_settings = {
    S2S_URL                       = "${var.s2s_url}"
    SERVICE_BUS_CONNECTION_STRING = "${module.servicebus-queue.primary_send_connection_string}"
    LETTER_TRACKING_DB_HOST       = "${module.db.host_name}"
    LETTER_TRACKING_DB_PORT       = "${module.db.postgresql_listen_port}"
    LETTER_TRACKING_DB_USER_NAME  = "${module.db.user_name}"
    LETTER_TRACKING_DB_PASSWORD   = "${module.db.postgresql_password}"
    LETTER_TRACKING_DB_NAME       = "${module.db.postgresql_database}"
    FLYWAY_URL                    = "jdbc:postgresql://${module.db.host_name}:${module.db.postgresql_listen_port}/${module.db.postgresql_database}"
    FLYWAY_USER                   = "${module.db.user_name}"
    FLYWAY_PASSWORD               = "${module.db.postgresql_password}"
  }
}

# region save DB details to Azure Key Vault
module "key-vault" {
  source              = "git@github.com:contino/moj-module-key-vault?ref=master"
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  # dcd_cc-dev group object ID
  product_group_object_id = "38f9dea6-e861-4a50-9e73-21e64f563537"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = "${var.microservice}-POSTGRES-USER"
  value     = "${module.db.user_name}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = "${var.microservice}-POSTGRES-PASS"
  value     = "${module.db.postgresql_password}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = "${var.microservice}-POSTGRES-HOST"
  value     = "${module.db.host_name}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = "${var.microservice}-POSTGRES-PORT"
  value     = "${module.db.postgresql_listen_port}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = "${var.microservice}-POSTGRES-DATABASE"
  value     = "${module.db.postgresql_database}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}
# endregion
