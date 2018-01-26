#!/usr/bin/env sh

print_help() {
  echo "Script to run docker containers for Spring Boot Template API service

  Usage:

  ./run-in-docker.sh [OPTIONS]

  Options:
    --clean, -c                   Clean and install current state of source code
    --install, -i                 Install current state of source code
    --param PARAM=, -p PARAM=     Parse script parameter
    --help, -h                    Print this help block

  Available parameters:
    APPLICATION_INSIGHTS_IKEY     Defaults to '00000000-0000-0000-0000-000000000000'
    S2S_URL                       Defaults to 'false' - disables health check
    STUB_NOTIFY                   Defaults to 'true'
  "
}

# script execution flags
GRADLE_CLEAN=false
GRADLE_INSTALL=false

# environment variables
APPLICATION_INSIGHTS_IKEY="00000000-0000-0000-0000-000000000000"
STUB_NOTIFY=true
S2S_URL=false

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
  export STUB_NOTIFY=${STUB_NOTIFY}
  export S2S_URL=${S2S_URL}

  echo "Bringing up docker containers.."

  docker-compose up
}

while true ; do
  case "$1" in
    -h|--help) print_help ; shift ; break ;;
    -c|--clean) GRADLE_CLEAN=true ; GRADLE_INSTALL=true ; shift ;;
    -i|--install) GRADLE_INSTALL=true ; shift ;;
    -p|--param)
      case "$2" in
        APPLICATION_INSIGHTS_IKEY=*) APPLICATION_INSIGHTS_IKEY="${2#*=}" ; shift 2 ;;
        STUB_NOTIFY=*) STUB_NOTIFY="${2#*=}" ; shift 2 ;;
        S2S_URL=*) S2S_URL="${2#*=}" ; shift 2 ;;
        *) shift 2 ;;
      esac ;;
    *) execute_script ; break ;;
  esac
done
