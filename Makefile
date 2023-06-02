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
LICENSE_IGNORE := -e build/buf/validate -e build/tests
# Set to use a different compiler. For example, `GO=go1.18rc1 make test`.
GO ?= go
ARGS ?=
JAVA_VERSION = 20
JAVAC = javac
JAVA = java

JAVA_COMPILE_OPTIONS = --enable-preview --release $(JAVA_VERSION)
JAVA_OPTIONS = --enable-preview

JAVA_MAIN_CLASS = build.buf.protovalidate
JAVA_SOURCES = $(wildcard src/main/java/**/**/**/*.java, src/main/java/**/**/*.java)
JAVA_CLASSES = $(patsubst src/main/java/%.java, target/classes/%.class, $(JAVA_SOURCES))

.PHONY: help
help: ## Describe useful make targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-15s %s\n", $$1, $$2}'

.PHONY: all
all: lint build ## Run all tests and lint (default)

.PHONY: clean
clean:  ## Delete intermediate build artifacts
	@# -X only removes untracked files, -d recurses into directories, -f actually removes files/dirs
	git clean -Xdf

.PHONY: build
build: ## Build the entire project.
	./gradlew build

.PHONY: conformance
conformance: build $(BIN)/protovalidate-conformance
	./gradlew conformance:jar
	protovalidate-conformance ./conformance.sh

.PHONY: lint
lint: lint-java  ## Lint code

.PHONY: lint-java
lint-java: ## Run lint.
	./gradlew spotlessCheck

.PHONY: lintfix
lintfix: # Applies the lint changes.
	./gradlew spotlessApply

.PHONY: generate
generate: generate-proto generate-license ## Regenerate code and license headers

.PHONY: generate-proto
generate-proto: $(BIN)/buf
	#$(BIN)/buf generate
	$(BIN)/buf generate buf.build/bufbuild/protovalidate-testing
	$(BIN)/buf generate buf.build/bufbuild/protovalidate

.PHONY: generate-license
generate-license: $(BIN)/license-header
	@# We want to operate on a list of modified and new files, excluding
	@# deleted and ignored files. git-ls-files can't do this alone. comm -23 takes
	@# two files and prints the union, dropping lines common to both (-3) and
	@# those only in the second file (-2). We make one git-ls-files call for
	@# the modified, cached, and new (--others) files, and a second for the
	@# deleted files.
	comm -23 \
		<(git ls-files --cached --modified --others --no-empty-directory --exclude-standard | sort -u | grep -v $(LICENSE_IGNORE) ) \
		<(git ls-files --deleted | sort -u) | \
		xargs $(BIN)/license-header \
			--license-type apache \
			--copyright-holder "Buf Technologies, Inc." \
			--year-range "$(COPYRIGHT_YEARS)"

.PHONY: checkgenerate
checkgenerate: generate
	@# Used in CI to verify that `make generate` doesn't produce a diff.
	test -z "$$(git status --porcelain | tee /dev/stderr)"

$(BIN):
	@mkdir -p $(BIN)

$(BIN)/buf: $(BIN) Makefile
	GOBIN=$(abspath $(@D)) $(GO) install github.com/bufbuild/buf/cmd/buf@latest

$(BIN)/license-header: $(BIN) Makefile
	GOBIN=$(abspath $(@D)) $(GO) install \
		  github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@latest

$(BIN)/protovalidate-conformance: $(BIN) Makefile
	GOBIN=$(abspath $(BIN)) $(GO) install \
		github.com/bufbuild/protovalidate/tools/protovalidate-conformance@latest

