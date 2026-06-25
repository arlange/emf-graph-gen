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
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EFactory
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import picocli.CommandLine
import stomodel.StoFactory
import util.StoConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.random.Random
import kotlin.system.exitProcess

@CommandLine.Command(name = "sto-gentool", mixinStandardHelpOptions = true, version = ["v.0.1"])
class StoChecksum : Callable<Int> {

    private val defaultConfiguration = StoConfiguration()

    @CommandLine.Parameters(index = "0", description = ["output directory"])
    lateinit var output: File

    @CommandLine.Option(
        names = ["-c", "--count"],
        description = ["Number of separate STO model instances (files) to generate (INT)."]
    )
    var count: Int = defaultConfiguration.count

    @CommandLine.Option(
        names = ["-s", "--slots_per_connector"],
        description = ["Number of SlotTypes generated per ConnectorType (INT)."]
    )
    var slotsPerConnector: Int = defaultConfiguration.slotsPerConnector

    @CommandLine.Option(
        names = ["-a", "--cavities_per_slot"],
        description = ["Number of CavityTypes generated per SlotType (INT)."]
    )
    var cavitiesPerSlot: Int = defaultConfiguration.cavitiesPerSlot

    @CommandLine.Option(
        names = ["-d", "--sealed_probability"],
        description = ["Probability 0..1 that a cavity is sealed (DOUBLE)."]
    )
    var sealedProbability: Double = defaultConfiguration.sealedProbability

    @CommandLine.Option(
        names = ["-t", "--can_detect_probability"],
        description = ["Probability 0..1 that a cavity instance/prototype can detect (DOUBLE)."]
    )
    var canDetectProbability: Double = defaultConfiguration.canDetectProbability

    @CommandLine.Option(
        names = ["-m", "--prototypes_per_type"],
        description = ["Number of ConnectorPrototype/PLPrototype trees created per ConnectorType, " +
                "all congruently typed by the same ConnectorType (INT)."]
    )
    var prototypesPerConnectorType: Int = defaultConfiguration.prototypesPerConnectorType

    @CommandLine.Option(
        names = ["-p", "--physical_instances"],
        description = ["Number of HeaderInstance/WiringConnectorInstance trees created per " +
                "ConnectorPrototype, and PluginLocationInstance trees created per PLPrototype (INT)."]
    )
    var physicalInstancesPerPrototype: Int = defaultConfiguration.physicalInstancesPerPrototype

    @CommandLine.Option(
        names = ["-u", "--random_seed"],
        description = ["Random seed for the deterministic random generation algorithm."]
    )
    var userRandomSeed: Int = 0

    override fun call(): Int {
        runWithStoConfig(
            StoConfiguration(
                randomSeed = userRandomSeed,
                outputPath = output.path,
                count = count,
                slotsPerConnector = slotsPerConnector,
                cavitiesPerSlot = cavitiesPerSlot,
                sealedProbability = sealedProbability,
                canDetectProbability = canDetectProbability,
                prototypesPerConnectorType = prototypesPerConnectorType,
                physicalInstancesPerPrototype = physicalInstancesPerPrototype
            )
        )
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(StoChecksum()).execute(*args))
}

fun runWithStoConfig(configuration: StoConfiguration) {
    Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
    Resource.Factory.Registry.INSTANCE.extensionToFactoryMap["sto"] = XMIResourceFactoryImpl()

    val resourceSet = ResourceSetImpl()
    val metamodelPath = createTempStoMetamodelFile()
    val metamodelResource = resourceSet.getResource(URI.createFileURI(metamodelPath.toString()), true)
    val ePackage = metamodelResource.contents[0] as EPackage
    resourceSet.packageRegistry[ePackage.nsURI] = ePackage

    val classes: Map<String, EClass> = ePackage.eClassifiers.filterIsInstance<EClass>().associateBy { it.name }
    val factory: EFactory = ePackage.eFactoryInstance

    Files.createDirectories(Paths.get(configuration.outputPath))

    val random = Random(configuration.randomSeed)

    println("Generating ${configuration.count} STO model instance(s)...")
    val startTime = System.currentTimeMillis()

    for (i in 0 until configuration.count) {
        val bundle = StoFactory(configuration, random).createBundle()

        val connectorTypeObj = bundle.connectorType.build(classes, factory)
        val connectorPrototypeObjs = bundle.connectorPrototypes.map { it.build(classes, factory) }
        val plPrototypeObjs = bundle.plPrototypes.map { it.build(classes, factory) }
        val headerObjs = bundle.headerInstances.map { it.build(classes, factory) }
        val wiringObjs = bundle.wiringInstances.map { it.build(classes, factory) }
        val pluginLocationObjs = bundle.pluginLocationInstances.map { it.build(classes, factory) }

        val modelPath = Paths.get(configuration.outputPath, "instance_$i.sto")
        val resource = resourceSet.createResource(URI.createFileURI(modelPath.toString()))
        resource.contents.add(connectorTypeObj)
        resource.contents.addAll(connectorPrototypeObjs)
        resource.contents.addAll(plPrototypeObjs)
        resource.contents.addAll(headerObjs)
        resource.contents.addAll(wiringObjs)
        resource.contents.addAll(pluginLocationObjs)
        resource.save(null)
    }

    val endTime = System.currentTimeMillis()
    println("Generation Time: ${endTime - startTime} ms")
}

fun createTempStoMetamodelFile(): Path {
    val tempPath = Files.createTempFile("sto", ".ecore")
    Runtime.getRuntime().addShutdownHook(Thread {
        Files.delete(tempPath)
    })

    object {}.javaClass.getResourceAsStream("sto.ecore").use { input ->
        if (input != null)
            BufferedReader(InputStreamReader(input)).use { reader ->
                Files.newBufferedWriter(tempPath).use { writer ->
                    reader.copyTo(writer)
                    writer.flush()
                }
            }
    }

    return tempPath
}
