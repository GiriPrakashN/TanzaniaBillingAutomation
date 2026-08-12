package com.navis.external.framework.AbstractExtensionCallback

import com.navis.external.framework.AbstractExtensionCallback
import com.navis.external.framework.entity.EEntityView
import com.navis.external.framework.util.EFieldChange
import com.navis.external.framework.util.EFieldChangesView
import com.navis.framework.business.Roastery
import com.navis.framework.persistence.Entity
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.argo.business.api.ServicesManager
import com.navis.services.business.rules.EventType
import com.navis.framework.util.LogUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger

/**
 * BillableEventBusinessRuleDispatcher
 *
 * Central dispatcher for billable-event business rules.
 *
 * Current business rule:
 * Export Vessel Rollover
 *
 * The rollover may be represented by N4 at different entity levels:
 *
 * 1. UnitFacilityVisit
 *    - ufvIntendedObCv
 *    - ufvActualObCv
 *
 * 2. Unit
 *    - unitRouting
 *
 * Current Event:
 * UNIT_EXPORT_VESSEL_REROUTE_YARD
 *
 * IMPORTANT:
 * UNIT_EXPORT_VESSEL_REROUTE_YARD is currently treated as a
 * NON-BILLABLE derived event.
 *
 * Therefore, duplicate-event checking is NOT performed when
 * recording this event.
 *
 * Duplicate-event checking is reserved for BILLABLE events.
 */
