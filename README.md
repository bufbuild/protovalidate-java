# [![The Buf logo](.github/buf-logo.svg)][buf] protovalidate-java

[![CI](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml)
[![Conformance](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml)
[![BSR](https://img.shields.io/badge/BSR-Module-0C65EC)][buf-mod]

[Protovalidate][protovalidate] provides standard annotations to validate common constraints on messages and fields, as well as the ability to use [CEL][cel] to write custom constraints. It's the next generation of [protoc-gen-validate][protoc-gen-validate], the only widely used validation library for Protobuf.

With Protovalidate, you can annotate your Protobuf messages with both standard and custom validation rules:

```protobuf
syntax = "proto3";

package banking.v1;

import "buf/validate/validate.proto";

message MoneyTransfer {
  string to_account_id = 1 [
    // Standard rule: `to_account_id` must be a UUID
    (buf.validate.field).string.uuid = true
  ];

  string from_account_id = 2 [
    // Standard rule: `from_account_id` must be a UUID
    (buf.validate.field).string.uuid = true
  ];

  // Custom rule: `to_account_id` and `from_account_id` can't be the same.
  option (buf.validate.message).cel = {
    id: "to_account_id.not.from_account_id"
    message: "to_account_id and from_account_id should not be the same value"
    expression: "this.to_account_id != this.from_account_id"
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
> The easiest way to get started with Protovalidate for RPC APIs are the how-to's in Buf's documentation. There's one available for [Java and gRPC][grpc-java].

`protovalidate-java` is listed in [Maven Central][maven], which provides installation snippets for Gradle, Maven, and other package managers. In Gradle, it's:

```gradle
dependencies {
    implementation 'build.buf:protovalidate:<version>'
}
```

# Documentation

Comprehensive documentation for Protovalidate is available in [Buf's documentation library][protovalidate].

Highlights for Java developers include:

* The [developer quickstart][quickstart]
* A comprehensive RPC how-to's for [Java and gRPC][grpc-java]
* A [migration guide for protoc-gen-validate][migration-guide] users

# Additional Languages and Repositories

Protovalidate isn't just for Java! You might be interested in sibling repositories for other languages:

- [`protovalidate-go`][pv-go] (Go)
- [`protovalidate-python`][pv-python] (Python)
- [`protovalidate-cc`][pv-cc] (C++)
- `protovalidate-ts` (TypeScript, coming soon!)

For a peek into how Protovalidate works, you might also want to check out [`protovalidate's core repository`](https://github.com/bufbuild/protovalidate), where `validate.proto` defines the entire cross-language API.

## Related Sites

- [Buf][buf] - Enterprise-grade Kafka and gRPC for the modern age
- [Common Expression Language (CEL)][cel] - The open-source technology at the core of Protovalidate

# Contribution

We genuinely appreciate any help! If you'd like to contribute, the following will be of interest:

- [Contributing Guidelines][contributing] - Guidelines to make your contribution process straightforward and meaningful
- [Conformance testing utilities](https://github.com/bufbuild/protovalidate/tree/main/docs/conformance.md) - Utilities providing acceptance testing of `protovalidate` implementations

# Legal

Offered under the [Apache 2 license][license].

[buf]: https://buf.build
[cel]: https://cel.dev

[pv-go]: https://github.com/bufbuild/protovalidate-go
[pv-java]: https://github.com/bufbuild/protovalidate-java
[pv-python]: https://github.com/bufbuild/protovalidate-python
[pv-cc]: https://github.com/bufbuild/protovalidate-cc

[license]: LICENSE
[contributing]: .github/CONTRIBUTING.md
[buf-mod]: https://buf.build/bufbuild/protovalidate

[protoc-gen-validate]: https://github.com/bufbuild/protoc-gen-validate

[protovalidate]: https://buf.build/docs/protovalidate/overview/
[quickstart]: https://buf.build/docs/protovalidate/quickstart/
[connect-go]: https://buf.build/docs/protovalidate/how-to/connect-go/
[grpc-go]: https://buf.build/docs/protovalidate/how-to/grpc-go/
[grpc-java]: https://buf.build/docs/protovalidate/how-to/grpc-java/
[grpc-python]: https://buf.build/docs/protovalidate/how-to/grpc-python/
[migration-guide]: https://buf.build/docs/migration-guides/migrate-from-protoc-gen-validate/

[maven]: https://central.sonatype.com/artifact/build.buf/protovalidate/overview
