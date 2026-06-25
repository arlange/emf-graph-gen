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

fun create(classes: Map<String, EClass>, factory: EFactory, className: String): EObject {
    return factory.create(classes[className]!!)
}

fun EObject.setAttr(name: String, value: Any?) {
    eSet(eClass().getEStructuralFeature(name), value)
}

fun EObject.setRef(name: String, target: EObject) {
    eSet(eClass().getEStructuralFeature(name), target)
}

@Suppress("UNCHECKED_CAST")
fun EObject.children(name: String): java.util.List<EObject> {
    return eGet(eClass().getEStructuralFeature(name)) as java.util.List<EObject>
}
