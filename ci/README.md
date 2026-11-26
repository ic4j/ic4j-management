# GitHub build and test

The workflow runs for pull requests and pushes to `master`, or on manual
dispatch. Java 21 and Gradle 8.14 build the Java-8-compatible module using
fixed Candid and Agent source commits in a new job-local Maven repository.
No package is published.

A disposable, loopback-only DFX 0.32.0 replica supplies a routing canister.
The Java test generates a transient signing key and creates a separate
canister. It installs the repo's test WASM, confirms that the canister is
running, calls the update method and checks its reply, stops the canister
and verifies its stopped status, then deletes it and verifies a rejected
status request. The replica is stopped when the job exits. The old test
that assumes a workstation identity file and static effective canister ID
is not selected in CI. Nothing connects to a public ICP replica.

The Actions artifact includes JUnit XML, versions, exact source revisions,
hashes and the replica deployment log.
