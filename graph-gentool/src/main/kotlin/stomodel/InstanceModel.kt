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

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EFactory
import org.eclipse.emf.ecore.EObject
import java.util.LinkedList

/**
 * Physical instances (Header, WiringConnector, PluginLocation) are themselves
 * containment roots that cross-link back into a Prototype tree via "type".
 * None of these objects are referenced elsewhere, so they don't need to expose
 * their built EObject.
 */
class CavityAInstance(val type: CavityBPrototype, val cavityNumber: Int, val canDetect: Boolean) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavityAInstance")
        obj.setAttr("isSealed", false)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("canDetect", canDetect)
        obj.setRef("type", type.eobj!!)
        return obj
    }
}

class CavitySealedAInstance(val type: CavitySealedBPrototype, val cavityNumber: Int, val sealMaterial: String) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavitySealedAInstance")
        obj.setAttr("isSealed", true)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("sealMaterial", sealMaterial)
        obj.setRef("type", type.eobj!!)
        return obj
    }
}

class SlotAInstane(
    val slotbprototype: SlotBPrototype,
    val cavityInstances: MutableList<CavityAInstance> = LinkedList(),
    val sealedCavityInstances: MutableList<CavitySealedAInstance> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "SlotAInstane")
        obj.setRef("slotbprototype", slotbprototype.eobj!!)
        val cavities = obj.children("cavityainstance")
        cavityInstances.forEach { cavities.add(it.build(classes, factory)) }
        val sealedCavities = obj.children("cavitysealedainstnce")
        sealedCavityInstances.forEach { sealedCavities.add(it.build(classes, factory)) }
        return obj
    }
}

class HeaderInstance(
    val type: ConnectorPrototype,
    val slotInstances: MutableList<SlotAInstane> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "HeaderInstance")
        obj.setRef("type", type.eobj!!)
        val slots = obj.children("slotainstane")
        slotInstances.forEach { slots.add(it.build(classes, factory)) }
        return obj
    }
}

class CavityBInstance(val type: CavityBPrototype, val cavityNumber: Int, val canDetect: Boolean) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavityBInstance")
        obj.setAttr("isSealed", false)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("canDetect", canDetect)
        obj.setRef("type", type.eobj!!)
        return obj
    }
}

class CavitySealedBInstance(val type: CavitySealedBPrototype, val cavityNumber: Int, val sealMaterial: String) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavitySealedBInstance")
        obj.setAttr("isSealed", true)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("sealMaterial", sealMaterial)
        obj.setRef("type", type.eobj!!)
        return obj
    }
}

class SlotBInstance(
    val cavityInstances: MutableList<CavityBInstance> = LinkedList(),
    val sealedCavityInstances: MutableList<CavitySealedBInstance> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "SlotBInstance")
        val cavities = obj.children("cavitybinstance")
        cavityInstances.forEach { cavities.add(it.build(classes, factory)) }
        val sealedCavities = obj.children("cavitysealedbinstance")
        sealedCavityInstances.forEach { sealedCavities.add(it.build(classes, factory)) }
        return obj
    }
}

class WiringConnectorInstance(
    val type: ConnectorPrototype,
    val slotInstances: MutableList<SlotBInstance> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "WiringConnectorInstance")
        obj.setRef("type", type.eobj!!)
        val slots = obj.children("slotbinstance")
        slotInstances.forEach { slots.add(it.build(classes, factory)) }
        return obj
    }
}

class PluginLocationCavityCInstance(val type: CavityCPrototype, val canDetect: Boolean) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "PluginLocationCavityCInstance")
        obj.setAttr("isSealed", false)
        obj.setAttr("canDetect", canDetect)
        obj.setRef("type", type.eobj!!)
        return obj
    }
}

class PluginLocationSealedCavityCInstance(val type: CavitySealedCPrototype, val sealMaterial: String) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "PluginLocationSealedCavityCInstance")
        obj.setAttr("isSealed", true)
        obj.setAttr("sealMaterial", sealMaterial)
        obj.setRef("cavitysealedcprototype", type.eobj!!)
        return obj
    }
}

class PluginLocationSlotCInstance(
    val type: SlotCPrototype,
    val cavityInstances: MutableList<PluginLocationCavityCInstance> = LinkedList(),
    val sealedCavityInstances: MutableList<PluginLocationSealedCavityCInstance> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "PluginLocationSlotCInstance")
        obj.setRef("type", type.eobj!!)
        val cavities = obj.children("pluginlocationcavitycinstance")
        cavityInstances.forEach { cavities.add(it.build(classes, factory)) }
        val sealedCavities = obj.children("pluginlocationsealedcavitycinstance")
        sealedCavityInstances.forEach { sealedCavities.add(it.build(classes, factory)) }
        return obj
    }
}

class PluginLocationInstance(
    val type: PLPrototype,
    val slotInstances: MutableList<PluginLocationSlotCInstance> = LinkedList()
) {
    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "PluginLocationInstance")
        obj.setRef("type", type.eobj!!)
        val slots = obj.children("pluginlocationslotcinstance")
        slotInstances.forEach { slots.add(it.build(classes, factory)) }
        return obj
    }
}
