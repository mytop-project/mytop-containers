package dev.droidtop.shared.containers

/**
 * A container's architecture and the translator its foreign programs run through (droidtop docs/SPEC.md 3c), kept
 * beside the container's other settings in the same `key=value` line form the backends already use.
 *
 * An old container with neither line is an arm64 container on an arm64 device with the default translator, which is
 * what every container was before architectures existed.
 */
data class ContainerPlatform(
    val arch: ContainerArch,
    val translator: Translator = Translator.DEFAULT,
) {
    /** Whether this container's own userland runs translated on [host]: an x86 container on an arm64 device. */
    fun translatedOn(host: ContainerArch): Boolean = !arch.runsNativelyOn(host)

    /** Whether it can run on [host] at all: natively, or with a translator that covers its architecture. */
    fun runsOn(host: ContainerArch): Boolean =
        arch.runsNativelyOn(host) || Translator.forProgram(arch, host, translator) != null

    fun encode(): String = "$KEY_ARCH=${arch.id}\n$KEY_TRANSLATOR=${translator.id}\n"

    companion object {
        const val KEY_ARCH = "arch"
        const val KEY_TRANSLATOR = "translator"

        /** The platform a container made on [host] has when it says nothing: [host]'s own, default translator. */
        fun native(host: ContainerArch): ContainerPlatform = ContainerPlatform(host)

        /**
         * Reads the lines [encode] writes, among any others. A missing or unknown architecture is [host]'s own; a
         * missing or unknown translator is the default, never an error, so a damaged file still opens the container.
         */
        fun decode(text: String, host: ContainerArch): ContainerPlatform {
            val lines = text.lineSequence().mapNotNull { line ->
                line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
            }.toMap()
            return ContainerPlatform(
                arch = ContainerArch.fromId(lines[KEY_ARCH]) ?: host,
                translator = Translator.fromId(lines[KEY_TRANSLATOR]) ?: Translator.DEFAULT,
            )
        }
    }
}
