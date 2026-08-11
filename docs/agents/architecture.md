# Architecture

## API and Protocol Changes

Make direct API and protocol changes and update every production and test caller. Add compatibility only when the user
explicitly requires downstream compatibility.

## Boundaries and State

- Keep shared transport path fragments, constants, and message keys in `shared/`.
- Put code shared by `server.transport` and `server.ee` in `server.runtime`; transport code must not depend on
  `server.ee`.
- Guard Subscription and Dataset Cache Version state and packet sending with `ReplicantSession.getLock()`. Follow the
  locking patterns in `ReplicantSessionManagerImpl` and `ReplicantMessageBrokerImpl`.

## Transport Encoding

Keep client and server transport routes, validation, and message formats synchronized through shared constants and
message keys. Use JSON-P builders and generators for server-side JSON encoding.
