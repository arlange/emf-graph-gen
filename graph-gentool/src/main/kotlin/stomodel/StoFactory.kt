/**
 * Copyright 2023 Karl Kegel
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
package stomodel

import util.StoConfiguration
import java.util.LinkedList
import kotlin.random.Random

/**
 * One bundle is a self-contained, fully cross-linked STO model:
 * a Type tree, the two Prototype trees derived from it (Connector/PL), and a
 * configurable number of physical instance trees (Header/WiringConnector/PluginLocation).
 */
class StoBundle(
    val connectorType: ConnectorType,
    val connectorPrototypes: List<ConnectorPrototype>,
    val plPrototypes: List<PLPrototype>,
    val headerInstances: List<HeaderInstance>,
    val wiringInstances: List<WiringConnectorInstance>,
    val pluginLocationInstances: List<PluginLocationInstance>
)

class StoFactory(private val conf: StoConfiguration, private val random: Random) {

    private val materials = listOf("Silicone", "Rubber", "Foam", "PVC")
    private var cavityCounter = 0

    fun createBundle(): StoBundle {
        val connectorType = buildConnectorType()

        // Multiple prototypes congruently typed by the same ConnectorType
        // (Figure 5's well-formed scenario): each one independently walks
        // connectorType.slotTypes, so none of them can cross into another type's parts.
        val connectorPrototypes = (0 until conf.prototypesPerConnectorType).map {
            buildConnectorPrototype(connectorType)
        }
        val plPrototypes = (0 until conf.prototypesPerConnectorType).map {
            buildPLPrototype(connectorType)
        }

        val headerInstances = connectorPrototypes.flatMap { cp ->
            (0 until conf.physicalInstancesPerPrototype).map { buildHeaderInstance(cp) }
        }
        val wiringInstances = connectorPrototypes.flatMap { cp ->
            (0 until conf.physicalInstancesPerPrototype).map { buildWiringConnectorInstance(cp) }
        }
        val pluginLocationInstances = plPrototypes.flatMap { pl ->
            (0 until conf.physicalInstancesPerPrototype).map { buildPluginLocationInstance(pl) }
        }

        return StoBundle(
            connectorType, connectorPrototypes, plPrototypes,
            headerInstances, wiringInstances, pluginLocationInstances
        )
    }

    private fun nextCavityNumber(): Int = cavityCounter++

    private fun randomMaterial(): String = materials[random.nextInt(materials.size)]

    private fun buildConnectorType(): ConnectorType {
        val slotTypes = (0 until conf.slotsPerConnector).map { buildSlotType() }.toMutableList()
        return ConnectorType(slotTypes)
    }

    private fun buildSlotType(): SlotType {
        val cavityTypes = (0 until conf.cavitiesPerSlot).map {
            CavityType(random.nextDouble() < conf.sealedProbability)
        }.toMutableList()
        return SlotType(cavityTypes)
    }

    private fun buildConnectorPrototype(connectorType: ConnectorType): ConnectorPrototype {
        val slotPrototypes = connectorType.slotTypes.map { buildSlotBPrototype(it) }.toMutableList()
        return ConnectorPrototype(connectorType, slotPrototypes)
    }

    private fun buildSlotBPrototype(slotType: SlotType): SlotBPrototype {
        val cavityPrototypes = LinkedList<CavityBPrototype>()
        val sealedCavityPrototypes = LinkedList<CavitySealedBPrototype>()
        slotType.cavityTypes.forEach { cavityType ->
            if (cavityType.isSealed) {
                sealedCavityPrototypes.add(CavitySealedBPrototype(cavityType, nextCavityNumber(), randomMaterial()))
            } else {
                cavityPrototypes.add(CavityBPrototype(cavityType, nextCavityNumber(), random.nextBoolean()))
            }
        }
        return SlotBPrototype(slotType, cavityPrototypes, sealedCavityPrototypes)
    }

    private fun buildPLPrototype(connectorType: ConnectorType): PLPrototype {
        val slotPrototypes = connectorType.slotTypes.map { buildSlotCPrototype(it) }.toMutableList()
        return PLPrototype(connectorType, slotPrototypes)
    }

    private fun buildSlotCPrototype(slotType: SlotType): SlotCPrototype {
        val cavityPrototypes = LinkedList<CavityCPrototype>()
        val sealedCavityPrototypes = LinkedList<CavitySealedCPrototype>()
        slotType.cavityTypes.forEach { cavityType ->
            if (cavityType.isSealed) {
                sealedCavityPrototypes.add(CavitySealedCPrototype(cavityType, randomMaterial()))
            } else {
                cavityPrototypes.add(CavityCPrototype(cavityType, random.nextBoolean()))
            }
        }
        return SlotCPrototype(slotType, cavityPrototypes, sealedCavityPrototypes)
    }

    private fun buildHeaderInstance(connectorPrototype: ConnectorPrototype): HeaderInstance {
        val slotInstances = connectorPrototype.slotPrototypes.map { slotProto ->
            val cavityInstances = slotProto.cavityPrototypes.map {
                CavityAInstance(it, nextCavityNumber(), random.nextDouble() < conf.canDetectProbability)
            }.toMutableList()
            val sealedCavityInstances = slotProto.sealedCavityPrototypes.map {
                CavitySealedAInstance(it, nextCavityNumber(), randomMaterial())
            }.toMutableList()
            SlotAInstane(slotProto, cavityInstances, sealedCavityInstances)
        }.toMutableList()
        return HeaderInstance(connectorPrototype, slotInstances)
    }

    private fun buildWiringConnectorInstance(connectorPrototype: ConnectorPrototype): WiringConnectorInstance {
        val slotInstances = connectorPrototype.slotPrototypes.map { slotProto ->
            val cavityInstances = slotProto.cavityPrototypes.map {
                CavityBInstance(it, nextCavityNumber(), random.nextDouble() < conf.canDetectProbability)
            }.toMutableList()
            val sealedCavityInstances = slotProto.sealedCavityPrototypes.map {
                CavitySealedBInstance(it, nextCavityNumber(), randomMaterial())
            }.toMutableList()
            SlotBInstance(cavityInstances, sealedCavityInstances)
        }.toMutableList()
        return WiringConnectorInstance(connectorPrototype, slotInstances)
    }

    private fun buildPluginLocationInstance(plPrototype: PLPrototype): PluginLocationInstance {
        val slotInstances = plPrototype.slotPrototypes.map { slotProto ->
            val cavityInstances = slotProto.cavityPrototypes.map {
                PluginLocationCavityCInstance(it, random.nextDouble() < conf.canDetectProbability)
            }.toMutableList()
            val sealedCavityInstances = slotProto.sealedCavityPrototypes.map {
                PluginLocationSealedCavityCInstance(it, randomMaterial())
            }.toMutableList()
            PluginLocationSlotCInstance(slotProto, cavityInstances, sealedCavityInstances)
        }.toMutableList()
        return PluginLocationInstance(plPrototype, slotInstances)
    }
}
