provider "azurerm" {}

module "send-letter-producer-service" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-producer"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    S2S_URL                       = "http://betadevbccidams2slb.reform.hmcts.net:80"
    SERVICE_BUS_CONNECTION_STRING = "TODO"
    SERVICE_BUS_QUEUE_NAME        = "TODO"
  }
}
