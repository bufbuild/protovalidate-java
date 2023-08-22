# See https://tech.davis-hansson.com/p/make/
SHELL := bash
.DELETE_ON_ERROR:
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-print-directory
BIN := .tmp/bin
COPYRIGHT_YEARS := 2023
LICENSE_IGNORE :=
JAVA_VERSION = 20
JAVAC = javac
JAVA = java
GO ?= go
ARGS ?= --strict_message
JAVA_COMPILE_OPTIONS = --enable-preview --release $(JAVA_VERSION)
JAVA_OPTIONS = --enable-preview
PROTOVALIDATE_VERSION ?= v0.3.1
JAVA_MAIN_CLASS = build.buf.protovalidate
JAVA_SOURCES = $(wildcard src/main/java/**/**/**/*.java, src/main/java/**/**/*.java)
JAVA_CLASSES = $(patsubst src/main/java/%.java, target/classes/%.class, $(JAVA_SOURCES))

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
	@# -X only removes untracked files, -d recurses into directories, -f actually removes files/dirs
	git clean -Xdf

.PHONY: conformance
conformance: build $(BIN)/protovalidate-conformance  ## Execute conformance tests.
	./gradlew conformance:jar
	$(BIN)/protovalidate-conformance $(ARGS) ./conformance/conformance.sh

.PHONY: generate-license
generate-license: $(BIN)/license-header  ## Generates license headers for all source files.
	$(BIN)/license-header \
		--license-type apache \
		--copyright-holder "Buf Technologies, Inc." \
		--year-range "$(COPYRIGHT_YEARS)" $(LICENSE_IGNORE)

.PHONY: help
help:  ## Describe useful make targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-15s %s\n", $$1, $$2}'

.PHONY: generate
generate: generate-license  ## Regenerate code and license headers
	buf generate --template buf.gen.yaml buf.build/bufbuild/protovalidate
	buf generate --template conformance/buf.gen.yaml -o conformance/ buf.build/bufbuild/protovalidate-testing

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

$(BIN):
	@mkdir -p $(BIN)

$(BIN)/license-header: $(BIN) Makefile
	GOBIN=$(abspath $(@D)) $(GO) install \
		  github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@latest

$(BIN)/protovalidate-conformance: $(BIN) Makefile
	GOBIN=$(abspath $(BIN)) $(GO) install \
		github.com/bufbuild/protovalidate/tools/protovalidate-conformance@$(PROTOVALIDATE_VERSION)

