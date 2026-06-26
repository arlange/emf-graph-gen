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
package stocheck

import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource

/**
 * One failed well-formedness rule for an STO "Type-Square" instance
 * (cf. §2.2 of the MULTI Vector Industry Challenge paper).
 */
data class StoViolation(val rule: String, val message: String)

/**
 * The result of checking one Type-Square instance found while scanning a model
 * (cf. [StoSquareChecker.scan]). [violations] is empty if the square is well-formed.
 */
data class StoSquareScanResult(
    val topType: EObject,
    val bottomType: EObject,
    val topInstance: EObject,
    val bottomInstance: EObject,
    val violations: List<StoViolation>
)

/**
 * Checks a single M1-level "Type-Square" instance for structural well-formedness.
 *
 * A Type-Square spans two adjacent abstraction levels (e.g. Type/Prototype) and two
 * adjacent owner/part roles (e.g. Connector/Slot). [topType] and [bottomType] are the
 * M1 objects at the upper abstraction level (owner and part respectively); [topInstance]
 * and [bottomInstance] are their M1 counterparts one level down, e.g.:
 *   topType = a ConnectorType instance, bottomType = one of its SlotType parts,
 *   topInstance = a ConnectorPrototype instance, bottomInstance = one of its SlotBPrototype parts.
 *
 * The three feature-name parameters identify, by structural feature name, how to
 * navigate the M2-level metamodel along the edges of that same square, e.g. for the
 * Connector/Slot square above:
 *   [topTypeToTopInstanceFeature]      = "connectorProtInstance" (ConnectorType -> ConnectorPrototype)
 *   [topTypeToBottomTypeFeature]       = "slotType"               (ConnectorType -> SlotType)
 *   [bottomTypeToBottomInstanceFeature] = "slotBInstance"          (SlotType -> SlotBPrototype)
 *
 * Each rule is implemented by its own private check*() function returning the
 * violations it found (empty if the rule holds). [check] runs the rules in order and
 * stops at the first one that reports a violation - later rules generally presuppose
 * that earlier, more foundational ones (e.g. navigability) already hold, so there is
 * no point continuing once one has failed. To add a new rule, write a new check*()
 * function with the same shape and add it to the list inside [check].
 */
