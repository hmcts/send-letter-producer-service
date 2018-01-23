
module "send-letter-producer-service-api" {
  source   = "git@github.com/hmcts/terraform-module-webapp.git"
  product  = "${var.product}"
  location = "${var.location_api}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
  }
}
