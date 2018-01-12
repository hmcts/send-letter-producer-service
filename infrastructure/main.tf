
module "send-letter-service-api" {
  source   = "git@github.com/hmcts/terraform-module-webapp.git"
  product  = "${var.product}"
  location = "${var.location_api}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    REDIS_HOST     = "${module.redis-cache.host_name}"
    REDIS_PORT     = "${module.redis-cache.redis_port}"
    REDIS_PASSWORD = "${module.redis-cache.access_key}"
  }
}

module "redis-cache" {
  source   = "git@github.com:contino/moj-module-redis?ref=master"
  product  = "${var.product}-redis"
  location = "${var.location_api}"
  env      = "${var.env}"
  subnetid = "${data.terraform_remote_state.core_apps_infrastructure.subnet_ids[2]}"
}