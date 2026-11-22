package dev.droidtop.shared.containers

/**
 * What runs a program built for another architecture inside a container (droidtop docs/SPEC.md 3c): the same choice
 * a Wine prefix has between FEXCore and Box64 (5a), made per container, with a default.
 */
enum class Translator(val id: String, val label: String, private val covers: Set<ContainerArch>) {
    /** FEX-Emu: 32- and 64-bit x86 on arm64. Needs a glibc container. */
    FEX("fex", "FEX", setOf(ContainerArch.X86_64, ContainerArch.X86)),

    /** Box64: 64-bit x86 on arm64 only. */
    BOX64("box64", "Box64", setOf(ContainerArch.X86_64));

    /** Whether it runs [program]'s architecture on an arm64 host. */
    fun covers(program: ContainerArch): Boolean = program in covers

    companion object {
        fun fromId(id: String?): Translator? = entries.firstOrNull { it.id == id }

        /**
         * The translator a [program] needs on [host], given the container's [chosen] one: null when the program runs
         * natively; the chosen one when it covers the program; otherwise FEX where FEX covers it (it covers every
         * architecture a translator here does, so a container set to Box64 still runs its 32-bit x86 programs);
         * null when nothing here runs it (an Arm program on an x86 device).
         */
        fun forProgram(program: ContainerArch, host: ContainerArch, chosen: Translator?): Translator? = when {
            program.runsNativelyOn(host) -> null
            host != ContainerArch.ARM64 -> null
            chosen != null && chosen.covers(program) -> chosen
            FEX.covers(program) -> FEX
            else -> null
        }

        /** A container's default: FEX, which covers both x86 architectures; the person may pick Box64 instead. */
        val DEFAULT: Translator = FEX
    }
}
