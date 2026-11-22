package dev.droidtop.shared.containers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerPlatformTest {
    private val arm = ContainerArch.ARM64

    @Test
    fun hostNamesMapToArchitectures() {
        assertEquals(arm, ContainerArch.host("aarch64"))
        assertEquals(arm, ContainerArch.host("arm64-v8a"))
        assertEquals(ContainerArch.X86_64, ContainerArch.host("amd64"))
        assertEquals(ContainerArch.X86, ContainerArch.host("i686"))
        assertNull(ContainerArch.host("armeabi-v7a"))
    }

    @Test
    fun imagesArePulledForTheirOciPlatform() {
        assertEquals("linux/arm64/v8", arm.ociPlatform)
        assertEquals("linux/amd64", ContainerArch.X86_64.ociPlatform)
        assertEquals("linux/386", ContainerArch.X86.ociPlatform)
        assertEquals(ContainerArch.X86_64, ContainerArch.fromOci("amd64"))
    }

    @Test
    fun elfHeadersNameTheirArchitecture() {
        assertEquals(ContainerArch.X86_64, ContainerArch.fromElf(62, 64))
        assertEquals(ContainerArch.X86, ContainerArch.fromElf(3, 32))
        assertEquals(arm, ContainerArch.fromElf(183, 64))
        assertNull(ContainerArch.fromElf(40, 32))
    }

    @Test
    fun nativeProgramsNeedNoTranslator() {
        assertNull(Translator.forProgram(arm, arm, Translator.FEX))
        assertNull(Translator.forProgram(ContainerArch.X86, ContainerArch.X86_64, null))
    }

    @Test
    fun x86ProgramsOnArmTakeTheChosenTranslatorWhenItCoversThem() {
        assertEquals(Translator.BOX64, Translator.forProgram(ContainerArch.X86_64, arm, Translator.BOX64))
        assertEquals(Translator.FEX, Translator.forProgram(ContainerArch.X86_64, arm, Translator.FEX))
        // Box64 has no 32-bit x86: FEX runs those even in a container set to Box64.
        assertEquals(Translator.FEX, Translator.forProgram(ContainerArch.X86, arm, Translator.BOX64))
        assertEquals(Translator.FEX, Translator.forProgram(ContainerArch.X86_64, arm, null))
    }

    @Test
    fun armProgramsOnAnX86DeviceHaveNoTranslator() {
        assertNull(Translator.forProgram(arm, ContainerArch.X86_64, Translator.FEX))
        assertFalse(ContainerPlatform(arm).runsOn(ContainerArch.X86_64))
    }

    @Test
    fun anX86ContainerOnArmRunsTranslated() {
        val platform = ContainerPlatform(ContainerArch.X86_64, Translator.FEX)
        assertTrue(platform.translatedOn(arm))
        assertTrue(platform.runsOn(arm))
        assertFalse(ContainerPlatform(arm).translatedOn(arm))
    }

    @Test
    fun settingsRoundTripAmongOtherLines() {
        val platform = ContainerPlatform(ContainerArch.X86_64, Translator.BOX64)
        val text = "role=sibling\nimage=docker.io/library/debian:trixie\n" + platform.encode()
        assertEquals(platform, ContainerPlatform.decode(text, arm))
    }

    @Test
    fun oldOrDamagedSettingsReadAsTheHostsOwnPlatform() {
        assertEquals(ContainerPlatform(arm), ContainerPlatform.decode("role=primary\n", arm))
        assertEquals(ContainerPlatform(arm), ContainerPlatform.decode("arch=riscv64\ntranslator=qemu\n", arm))
        assertEquals(ContainerPlatform.native(ContainerArch.X86_64), ContainerPlatform.decode("", ContainerArch.X86_64))
    }
}