class BillableEventBusinessRuleDispatcher
        extends AbstractExtensionCallback {

    private static final Logger LOGGER =
            Logger.getLogger(
                    BillableEventBusinessRuleDispatcher.class
            )

    private static final String TRIGGER_UPDATE =
            "UPDATE"

    private static final String EXPORT_UNIT_CATEGORY =
            "EXPRT"

    private static final String FIELD_INTENDED_OB_CV =
            "ufvIntendedObCv"

    private static final String FIELD_ACTUAL_OB_CV =
            "ufvActualObCv"

    private static final String FIELD_UNIT_ROUTING =
            "unitRouting"

    private static final String EXPORT_ROLLOVER_EVENT =
            "UNIT_EXPORT_VESSEL_REROUTE_YARD"

    private static final String EXPORT_ROLLOVER_NOTE =
            "Export Vessel RollOver detected."

    public void execute(Map inMapParam) {

        LogUtils.setLogLevel(
                this.class,
                Level.INFO
        )

        LOGGER.info(
                "Starting BillableEventBusinessRuleDispatcher..."
        )

        if (inMapParam == null) {

            LOGGER.warn(
                    "BillableEventBusinessRuleDispatcher skipped: " +
                    "input parameter map is null."
            )

            return
        }

        String triggerType =
                (String) inMapParam.get(
                        "inTriggerType"
                )

        EEntityView entityView =
                (EEntityView) inMapParam.get(
                        "inEntity"
                )

        EFieldChangesView fieldChanges =
                (EFieldChangesView) inMapParam.get(
                        "inOriginalFieldChanges"
                )

        LOGGER.info(
                "Trigger: ${triggerType}"
        )

        if (entityView == null) {

            LOGGER.warn(
                    "BillableEventBusinessRuleDispatcher skipped: " +
                    "Entity View is null."
            )

            return
        }

        String entityName =
                entityView.getEntityName()

        LOGGER.info(
                "Entity: ${entityName}"
        )

        if (!TRIGGER_UPDATE.equals(triggerType)) {

            LOGGER.info(
                    "Skipping trigger '${triggerType}'. " +
                    "Business rule requires '${TRIGGER_UPDATE}'."
            )

            return
        }

        if (fieldChanges == null) {

            LOGGER.info(
                    "No original field changes found for entity " +
                    "${entityName}."
            )

            return
        }

        Entity entity =
                entityView._entity

        if (entity == null) {

            LOGGER.warn(
                    "Underlying Entity is null."
            )

            return
        }

        LOGGER.info(
                "Underlying Entity: " +
                "${entity.getClass().getName()}"
        )

        /*
         * ----------------------------------------------------------
         * UNIT FACILITY VISIT
         * ----------------------------------------------------------
         */
        if (entity instanceof UnitFacilityVisit) {

            processUnitFacilityVisitUpdate(
                    (UnitFacilityVisit) entity,
                    fieldChanges
            )

            LOGGER.info(
                    "BillableEventBusinessRuleDispatcher completed."
            )

            return
        }

        /*
         * ----------------------------------------------------------
         * UNIT
         * ----------------------------------------------------------
         */
        if (entity instanceof Unit) {

            processUnitUpdate(
                    (Unit) entity,
                    fieldChanges
            )

            LOGGER.info(
                    "BillableEventBusinessRuleDispatcher completed."
            )

            return
        }

        LOGGER.info(
                "No business rule configured for entity: " +
                "${entityName}"
        )

        LOGGER.info(
                "BillableEventBusinessRuleDispatcher completed."
        )
    }

    /**
     * Processes UnitFacilityVisit UPDATE events.
     *
     * Export rollover is identified when both intended and
     * actual outbound carrier vessels are changed.
     */
    private void processUnitFacilityVisitUpdate(
            UnitFacilityVisit inUfv,
            EFieldChangesView inFieldChanges) {

        LOGGER.info(
                "Processing UnitFacilityVisit UPDATE..."
        )

        if (inUfv == null) {

            LOGGER.warn(
                    "UnitFacilityVisit is null."
            )

            return
        }

        Unit unit =
                inUfv.getUfvUnit()

        if (unit == null) {

            LOGGER.warn(
                    "UnitFacilityVisit has no associated Unit."
            )

            return
        }

        LOGGER.info(
                "Unit: ${unit.getUnitId()}"
        )

        String unitCategory =
                unit.getUnitCategory()?.getKey()

        LOGGER.info(
                "Unit Category: ${unitCategory}"
        )

        if (!EXPORT_UNIT_CATEGORY.equals(unitCategory)) {

            LOGGER.info(
                    "Unit ${unit.getUnitId()} is not an Export Unit."
            )

            return
        }

        boolean intendedObCvChanged =
                false

        boolean actualObCvChanged =
                false

        int fieldChangeCount =
                inFieldChanges.getFieldChangeCount()

        LOGGER.info(
                "Field Change Count: ${fieldChangeCount}"
        )

        for (int i = 0;
             i < fieldChangeCount;
             i++) {

            EFieldChange fieldChange =
                    inFieldChanges.getFieldChange(i)

            if (fieldChange == null) {

                LOGGER.warn(
                        "Field Change [${i}] is null."
                )

                continue
            }

            String fieldName =
                    fieldChange.getFieldId().toString()

            LOGGER.info(
                    "Field: ${fieldName}"
            )

            LOGGER.info(
                    "Field Change: ${fieldChange}"
            )

            if (FIELD_INTENDED_OB_CV.equals(fieldName)) {

                intendedObCvChanged =
                        true

                LOGGER.info(
                        "Detected field change: " +
                        "${FIELD_INTENDED_OB_CV}"
                )
            }

            if (FIELD_ACTUAL_OB_CV.equals(fieldName)) {

                actualObCvChanged =
                        true

                LOGGER.info(
                        "Detected field change: " +
                        "${FIELD_ACTUAL_OB_CV}"
                )
            }
        }

        LOGGER.info(
                "Intended OB CV Changed: " +
                "${intendedObCvChanged}"
        )

        LOGGER.info(
                "Actual OB CV Changed: " +
                "${actualObCvChanged}"
        )

        if (intendedObCvChanged &&
                actualObCvChanged) {

            LOGGER.info(
                    "Export Vessel RollOver detected from " +
                    "UnitFacilityVisit for Unit " +
                    "${unit.getUnitId()}."
            )

            /*
             * UNIT_EXPORT_VESSEL_REROUTE_YARD is a NON-BILLABLE
             * event.
             *
             * Therefore, no duplicate-event check is performed.
             */
            recordNonBillableEvent(
                    unit,
                    EXPORT_ROLLOVER_EVENT,
                    EXPORT_ROLLOVER_NOTE
            )

        } else {

            LOGGER.info(
                    "Export Vessel RollOver condition not satisfied " +
                    "for UnitFacilityVisit."
            )
        }

        LOGGER.info(
                "UnitFacilityVisit UPDATE processing completed."
        )
    }

    /**
     * Processes Unit UPDATE events.
     *
     * At Unit level, the routing itself changes when the unit is
     * moved from one export vessel routing to another.
     *
     * Example:
     *
     * unitRouting =
     * A298 -> A291
     *
     * This represents the same export vessel rollover detected
     * at UnitFacilityVisit level.
     */
    private void processUnitUpdate(
            Unit inUnit,
            EFieldChangesView inFieldChanges) {

        LOGGER.info(
                "Processing Unit UPDATE..."
        )

        if (inUnit == null) {

            LOGGER.warn(
                    "Unit is null."
            )

            return
        }

        LOGGER.info(
                "Unit: ${inUnit.getUnitId()}"
        )

        String unitCategory =
                inUnit.getUnitCategory()?.getKey()

        LOGGER.info(
                "Unit Category: ${unitCategory}"
        )

        if (!EXPORT_UNIT_CATEGORY.equals(unitCategory)) {

            LOGGER.info(
                    "Unit ${inUnit.getUnitId()} is not an Export Unit."
            )

            return
        }

        boolean routingChanged =
                false

        int fieldChangeCount =
                inFieldChanges.getFieldChangeCount()

        LOGGER.info(
                "Field Change Count: ${fieldChangeCount}"
        )

        for (int i = 0;
             i < fieldChangeCount;
             i++) {

            EFieldChange fieldChange =
                    inFieldChanges.getFieldChange(i)

            if (fieldChange == null) {

                LOGGER.warn(
                        "Field Change [${i}] is null."
                )

                continue
            }

            String fieldName =
                    fieldChange.getFieldId().toString()

            LOGGER.info(
                    "Field: ${fieldName}"
            )

            LOGGER.info(
                    "Field Change: ${fieldChange}"
            )

            if (FIELD_UNIT_ROUTING.equals(fieldName)) {

                routingChanged =
                        true

                LOGGER.info(
                        "Detected Unit routing change."
                )
            }
        }

        LOGGER.info(
                "Unit Routing Changed: ${routingChanged}"
        )

        if (!routingChanged) {

            LOGGER.info(
                    "No Unit routing change detected."
            )

            return
        }

        /*
         * The Unit entity has changed its routing.
         *
         * For an Export unit this represents the Unit-level
         * representation of the vessel rollover.
         *
         * UNIT_EXPORT_VESSEL_REROUTE_YARD is NON-BILLABLE,
         * therefore duplicate checking is intentionally not
         * performed here.
         */
        LOGGER.info(
                "Export Vessel RollOver detected from " +
                "Unit routing change for Unit " +
                "${inUnit.getUnitId()}."
        )

        recordNonBillableEvent(
                inUnit,
                EXPORT_ROLLOVER_EVENT,
                EXPORT_ROLLOVER_NOTE
        )

        LOGGER.info(
                "Unit UPDATE processing completed."
        )
    }

    /**
     * Records a NON-BILLABLE event.
     *
     * IMPORTANT:
     * No duplicate-event check is performed here.
     *
     * This method is intentionally separate from
     * recordBillableEvent() so that billable-event duplicate
     * handling cannot accidentally affect non-billable events.
     */
    private void recordNonBillableEvent(
            Unit inUnit,
            String inEventId,
            String inNote) {

        if (inUnit == null) {

            LOGGER.warn(
                    "Non-billable event recording skipped: " +
                    "Unit is null."
            )

            return
        }

        try {

            ServicesManager servicesManager =
                    (ServicesManager) Roastery.getBean(
                            ServicesManager.BEAN_ID
                    )

            if (servicesManager == null) {

                LOGGER.warn(
                        "ServicesManager is not available."
                )

                return
            }

            EventType eventType =
                    EventType.findEventType(
                            inEventId
                    )

            if (eventType == null) {

                LOGGER.warn(
                        "EventType '${inEventId}' was not found."
                )

                return
            }

            LOGGER.info(
                    "Recording NON-BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}."
            )

            servicesManager.recordEvent(
                    eventType,
                    inNote,
                    null,
                    null,
                    inUnit,
                    null
            )

            LOGGER.info(
                    "Successfully recorded NON-BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}."
            )

        } catch (Exception e) {

            LOGGER.warn(
                    "Error while recording NON-BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}: ",
                    e
            )
        }
    }

    /**
     * Records a BILLABLE event.
     *
     * Duplicate-event checking is intentionally implemented
     * ONLY in this method.
     *
     * This method is reserved for future billable-event rules.
     */
    private void recordBillableEvent(
            Unit inUnit,
            String inEventId,
            String inNote) {

        if (inUnit == null) {

            LOGGER.warn(
                    "Billable event recording skipped: " +
                    "Unit is null."
            )

            return
        }

        try {

            ServicesManager servicesManager =
                    (ServicesManager) Roastery.getBean(
                            ServicesManager.BEAN_ID
                    )

            if (servicesManager == null) {

                LOGGER.warn(
                        "ServicesManager is not available."
                )

                return
            }

            EventType eventType =
                    EventType.findEventType(
                            inEventId
                    )

            if (eventType == null) {

                LOGGER.warn(
                        "Billable EventType '${inEventId}' " +
                        "was not found."
                )

                return
            }

            /*
             * Duplicate-event validation is ONLY performed for
             * BILLABLE events.
             */
            boolean alreadyRecorded =
                    servicesManager.hasEventTypeBeenRecorded(
                            eventType,
                            inUnit
                    )

            if (alreadyRecorded) {

                LOGGER.info(
                        "BILLABLE EventType '${inEventId}' has " +
                        "already been recorded for Unit " +
                        "${inUnit.getUnitId()}."
                )

                LOGGER.info(
                        "Duplicate BILLABLE event will not be recorded."
                )

                return
            }

            LOGGER.info(
                    "Recording BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}."
            )

            servicesManager.recordEvent(
                    eventType,
                    inNote,
                    null,
                    null,
                    inUnit,
                    null
            )

            LOGGER.info(
                    "Successfully recorded BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}."
            )

        } catch (Exception e) {

            LOGGER.warn(
                    "Error while recording BILLABLE EventType " +
                    "'${inEventId}' for Unit " +
                    "${inUnit.getUnitId()}: ",
                    e
            )
        }
    }
}