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
 * Types form the top-level containment tree: ConnectorType -> SlotType -> CavityType.
 * Prototypes and instances cross-link back into this tree via single-valued opposite references.
 */
class CavityType(val isSealed: Boolean) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "CavityType")
        obj.setAttr("isSealed", isSealed)
        eobj = obj
        return obj
    }
}

class SlotType(val cavityTypes: MutableList<CavityType> = LinkedList()) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "SlotType")
        val cavities = obj.children("cavitytype")
        cavityTypes.forEach { cavities.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}

class ConnectorType(val slotTypes: MutableList<SlotType> = LinkedList()) {
    var eobj: EObject? = null

    fun build(classes: Map<String, EClass>, factory: EFactory): EObject {
        val obj = create(classes, factory, "ConnectorType")
        val slots = obj.children("slotType")
        slotTypes.forEach { slots.add(it.build(classes, factory)) }
        eobj = obj
        return obj
    }
}
