# [![The Buf logo](.github/buf-logo.svg)][buf] protovalidate-java

[![CI](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/ci.yaml)
[![Conformance](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml/badge.svg)](https://github.com/bufbuild/protovalidate-java/actions/workflows/conformance.yaml)
[![BSR](https://img.shields.io/badge/BSR-Module-0C65EC)][buf-mod]

`protovalidate-java` is the Java language implementation of [`protovalidate`](https://github.com/bufbuild/protovalidate) designed to validate Protobuf messages at runtime based on user-defined validation constraints. Powered by Google's Common Expression Language ([CEL](https://github.com/google/cel-spec)), it provides a flexible and efficient foundation for defining and evaluating custom validation rules. The primary goal of `protovalidate` is to help developers ensure data consistency and integrity across the network without requiring generated code.

## The `protovalidate` project

Head over to the core [`protovalidate`](https://github.com/bufbuild/protovalidate/) repository for:

- [The API definition](https://github.com/bufbuild/protovalidate/tree/main/proto/protovalidate/buf/validate/validate.proto): used to describe validation constraints
- [Documentation](https://github.com/bufbuild/protovalidate/tree/main/docs): how to apply `protovalidate` effectively
- [Migration tooling](https://github.com/bufbuild/protovalidate/tree/main/docs/migrate.md): incrementally migrate from `protoc-gen-validate`
- [Conformance testing utilities](https://github.com/bufbuild/protovalidate/tree/main/docs/conformance.md): for acceptance testing of `protovalidate` implementations

Other `protovalidate` runtime implementations include:

- C++: [`protovalidate-cc`](https://github.com/bufbuild/protovalidate-cc)
- Go: [`protovalidate-go`](https://github.com/bufbuild/protovalidate-go)

And others coming soon:

- Python: `protovalidate-py`
- TypeScript: `protovalidate-ts`

## Installation

To include `protovalidate-java` in your project, add the following to your build file:

```gradle
dependencies {
    implementation 'build.buf:protovalidate:1.0.0'
}
```

Remember to always check for the latest version of `protovalidate-java` on the project's [GitHub releases page](https://github.com/bufbuild/protovalidate-java/releases) to ensure you're using the most up-to-date version.

## Usage

### Implementing validation constraints

Validation constraints are defined directly within `.proto` files. Documentation for adding constraints can be found in the `protovalidate` project [README](https://github.com/bufbuild/protovalidate) and its [comprehensive docs](https://github.com/bufbuild/protovalidate/tree/main/docs).

```protobuf
syntax = "proto3";

package my.package;

import "google/protobuf/timestamp.proto";
import "buf/validate/validate.proto";

message Transaction {
  uint64 id = 1 [(buf.validate.field).uint64.gt = 999];
  google.protobuf.Timestamp purchase_date = 2;
  google.protobuf.Timestamp delivery_date = 3;
  
  string price = 4 [(buf.validate.field).cel = {
    id: "transaction.price",
    message: "price must be positive and include a valid currency symbol ($ or £)",
    expression: "(this.startsWith('$') || this.startsWith('£')) && double(this.substring(1)) > 0"
  }];
  
  option (buf.validate.message).cel = {
    id: "transaction.delivery_date",
    message: "delivery date must be after purchase date",
    expression: "this.delivery_date > this.purchase_date"
  };
}
```

### Example

In your Java code, create an instance of the `Validator` class and use the `validate` method to validate your messages.

```java
import com.google.protobuf.Timestamp;
import build.buf.protovalidate.Validator;
import my.package.Transaction;

public class Main {
  public static void main(String[] args) {
    Transaction.Builder transactionBuilder = Transaction.newBuilder()
        .setId(1234)
        .setPrice("$5.67");

    Timestamp purchaseDate = Timestamp.newBuilder()
        // set time for purchaseDate
        .build();
    
    Timestamp deliveryDate = Timestamp.newBuilder()
        // set time for deliveryDate
        .build();

    transactionBuilder.setPurchaseDate(purchaseDate);
    transactionBuilder.setDeliveryDate(deliveryDate);

    Transaction transaction = transactionBuilder.build();

    Validator validator = new Validator();
    try {
      validator.validate(transaction);
      System.out.println("Validation succeeded");
    } catch (ValidationException e) {
      System.out.println("Validation failed: " + e.getMessage());
    }
  }
}
```

### Ecosystem

- [`protovalidate`](https://github.com/bufbuild/protovalidate) core repository
- [Buf][buf]
- [CEL Spec][cel-spec]

## Legal

Offered under the [Apache 2 license][license].

[license]: LICENSE
[buf]: https://buf.build
[buf-mod]: https://buf.build/bufbuild/protovalidate
[cel-spec]: https://github.com/google/cel-spec
