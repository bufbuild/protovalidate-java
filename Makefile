# See https://tech.davis-hansson.com/p/make/
SHELL := bash
.DELETE_ON_ERROR:
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-print-directory

.PHONY: all
all: lint generate build docs conformance  ## Run all tests and lint (default)

.PHONY: build
build:  ## Build the entire project.
	./gradlew build

.PHONY: docs
docs:  ## Build javadocs for the project.
	./gradlew javadoc

.PHONY: checkgenerate
checkgenerate: generate  ## Checks if `make generate` produces a diff.
	@# Used in CI to verify that `make generate` doesn't produce a diff.
	test -z "$$(git status --porcelain | tee /dev/stderr)"

.PHONY: clean
clean:  ## Delete intermediate build artifacts
	./gradlew clean

.PHONY: conformance
conformance: ## Execute conformance tests.
	./gradlew conformance:conformance

.PHONY: help
help: ## Describe useful make targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-15s %s\n", $$1, $$2}'

.PHONY: generate
generate: ## Regenerate code and license headers
	./gradlew generate

.PHONY: lint
lint: ## Lint code
	./gradlew spotlessCheck

.PHONY: lintfix
lintfix:  ## Applies the lint changes.
	./gradlew spotlessApply

.PHONY: release
release: ## Upload artifacts to Sonatype Nexus.
	./gradlew --info publish --stacktrace --no-daemon --no-parallel
	./gradlew --info closeAndReleaseRepository

.PHONY: releaselocal
releaselocal: ## Release artifacts to local maven repository.
	./gradlew --info publishToMavenLocal

.PHONY: test
test:  ## Run all tests.
	./gradlew test
