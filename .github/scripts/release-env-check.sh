#!/usr/bin/env bash

[[ "${GITHUB_REPOSITORY}" != "tinkoff-mobile-tech/droidherd" ]] && exit 1
[[ "${GITHUB_REF_NAME}" =~ ^release-[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 1
