[![The Buf logo](.github/buf-logo.svg)][buf]

# protovalidate-java

[![CI](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml)
[![Conformance](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml)
[![BSR](https://img.shields.io/badge/BSR-Module-0C65EC)][buf-mod]

[Protovalidate][protovalidate] is the semantic validation library for Protobuf. It provides standard annotations to validate common rules on messages and fields, as well as the ability to use [CEL][cel] to write custom rules. It's the next generation of [protoc-gen-validate][protoc-gen-validate].

With Protovalidate, you can annotate your Protobuf messages with both standard and custom validation rules:

```protobuf
syntax = "proto3";

package acme.user.v1;

import "buf/validate/validate.proto";

message User {
  string id = 1 [(buf.validate.field).string.uuid = true];
  uint32 age = 2 [(buf.validate.field).uint32.lte = 150]; // We can only hope.
  string email = 3 [(buf.validate.field).string.email = true];
  string first_name = 4 [(buf.validate.field).string.max_len = 64];
  string last_name = 5 [(buf.validate.field).string.max_len = 64];

  option (buf.validate.message).cel = {
    id: "first_name_requires_last_name"
    message: "last_name must be present if first_name is present"
    expression: "!has(this.first_name) || has(this.last_name)"
  };
}
```

Once you've added `protovalidate-java` to your project, validation is idiomatic Java:

```java
ValidationResult result = validator.validate(message);
if (!result.isSuccess()) {
    // Handle failure.
}
```

## Installation

> [!TIP]
> The easiest way to get started with Protovalidate for RPC APIs are the quickstarts in Buf's documentation. There's one available for [Java and gRPC][grpc-java].

`protovalidate-java` is listed in [Maven Central][maven], which provides installation snippets for Gradle, Maven, and other package managers. In Gradle, it's:

```gradle
dependencies {
    implementation 'build.buf:protovalidate:<version>'
}
```

## Documentation

Comprehensive documentation for Protovalidate is available at [protovalidate.com][protovalidate].

Highlights for Java developers include:

* The [developer quickstart][quickstart]
* A comprehensive RPC quickstart for [Java and gRPC][grpc-java]
* A [migration guide for protoc-gen-validate][migration-guide] users

## Additional languages and repositories

Protovalidate isn't just for Java! You might be interested in sibling repositories for other languages:

- [`protovalidate-go`][pv-go] (Go)
- [`protovalidate-python`][pv-python] (Python)
- [`protovalidate-cc`][pv-cc] (C++)
- [`protovalidate-es`][pv-es] (TypeScript and JavaScript)

Additionally, [protovalidate's core repository](https://github.com/bufbuild/protovalidate) provides:

- [Protovalidate's Protobuf API][validate-proto]
- [Conformance testing utilities][conformance] for acceptance testing of `protovalidate` implementations


## Contributing

We genuinely appreciate any help! If you'd like to contribute, check out these resources:

- [Contributing Guidelines][contributing]: Guidelines to make your contribution process straightforward and meaningful
- [Conformance testing utilities](https://github.com/bufbuild/protovalidate/tree/main/docs/conformance.md): Utilities providing acceptance testing of `protovalidate` implementations

## Legal

Offered under the [Apache 2 license][license].

[buf]: https://buf.build
[cel]: https://cel.dev

[pv-go]: https://github.com/bufbuild/protovalidate-go
[pv-java]: https://github.com/bufbuild/protovalidate-java
[pv-python]: https://github.com/bufbuild/protovalidate-python
[pv-cc]: https://github.com/bufbuild/protovalidate-cc
[pv-es]: https://github.com/bufbuild/protovalidate-es

[license]: LICENSE
[contributing]: .github/CONTRIBUTING.md
[buf-mod]: https://buf.build/bufbuild/protovalidate

[protoc-gen-validate]: https://github.com/bufbuild/protoc-gen-validate

[protovalidate]: https://protovalidate.com
[quickstart]: https://protovalidate.com/quickstart/
[connect-go]: https://protovalidate.com/quickstart/connect-go/
[grpc-go]: https://protovalidate.com/quickstart/grpc-go/
[grpc-java]: https://protovalidate.com/quickstart/grpc-java/
[grpc-python]: https://protovalidate.com/quickstart/grpc-python/
[migration-guide]: https://protovalidate.com/migration-guides/migrate-from-protoc-gen-validate/

[maven]: https://central.sonatype.com/artifact/build.buf/protovalidate/overview
[pkg-go]: https://pkg.go.dev/github.com/bufbuild/protovalidate-go

[validate-proto]: https://buf.build/bufbuild/protovalidate/docs/main:buf.validate
[conformance]: https://github.com/bufbuild/protovalidate/blob/main/docs/conformance.md
[examples]: https://github.com/bufbuild/protovalidate/tree/main/examples
[migrate]: https://protovalidate.com/migration-guides/migrate-from-protoc-gen-validate/
