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
import picocli.CommandLine
import stocheck.StoSquareChecker
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "sto-checktool", mixinStandardHelpOptions = true, version = ["v.0.1"])
class StoCheckCommand : Callable<Int> {

    @CommandLine.Parameters(
        index = "0..*",
        description = ["STO model instance file(s) to check (e.g. .sto/.xmi files produced by sto-gentool)"]
    )
    lateinit var inputs: List<File>

    override fun call(): Int {
        Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["sto"] = XMIResourceFactoryImpl()
        Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()

        val resourceSet = ResourceSetImpl()
        val metamodelResource = resourceSet.getResource(URI.createFileURI(createTempStoMetamodelFile().toString()), true)
        val ePackage = metamodelResource.contents[0] as EPackage
        resourceSet.packageRegistry[ePackage.nsURI] = ePackage

        var totalSquares = 0
        var totalViolating = 0
        val overallStart = System.currentTimeMillis()

        for (input in inputs) {
            val fileStart = System.currentTimeMillis()
            val loadStart = System.currentTimeMillis()
            val modelResource = resourceSet.getResource(URI.createFileURI(input.absolutePath), true)
            val loadElapsed = System.currentTimeMillis() - loadStart
            val scanStart = System.currentTimeMillis()
            val results = StoSquareChecker.scan(modelResource)
            val scanElapsed = System.currentTimeMillis() - scanStart
            val elapsed = System.currentTimeMillis() - fileStart

            val violatingResults = results.filter { it.violations.isNotEmpty() }
            totalSquares += results.size
            totalViolating += violatingResults.size

            println("Checked ${results.size} Type-Square instance(s) in ${input.path} (${elapsed} ms; load=${loadElapsed} ms, scan=${scanElapsed} ms).")
            for (result in violatingResults) {
                for (violation in result.violations) {
                    println("  [${violation.rule}] ${violation.message}")
                }
            }
        }

        val overallElapsed = System.currentTimeMillis() - overallStart
        println(
            "Checked ${inputs.size} file(s), $totalSquares Type-Square instance(s) total, in $overallElapsed ms " +
                    "(avg ${if (inputs.isEmpty()) 0 else overallElapsed / inputs.size} ms/file)."
        )

        return if (totalViolating == 0) {
            println("All Type-Squares are well-formed.")
            0
        } else {
            println("$totalViolating of $totalSquares Type-Square instance(s) violate well-formedness.")
            1
        }
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(StoCheckCommand()).execute(*args))
}
