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
 * Prototypes mirror the Type tree but live in their own containment trees
 * (ConnectorPrototype, PLPrototype) and cross-link back to the Type tree via "type".
 * Whether a cavity becomes a sealed or unsealed prototype is decided by the
 * originating CavityType.isSealed flag.
 */
class CavityBPrototype(val type: CavityType, val cavityNumber: Int, val canDetect: Boolean) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavityBPrototype")
        obj.setAttr("isSealed", false)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("canDetect", canDetect)
        obj.setRef("type", type.eobj!!)
        eobj = obj
        return obj
    }
}

class CavitySealedBPrototype(val type: CavityType, val cavityNumber: Int, val sealMaterial: String) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavitySealedBPrototype")
        obj.setAttr("isSealed", true)
        obj.setAttr("cavityNumber", cavityNumber)
        obj.setAttr("sealMaterial", sealMaterial)
        obj.setRef("type", type.eobj!!)
        eobj = obj
        return obj
    }
}

class SlotBPrototype(
    val type: SlotType,
    val cavityPrototypes: MutableList<CavityBPrototype> = LinkedList(),
    val sealedCavityPrototypes: MutableList<CavitySealedBPrototype> = LinkedList()
) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "SlotBPrototype")
        obj.setRef("type", type.eobj!!)
        val cavities = obj.children("cavitybprototype")
        cavityPrototypes.forEach { cavities.add(it.build(classes, factory)) }
        val sealedCavities = obj.children("cavitysealedbprototype")
        sealedCavityPrototypes.forEach { sealedCavities.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}

class CavityCPrototype(val type: CavityType, val canDetect: Boolean) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavityCPrototype")
        obj.setAttr("isSealed", false)
        obj.setAttr("canDetect", canDetect)
        obj.setRef("type", type.eobj!!)
        eobj = obj
        return obj
    }
}

class CavitySealedCPrototype(val type: CavityType, val sealMaterial: String) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavitySealedCPrototype")
        obj.setAttr("isSealed", true)
        obj.setAttr("sealMaterial", sealMaterial)
        obj.setRef("type", type.eobj!!)
        eobj = obj
        return obj
    }
}

class SlotCPrototype(
    val type: SlotType,
    val cavityPrototypes: MutableList<CavityCPrototype> = LinkedList(),
    val sealedCavityPrototypes: MutableList<CavitySealedCPrototype> = LinkedList()
) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "SlotCPrototype")
        obj.setRef("type", type.eobj!!)
        // Note: the metamodel reuses the feature name "cavitybprototype" for the CavityCPrototype containment.
        val cavities = obj.children("cavitybprototype")
        cavityPrototypes.forEach { cavities.add(it.build(classes, factory)) }
        val sealedCavities = obj.children("cavitysealedcprototype")
        sealedCavityPrototypes.forEach { sealedCavities.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}

class ConnectorPrototype(
    val type: ConnectorType,
    val slotPrototypes: MutableList<SlotBPrototype> = LinkedList()
) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "ConnectorPrototype")
        obj.setRef("type", type.eobj!!)
        val slots = obj.children("slotbprototype")
        slotPrototypes.forEach { slots.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}

class PLPrototype(
    val type: ConnectorType,
    val slotPrototypes: MutableList<SlotCPrototype> = LinkedList()
) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "PLPrototype")
        obj.setRef("type", type.eobj!!)
        val slots = obj.children("slotcprototype")
        slotPrototypes.forEach { slots.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}
