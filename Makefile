# See https://tech.davis-hansson.com/p/make/
SHELL := bash
.DELETE_ON_ERROR:
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-print-directory
GRADLE ?= ./gradlew

.PHONY: all
all: lint build docs conformance  ## Run all tests and lint (default)

.PHONY: build
build:  ## Build the entire project.
	$(GRADLE) build

.PHONY: docs
docs:  ## Build javadocs for the project.
	$(GRADLE) javadoc

.PHONY: clean
clean:  ## Delete intermediate build artifacts
	$(GRADLE) clean

.PHONY: conformance
conformance: ## Execute conformance tests.
	$(GRADLE) conformance:conformance

.PHONY: help
help: ## Describe useful make targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-15s %s\n", $$1, $$2}'

.PHONY: lint
lint: ## Lint code
	$(GRADLE) spotlessCheck

.PHONY: lintfix
lintfix:  ## Applies the lint changes.
	$(GRADLE) spotlessApply

.PHONY: release
release: ## Upload artifacts to Sonatype Nexus.
	$(GRADLE) --info publish --stacktrace --no-daemon --no-parallel
	$(GRADLE) --info releaseRepository

.PHONY: releaselocal
releaselocal: ## Release artifacts to local maven repository.
	$(GRADLE) --info publishToMavenLocal

.PHONY: test
test:  ## Run all tests.
	$(GRADLE) test
