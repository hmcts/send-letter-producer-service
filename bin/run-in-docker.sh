#!/usr/bin/env sh

print_help() {
  echo "Script to run docker containers for Spring Boot Template API service

  Usage:

  ./run-in-docker.sh [OPTIONS]

  Options:
    --clean, -c                         Clean and install current state of source code
    --install, -i                       Install current state of source code
    --with-flyway, -f                   Run docker compose with flyway enabled
    --param PARAM=, -p PARAM=           Parse script parameter
    --help, -h                          Print this help block

  Available parameters:
    APPLICATION_INSIGHTS_IKEY           Defaults to '00000000-0000-0000-0000-000000000000'
    LETTER_TRACKING_DB_PASSWORD         Defaults to 'password'
    S2S_URL                             Defaults to 'false' - disables health check
    SERVICE_BUS_CONNECTION_STRING       Defaults to 'sb://namespace.servicebus.windows.net'
    SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS   Default to '7'
  "
}

# script execution flags
GRADLE_CLEAN=false
GRADLE_INSTALL=false
FLYWAY_ENABLED=false

# environment variables
APPLICATION_INSIGHTS_IKEY="00000000-0000-0000-0000-000000000000"
LETTER_TRACKING_DB_PASSWORD="password"
S2S_URL=false
SERVICE_BUS_CONNECTION_STRING="sb://namespace.servicebus.windows.net"
SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS=7

execute_script() {
  cd $(dirname "$0")/..

  if [ ${GRADLE_CLEAN} = true ]
  then
    echo "Clearing previous build.."
    ./gradlew clean
  fi

  if [ ${GRADLE_INSTALL} = true ]
  then
    echo "Installing distribution.."
    ./gradlew installDist
  fi

  echo "Assigning environment variables.."

  export APPLICATION_INSIGHTS_IKEY=${APPLICATION_INSIGHTS_IKEY}
  export LETTER_TRACKING_DB_PASSWORD=${LETTER_TRACKING_DB_PASSWORD}
  export S2S_URL=${S2S_URL}
  export SERVICE_BUS_CONNECTION_STRING=${SERVICE_BUS_CONNECTION_STRING}
  export SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS=${SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS}

  echo "Bringing up docker containers.."

  if [ ${FLYWAY_ENABLED} = true ]
  then
    docker-compose -f docker-compose-flyway.yml up
  else
    docker-compose up
  fi
}

while true ; do
  case "$1" in
    -h|--help) print_help ; shift ; break ;;
    -c|--clean) GRADLE_CLEAN=true ; GRADLE_INSTALL=true ; shift ;;
    -i|--install) GRADLE_INSTALL=true ; shift ;;
    -f|--with-flyway) FLYWAY_ENABLED=true ; shift ;;
    -p|--param)
      case "$2" in
        APPLICATION_INSIGHTS_IKEY=*) APPLICATION_INSIGHTS_IKEY="${2#*=}" ; shift 2 ;;
        LETTER_TRACKING_DB_PASSWORD=*) LETTER_TRACKING_DB_PASSWORD="${2#*=}" ; shift 2 ;;
        S2S_URL=*) S2S_URL="${2#*=}" ; shift 2 ;;
        SERVICE_BUS_CONNECTION_STRING=*) SERVICE_BUS_CONNECTION_STRING="${2#*=}" ; shift 2 ;;
        SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS=*) SERVICE_QUEUE_MESSAGE_TTL_IN_DAYS="${2#*=}" ; shift 2 ;;
        *) shift 2 ;;
      esac ;;
    *) execute_script ; break ;;
  esac
done
