package dev.droidtop.shared.containers

/**
 * A container's architecture (droidtop docs/SPEC.md 3c, owner 2026-11-22: "letting us run either x86 or arm
 * containers"): the architecture of its image and of the programs built for it.
 *
 * An arm64 container on an arm64 device runs natively. An x86 container there runs its whole userland through a
 * [Translator]. A container's image is pulled for [ociPlatform].
 */
enum class ContainerArch(
    /** How droidtop keeps it in a container's settings. */
    val id: String,
    /** The OCI image index's `architecture` (and [ociVariant]) for this architecture. */
    val ociArchitecture: String,
    val ociVariant: String?,
    /** The ELF header's `e_machine` for programs built for it. */
    val elfMachine: Int,
    /** 64 or 32: the ELF class of its programs. */
    val bits: Int,
    /** What a person reads. */
    val label: String,
) {
    ARM64("arm64", "arm64", "v8", 183, 64, "Arm (64-bit)"),
    X86_64("x86_64", "amd64", null, 62, 64, "x86 (64-bit)"),
    X86("x86", "386", null, 3, 32, "x86 (32-bit)");

    /** `linux/arm64/v8`, `linux/amd64`: the platform an image is pulled for. */
    val ociPlatform: String get() = listOfNotNull("linux", ociArchitecture, ociVariant).joinToString("/")

    /**
     * Whether a program of this architecture runs on a [host] without a translator: the same architecture, or 32-bit
     * x86 on an x86_64 host. 32-bit Arm is not one of droidtop's architectures (Droidtop/tracker#646).
     */
    fun runsNativelyOn(host: ContainerArch): Boolean = this == host || (this == X86 && host == X86_64)

    companion object {
        fun fromId(id: String?): ContainerArch? = entries.firstOrNull { it.id == id }

        /** From an OCI index entry's `architecture`; null for one droidtop does not run. */
        fun fromOci(architecture: String?): ContainerArch? = entries.firstOrNull { it.ociArchitecture == architecture }

        /** From an ELF header's machine and class; null for one droidtop does not run. */
        fun fromElf(machine: Int, bits: Int): ContainerArch? = entries.firstOrNull { it.elfMachine == machine && it.bits == bits }

        /**
         * The device's own architecture from what the system reports: Java's `os.arch`, `uname -m`, or an Android ABI
         * (`arm64-v8a`, `x86_64`). Null for one droidtop does not run.
         */
        fun host(reported: String): ContainerArch? = when (reported.trim().lowercase()) {
            "aarch64", "arm64", "arm64-v8a", "armv8", "armv8l" -> ARM64
            "x86_64", "amd64", "x64" -> X86_64
            "x86", "i386", "i486", "i586", "i686" -> X86
            else -> null
        }
    }
}
