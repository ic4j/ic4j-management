package org.ic4j.management.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.AgentError;
import org.ic4j.agent.NonceFactory;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.http.ReplicaApacheHttpTransport;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.management.ManagementService;
import org.ic4j.management.Mode;
import org.ic4j.management.Status;
import org.ic4j.types.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "IC4J_CI_FIXTURE", matches = "^[a-z0-9]+(-[a-z0-9]+)+$")
class ManagementCiTest {
    @Test
    @Timeout(120)
    void createsInstallsCallsStopsAndDeletesDisposableCanister() throws Exception {
        // Never use a workstation key or an external replica. The fresh key
        // controls only the canister this test creates on its runner.
        Agent agent = new AgentBuilder()
                .transport(ReplicaApacheHttpTransport.create("http://127.0.0.1:4943/"))
                .identity(BasicIdentity.fromKeyPair(KeyPairGenerator.getInstance("Ed25519").generateKeyPair()))
                .nonceFactory(new NonceFactory()).build();
        try {
            agent.fetchRootKey();
            Principal seed = Principal.fromString(System.getenv("IC4J_CI_FIXTURE"));
            ManagementService provisional = ManagementService.create(agent, Principal.managementCanister(), seed);
            Principal canister = provisional.provisionalCreateCanisterWithCycles(Optional.empty(), Optional.empty())
                    .get(45, TimeUnit.SECONDS);
            assertNotEquals(seed, canister);
            ManagementService service = ManagementService.create(agent, Principal.managementCanister(), canister);
            try (InputStream wasm = getClass().getResourceAsStream("/hello.wasm")) {
                assertNotNull(wasm);
                // Use an empty Candid tuple; the fixture has no init arguments.
                service.installCode(canister, Mode.install,
                        readWasm(wasm), IDLArgs.create(java.util.Collections.emptyList()).toBytes());
            }
            assertEquals(Status.running, service.canisterStatus(canister).get(30, TimeUnit.SECONDS).status);
            HelloWorldProxy hello = ProxyBuilder.create(agent, canister).getProxy(HelloWorldProxy.class);
            assertEquals("Hello, from CI!", hello.greet("from CI").get(30, TimeUnit.SECONDS));
            service.stopCanister(canister);
            assertEquals(Status.stopped, service.canisterStatus(canister).get(30, TimeUnit.SECONDS).status);
            service.deleteCanister(canister);
            ExecutionException deletionError = assertThrows(ExecutionException.class,
                    () -> service.canisterStatus(canister).get(30, TimeUnit.SECONDS));
            assertInstanceOf(AgentError.class, deletionError.getCause());
            AgentError replicaError = (AgentError) deletionError.getCause();
            assertEquals(AgentError.AgentErrorCode.REPLICA_ERROR, replicaError.getCode());
            assertTrue(replicaError.getMessage().contains(canister.toString()));
            assertTrue(replicaError.getMessage().contains("not found"));
        } finally {
            agent.close();
        }
    }

    private static byte[] readWasm(InputStream stream) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] block = new byte[8192];
        for (int count; (count = stream.read(block)) != -1;) {
            buffer.write(block, 0, count);
        }
        return buffer.toByteArray();
    }
}
