package net.qvarford.giraffeed.it.util

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.SnapshotVerifier
import au.com.origin.snapshots.config.PropertyResolvingSnapshotConfig
import org.junit.jupiter.api.TestInfo

class Verifier<T>(cls: Class<T>) {
    private val snapshotVerifier = SnapshotVerifier(PropertyResolvingSnapshotConfig(), cls, false)

    fun expect(info: TestInfo): Expect {
        return Expect.of(snapshotVerifier, info.testMethod.get())
    }

    fun afterAll() {
        snapshotVerifier.validateSnapshots()
    }
}