class StoSquareChecker(
    private val topType: EObject,
    private val bottomType: EObject,
    private val topInstance: EObject,
    private val bottomInstance: EObject,
    private val topTypeToTopInstanceFeature: String,
    private val topTypeToBottomTypeFeature: String,
    private val bottomTypeToBottomInstanceFeature: String
) {

    fun check(): List<StoViolation> {
        val rules: List<() -> List<StoViolation>> = listOf(
            ::checkBottomInstanceIsPartOfTopInstance,
            ::checkTypingRelationshipCongruence
            // Next steps go here, e.g.:
            // ::checkBottomInstanceTypedByBottomType,
            // ::checkTopInstanceTypedByTopType,
            // ::checkPartTypingBijection
        )
        for (rule in rules) {
            val violations = rule()
            if (violations.isNotEmpty()) return violations
        }
        return emptyList()
    }

    /**
     * Structural preconditions for this square: that the type navigation from
     * bottomInstance/topInstance resolves to at most one object, and that
     * bottomInstance/bottomType are each contained at most once by their parent. Not
     * part of [check] - [scan] runs this for every square in the model as its own
     * phase, before checking any STO conformance, and aborts before that second phase
     * if any square's preconditions are violated. All four are evaluated and
     * collected (they're independent of each other, not a layered chain of
     * assumptions).
     */
    fun checkPreconditions(): List<StoViolation> {
        val violations = mutableListOf<StoViolation>()
        violations += checkTypedAtMostOnce(bottomInstance, "bottomInstance")
        violations += checkTypedAtMostOnce(topInstance, "topInstance")
        violations += checkContainedAtMostOnce(bottomInstance, "bottomInstance")
        violations += checkContainedAtMostOnce(bottomType, "bottomType")
        return violations
    }

    /**
     * [obj]'s type-navigation feature must resolve to at most one object. Checked by
     * inspecting the raw feature value directly (rather than the [typeOf] helper's
     * `as? EObject` cast, which would silently return null - not flag a problem - if
     * the feature turned out to hold more than one value).
     */
    private fun checkTypedAtMostOnce(obj: EObject, role: String): List<StoViolation> {
        val featureName = typeFeatureNameByClass[obj.eClass().name] ?: "type"
        val feature = obj.eClass().getEStructuralFeature(featureName) ?: return emptyList()
        val types = asEObjectList(obj.eGet(feature))
        return if (types.size <= 1) {
            emptyList()
        } else {
            listOf(
                StoViolation(
                    rule = "precondition-type",
                    message = "${describe(obj)} ($role) is connected to ${types.size} types via " +
                            "'$featureName', expected at most one"
                )
            )
        }
    }

    /**
     * [child]'s containing feature must list it at most once. [child].eContainer()
     * itself can never disagree (EMF maintains it as a single backpointer), so this
     * instead inspects the parent's raw feature value directly, which can in
     * principle hold the same child more than once.
     */
    private fun checkContainedAtMostOnce(child: EObject, role: String): List<StoViolation> {
        val container = child.eContainer() ?: return emptyList()
        val feature = child.eContainingFeature() ?: return emptyList()
        val occurrences = asEObjectList(container.eGet(feature)).count { it === child }
        return if (occurrences <= 1) {
            emptyList()
        } else {
            listOf(
                StoViolation(
                    rule = "precondition-containment",
                    message = "${describe(child)} ($role) is contained $occurrences times by " +
                            "${describe(container)}, expected at most once"
                )
            )
        }
    }

    /**
     * Normalizes the result of an eGet() call - a single EObject for a single-valued
     * feature, or a Collection for a many-valued one - into a uniform List.
     */
    private fun asEObjectList(value: Any?): List<EObject> = when (value) {
        is EObject -> listOf(value)
        is Collection<*> -> value.filterIsInstance<EObject>()
        else -> emptyList()
    }

    /**
     * Foundational check: bottomInstance must actually be a composite part of
     * topInstance (its eContainer), i.e. reachable via a containment relationship -
     * not just cross-linked or unrelated. This is a prerequisite for the typing
     * congruence and bijection rules that build on top of it.
     */
    private fun checkBottomInstanceIsPartOfTopInstance(): List<StoViolation> {
        return if (bottomInstance.eContainer() === topInstance) {
            emptyList()
        } else {
            listOf(
                StoViolation(
                    rule = "composition",
                    message = "${describe(bottomInstance)} is not a containment child of ${describe(topInstance)}"
                )
            )
        }
    }

    /**
     * Typing relationship congruence (§2.2.1): the Type-Square must commute. Going up
     * from bottomInstance to its container (topInstance) and then to that container's
     * type must reach the same type-level object as going up from bottomInstance to
     * its own type and then to that type's container:
     *   type-of(container-of(bottomInstance)) == container-of(type-of(bottomInstance))
     */
    private fun checkTypingRelationshipCongruence(): List<StoViolation> {
        val viaContainerThenType = bottomInstance.eContainer()?.let { typeOf(it) }
        val viaTypeThenContainer = typeOf(bottomInstance)?.eContainer()

        return if (viaContainerThenType === viaTypeThenContainer) {
            emptyList()
        } else {
            listOf(
                StoViolation(
                    rule = "typing-congruence",
                    message = "Typing relationship of ${describe(bottomInstance)} is not congruent with its " +
                            "composition structure: type-of(container) = " +
                            "${viaContainerThenType?.let { describe(it) } ?: "null"}, but " +
                            "container-of(type) = ${viaTypeThenContainer?.let { describe(it) } ?: "null"}"
                )
            )
        }
    }

    private fun describe(obj: EObject): String = describeStatic(obj)

    companion object {

        // SlotAInstane and PluginLocationSealedCavityCInstance are the only two
        // classes in STO.ecore whose upward "type" reference isn't actually named
        // "type" (they're named "slotbprototype" and "cavitysealedcprototype"
        // respectively). Hardcoded here since it's a fixed, known quirk of this one
        // metamodel, not something worth detecting generically.
        private val typeFeatureNameByClass = mapOf(
            "SlotAInstane" to "slotbprototype",
            "PluginLocationSealedCavityCInstance" to "cavitysealedcprototype"
        )

        /**
         * Navigates an M1 object to its type-level counterpart. Almost every
         * Prototype/Instance class in STO.ecore names this reference "type"; the two
         * exceptions ([typeFeatureNameByClass]) are hardcoded since they're a fixed,
         * known quirk of this specific metamodel.
         */
        fun typeOf(obj: EObject): EObject? {
            val featureName = typeFeatureNameByClass[obj.eClass().name] ?: "type"
            val feature = obj.eClass().getEStructuralFeature(featureName) ?: return null
            return obj.eGet(feature) as? EObject
        }

        fun describeStatic(obj: EObject): String = "${obj.eClass().name}@${System.identityHashCode(obj)}"

        private data class SquareKey(
            val topTypeClass: String,
            val bottomTypeClass: String,
            val topInstanceClass: String,
            val bottomInstanceClass: String
        )

        private data class SquareFeatures(
            val topTypeToTopInstanceFeature: String,
            val topTypeToBottomTypeFeature: String,
            val bottomTypeToBottomInstanceFeature: String
        )

        // The full set of "Type-Square" instances present in STO.ecore (cf. Figure 4
        // of the paper), hardcoded against the metamodel's known, fixed structure -
        // there is no need to algorithmically detect this pattern at runtime.
        private val knownSquares: Map<SquareKey, SquareFeatures> = mapOf(
            SquareKey("ConnectorType", "SlotType", "ConnectorPrototype", "SlotBPrototype") to
                SquareFeatures("connectorProtInstance", "slotType", "slotBInstance"),
            SquareKey("ConnectorType", "SlotType", "PLPrototype", "SlotCPrototype") to
                SquareFeatures("pluginPInstance", "slotType", "slotCInstance"),
            SquareKey("SlotType", "CavityType", "SlotBPrototype", "CavityBPrototype") to
                SquareFeatures("slotBInstance", "cavitytype", "cavityBInstance"),
            SquareKey("SlotType", "CavityType", "SlotBPrototype", "CavitySealedBPrototype") to
                SquareFeatures("slotBInstance", "cavitytype", "cavityBSealedInstance"),
            SquareKey("SlotType", "CavityType", "SlotCPrototype", "CavityCPrototype") to
                SquareFeatures("slotCInstance", "cavitytype", "cavityCInstance"),
            SquareKey("SlotType", "CavityType", "SlotCPrototype", "CavitySealedCPrototype") to
                SquareFeatures("slotCInstance", "cavitytype", "cavityCSealedInstance"),
            SquareKey("ConnectorPrototype", "SlotBPrototype", "HeaderInstance", "SlotAInstane") to
                SquareFeatures("headerinstance", "slotbprototype", "slotAinstane"),
            SquareKey("SlotBPrototype", "CavityBPrototype", "SlotAInstane", "CavityAInstance") to
                SquareFeatures("slotAinstane", "cavitybprototype", "cavityAinstance"),
            SquareKey("SlotBPrototype", "CavitySealedBPrototype", "SlotAInstane", "CavitySealedAInstance") to
                SquareFeatures("slotAinstane", "cavitysealedbprototype", "cavitySealedAInstnce"),
            SquareKey("PLPrototype", "SlotCPrototype", "PluginLocationInstance", "PluginLocationSlotCInstance") to
                SquareFeatures("pluginLocationInstance", "slotcprototype", "pluginLocationSlotCInstance"),
            SquareKey("SlotCPrototype", "CavityCPrototype", "PluginLocationSlotCInstance", "PluginLocationCavityCInstance") to
                SquareFeatures("pluginLocationSlotCInstance", "cavitybprototype", "pluginLocationCavityCInstance"),
            SquareKey("SlotCPrototype", "CavitySealedCPrototype", "PluginLocationSlotCInstance", "PluginLocationSealedCavityCInstance") to
                SquareFeatures("pluginLocationSlotCInstance", "cavitysealedcprototype", "pluginLocationSealedCavityCInstance")
        )

        /**
         * Builds a [StoSquareChecker] for the given four M1 objects, looking up the
         * three navigation feature names from the hardcoded STO.ecore catalog above
         * instead of requiring the caller to know and pass them in.
         */
        fun of(topType: EObject, bottomType: EObject, topInstance: EObject, bottomInstance: EObject): StoSquareChecker {
            val key = SquareKey(
                topType.eClass().name, bottomType.eClass().name,
                topInstance.eClass().name, bottomInstance.eClass().name
            )
            val features = knownSquares[key]
                ?: throw IllegalArgumentException("Not a known STO Type-Square: $key")
            return StoSquareChecker(
                topType, bottomType, topInstance, bottomInstance,
                features.topTypeToTopInstanceFeature,
                features.topTypeToBottomTypeFeature,
                features.bottomTypeToBottomInstanceFeature
            )
        }

        // Every (topInstanceClass, bottomInstanceClass) pair in knownSquares happens to
        // be unique on its own, so it can serve as a secondary index for scan(): given
        // any object and its container, this tells us whether that pair is the
        // bottomInstance/topInstance of some known square, without first knowing the
        // type-level objects.
        private val squaresByInstanceClasses: Map<Pair<String, String>, SquareKey> =
            knownSquares.keys.associateBy { it.topInstanceClass to it.bottomInstanceClass }

        private class PreparedSquare(
            val topType: EObject,
            val bottomType: EObject,
            val topInstance: EObject,
            val bottomInstance: EObject,
            val checker: StoSquareChecker
        )

        /**
         * Walks the whole containment tree under [root] and checks every Type-Square
         * instance it finds (matched purely by the containment class pairs in the
         * catalog above), deriving topType/bottomType automatically via [typeOf].
         *
         * Runs in two phases: first, every square's structural preconditions
         * ([StoSquareChecker.checkPreconditions]) are checked, in full, before any
         * STO conformance check starts; if any square fails its preconditions, that's
         * returned immediately and the conformance phase never runs. Only once every
         * square's preconditions hold does the second phase check actual STO
         * conformance ([StoSquareChecker.check]) for each of them.
         */
        fun scan(root: EObject): List<StoSquareScanResult> {
            val prepared = mutableListOf<PreparedSquare>()
            val unresolved = mutableListOf<StoSquareScanResult>()

            root.eAllContents().forEach { bottomInstance ->
                val topInstance = bottomInstance.eContainer() ?: return@forEach
                val key = squaresByInstanceClasses[topInstance.eClass().name to bottomInstance.eClass().name]
                    ?: return@forEach

                val topType = typeOf(topInstance)
                val bottomType = typeOf(bottomInstance)
                if (topType == null || bottomType == null) {
                    unresolved.add(
                        StoSquareScanResult(
                            topType ?: topInstance, bottomType ?: bottomInstance, topInstance, bottomInstance,
                            listOf(
                                StoViolation(
                                    rule = "navigability",
                                    message = "Cannot resolve type of ${describeStatic(topInstance)} or " +
                                            "${describeStatic(bottomInstance)}"
                                )
                            )
                        )
                    )
                    return@forEach
                }

                val features = knownSquares[key]!!
                val checker = StoSquareChecker(
                    topType, bottomType, topInstance, bottomInstance,
                    features.topTypeToTopInstanceFeature,
                    features.topTypeToBottomTypeFeature,
                    features.bottomTypeToBottomInstanceFeature
                )
                prepared.add(PreparedSquare(topType, bottomType, topInstance, bottomInstance, checker))
            }

            // Phase 1: preconditions for every square, before any conformance check starts.
            val preconditionViolations = prepared.mapNotNull { sq ->
                val violations = sq.checker.checkPreconditions()
                if (violations.isEmpty()) null
                else StoSquareScanResult(sq.topType, sq.bottomType, sq.topInstance, sq.bottomInstance, violations)
            }
            if (preconditionViolations.isNotEmpty()) {
                return unresolved + preconditionViolations
            }

            // Phase 2: actual STO conformance, only reached if every precondition held.
            val conformanceResults = prepared.map { sq ->
                StoSquareScanResult(sq.topType, sq.bottomType, sq.topInstance, sq.bottomInstance, sq.checker.check())
            }
            return unresolved + conformanceResults
        }

        /** Runs [scan] over every top-level root object in [resource]. */
        fun scan(resource: Resource): List<StoSquareScanResult> = resource.contents.flatMap { scan(it) }
    }
}
