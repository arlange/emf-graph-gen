/**
 * Copyright 2026 Arne Lange
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import stocheck.StoSquareChecker
import java.io.File

/**
 * Scans every "instance_*.sto" model file found in the repository root (loose,
 * untracked files produced by sto-gentool) and checks all of their Type-Squares
 * for well-formedness, one dynamic test per file. If none are found, this simply
 * contributes no tests rather than failing - these files aren't part of the
 * checked-in source tree, so their presence varies by local setup.
 */
class StoInstanceFileCheckerTests {

    companion object {
        private var totalCheckTimeMs: Long = 0
        private var totalFilesChecked: Int = 0

        @JvmStatic
        @AfterAll
        fun printOverallCheckTime() {
            if (totalFilesChecked == 0) return
            println(
                "Checked $totalFilesChecked STO file(s) in $totalCheckTimeMs ms total " +
                        "(avg ${totalCheckTimeMs / totalFilesChecked} ms/file)."
            )
        }
    }

    @TestFactory
    fun checkInstanceFiles(): List<DynamicTest> {
        val instanceFiles = File("..")
            .listFiles { file -> file.name.matches(Regex("instance_\\d+\\.sto")) }
            ?.sortedBy { it.name }
            ?: emptyList()
        if (instanceFiles.isEmpty()) return emptyList()

        Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["sto"] = XMIResourceFactoryImpl()
        val resourceSet = ResourceSetImpl()
        val metamodelResource = resourceSet.getResource(URI.createFileURI(createTempStoMetamodelFile().toString()), true)
        val ePackage = metamodelResource.contents[0] as EPackage
        resourceSet.packageRegistry[ePackage.nsURI] = ePackage

        return instanceFiles.map { file ->
            DynamicTest.dynamicTest(file.name) { checkFile(resourceSet, file) }
        }
    }

    private fun checkFile(resourceSet: ResourceSetImpl, file: File) {
        val startTime = System.currentTimeMillis()
        val modelResource = resourceSet.getResource(URI.createFileURI(file.absolutePath), true)
        val results = StoSquareChecker.scan(modelResource)
        val elapsed = System.currentTimeMillis() - startTime

        totalCheckTimeMs += elapsed
        totalFilesChecked++
        println("Checked ${file.name}: ${results.size} Type-Square instance(s) in $elapsed ms.")

        val violatingResults = results.filter { it.violations.isNotEmpty() }
        assertTrue(violatingResults.isEmpty(), buildString {
            appendLine("${violatingResults.size} of ${results.size} Type-Square instance(s) in ${file.name} violate well-formedness:")
            violatingResults.forEach { result ->
                result.violations.forEach { appendLine("  [${it.rule}] ${it.message}") }
            }
        })
    }
}
