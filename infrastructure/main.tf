provider "azurerm" {}

module "send-letter-producer-service" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-producer"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
  }
}
