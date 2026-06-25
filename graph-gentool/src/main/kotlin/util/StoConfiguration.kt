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
package util

data class StoConfiguration(

    val randomSeed: Int = 0,

    val outputPath: String = "./",

    /**
     * Number of separate STO model files to generate.
     */
    val count: Int = 10,

    /**
     * Number of SlotTypes generated per ConnectorType.
     */
    val slotsPerConnector: Int = 3,

    /**
     * Number of CavityTypes generated per SlotType.
     */
    val cavitiesPerSlot: Int = 2,

    /**
     * Probability 0..1 that a CavityType (and therefore its prototypes/instances) is sealed.
     */
    val sealedProbability: Double = 0.3,

    /**
     * Probability 0..1 that a cavity instance/prototype can detect.
     */
    val canDetectProbability: Double = 0.5,

    /**
     * Number of ConnectorPrototype and PLPrototype trees generated per ConnectorType,
     * all congruently typed by the same ConnectorType (Figure 5's well-formed
     * "multiple prototypes, same owner" scenario).
     */
    val prototypesPerConnectorType: Int = 2,

    /**
     * Number of HeaderInstance and WiringConnectorInstance trees generated per
     * ConnectorPrototype, and PluginLocationInstance trees generated per PLPrototype.
     */
    val physicalInstancesPerPrototype: Int = 2

) {
    init {
        assert(count > 0)
        assert(slotsPerConnector > 0)
        assert(cavitiesPerSlot > 0)
        assert(sealedProbability in 0.0..1.0)
        assert(canDetectProbability in 0.0..1.0)
        assert(prototypesPerConnectorType > 0)
        assert(physicalInstancesPerPrototype >= 0)
    }
}
