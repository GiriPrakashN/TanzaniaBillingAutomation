/*
 * Copyright (c) 2025 TEAGTL. All Rights Reserved.
 */

package com.navis.tpa2.custom

import com.navis.external.services.AbstractGeneralNoticeCodeExtension
import com.navis.framework.business.Roastery
import com.navis.framework.util.LogUtils
import com.navis.inventory.business.units.EqBaseOrderItem
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.orders.business.eqorders.Booking
import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.extract.ChargeableUnitEvent
import com.navis.services.business.event.Event
import com.navis.services.business.event.GroovyEvent
import com.navis.services.business.rules.EventType
import org.apache.log4j.Level


class ProcessExportRolloverBillableEvents
        extends AbstractGeneralNoticeCodeExtension {


    private static final String TRIGGER_EVENT =
            "UNIT_EXPORT_VESSEL_REROUTE_YARD"

    private static final String ROLL_OVER_EVENT =
            "UV_ROLL_OVER"

    private static final String DOCUMENTATION_EVENT =
            "UV_DOCUMENTATION"

    private static final String EXPORT_CATEGORY =
            "EXPRT"

    private static final String ROLL_OVER_NOTE =
            "Export Vessel RollOver billable event."

    private static final String DOCUMENTATION_NOTE =
            "Export Vessel RollOver documentation billable event."


    /*
     * Prevent simultaneous processing in the same JVM.
     */
    private static final Object BILLABLE_EVENT_LOCK =
            new Object()


    @Override
    void execute(GroovyEvent inGroovyEvent) {

        LogUtils.setLogLevel(
                this.class,
                Level.INFO
        )

        log("")
        log("==============================================================")
        log("PROCESS EXPORT ROLLOVER BILLABLE EVENTS - START")
        log("==============================================================")


        try {

            /*
             * ======================================================
             * EVENT
             * ======================================================
             */

            if (inGroovyEvent == null) {

                log("GroovyEvent: NULL")
                return
            }

            Event event =
                    inGroovyEvent.getEvent()

            if (event == null) {

                log("Event: NULL")
                return
            }

            String eventTypeId =
                    event.getEventTypeId()

            log(
                    "Triggered Event Type: " +
                    "${eventTypeId}"
            )

            if (!TRIGGER_EVENT.equals(eventTypeId)) {

                log(
                        "Triggered event is not " +
                        "${TRIGGER_EVENT}"
                )

                return
            }


            /*
             * ======================================================
             * UNIT
             * ======================================================
             */

            Unit unit =
                    (Unit) inGroovyEvent.getEntity()

            if (unit == null) {

                log("Unit: NULL")
                return
            }

            String equipmentNumber =
                    unit.getUnitId()

            Long unitGkey =
                    unit.getUnitGkey() as Long

            log(
                    "Equipment Number: " +
                    "${equipmentNumber}"
            )

            log(
                    "Unit GKEY: " +
                    "${unitGkey}"
            )


            /*
             * ======================================================
             * EXPORT VALIDATION
             * ======================================================
             */

            String unitCategory = null

            try {

                unitCategory =
                        unit.getUnitCategory()?.getKey()

            } catch (Exception ignored) {
            }

            log(
                    "Unit Category: " +
                    "${unitCategory}"
            )

            if (!EXPORT_CATEGORY.equals(unitCategory)) {

                log(
                        "Unit ${equipmentNumber} is not EXPRT."
                )

                return
            }


            /*
             * ======================================================
             * UFV
             * ======================================================
             */

            UnitFacilityVisit ufv =
                    unit.getUnitActiveUfvNowActive()

            if (ufv == null) {

                ufv =
                        unit.findProbableInitialUfv()
            }

            if (ufv == null) {

                log("UFV: NULL")
                return
            }

            Long ufvGkey =
                    ufv.getUfvGkey() as Long

            log(
                    "UFV GKEY: " +
                    "${ufvGkey}"
            )

            log(
                    "UFV: " +
                    "${ufv}"
            )


            /*
             * ======================================================
             * BOOKING
             * ======================================================
             */

            Booking booking =
                    resolveBooking(unit)

            String bookingNumber = null

            if (booking != null) {

                try {

                    bookingNumber =
                            booking.getEqboNbr()

                } catch (Exception ignored) {
                }

                if (bookingNumber == null ||
                        bookingNumber.trim().isEmpty()) {

                    try {

                        bookingNumber =
                                booking.getDocumentNbr()

                    } catch (Exception ignored) {
                    }
                }
            }

            log(
                    "Booking Number: " +
                    "${bookingNumber}"
            )


            /*
             * ======================================================
             * UV_ROLL_OVER
             * ======================================================
             */

            log(
                    "--------------------------------------------------------------"
            )

            log(
                    "Event: " +
                    "${ROLL_OVER_EVENT}"
            )

            log(
                    "Equipment: " +
                    "${equipmentNumber}"
            )

            log(
                    "Unit GKEY: " +
                    "${unitGkey}"
            )

            log(
                    "UFV GKEY: " +
                    "${ufvGkey}"
            )


            boolean rolloverRecorded =
                    processBillableEvent(
                            unit,
                            ufv,
                            ROLL_OVER_EVENT,
                            ROLL_OVER_NOTE
                    )

            log(
                    "${ROLL_OVER_EVENT} recorded: " +
                    "${rolloverRecorded}"
            )


            /*
             * ======================================================
             * UV_DOCUMENTATION
             * ======================================================
             */

            log(
                    "--------------------------------------------------------------"
            )

            log(
                    "Event: " +
                    "${DOCUMENTATION_EVENT}"
            )

            log(
                    "Booking Number: " +
                    "${bookingNumber}"
            )


            if (booking == null) {

                log(
                        "${DOCUMENTATION_EVENT}: " +
                        "Booking could not be resolved."
                )

            } else {

                boolean documentationRecorded =
                        processBookingDocumentation(
                                booking,
                                unit
                        )

                log(
                        "${DOCUMENTATION_EVENT} recorded: " +
                        "${documentationRecorded}"
                )
            }


        } catch (Exception e) {

            log(
                    "Exception Type: " +
                    "${e.getClass().getName()}"
            )

            log(
                    "Exception Message: " +
                    "${e.getMessage()}"
            )

        } finally {

            log(
                    "=============================================================="
            )

            log(
                    "PROCESS EXPORT ROLLOVER BILLABLE EVENTS - COMPLETED"
            )

            log(
                    "=============================================================="
            )
        }
    }


    /*
     * ==============================================================
     * BOOKING RESOLUTION
     * ==============================================================
     *
     * Unit
     *   -> Departure Order Item
     *      -> Departure Order
     *         -> Booking Number
     *            -> Booking.findBookingsByNbr()
     *
     * This is the booking resolution that was already working.
     */
    private Booking resolveBooking(
            Unit unit) {

        if (unit == null) {
            return null
        }

        try {

            EqBaseOrderItem departureOrderItem =
                    unit.getUnitDepartureOrderItem()

            if (departureOrderItem == null) {

                log(
                        "Departure Order Item: NULL"
                )

                return null
            }

            log(
                    "Departure Order Item: " +
                    "${departureOrderItem}"
            )


            def departureOrder =
                    departureOrderItem.getEqboiOrder()

            if (departureOrder == null) {

                log(
                        "Departure Order: NULL"
                )

                return null
            }

            log(
                    "Departure Order: " +
                    "${departureOrder}"
            )


            String bookingNumber = null

            try {

                bookingNumber =
                        departureOrder.getEqboNbr()

            } catch (Exception ignored) {
            }


            /*
             * Proxy-safe fallback.
             */
            if (bookingNumber == null ||
                    bookingNumber.trim().isEmpty()) {

                try {

                    def method =
                            departureOrder
                                    .getClass()
                                    .getMethod(
                                            "getEqboNbr"
                                    )

                    bookingNumber =
                            method.invoke(
                                    departureOrder
                            ) as String

                } catch (Exception ignored) {
                }
            }


            log(
                    "Booking Number: " +
                    "${bookingNumber}"
            )


            if (bookingNumber == null ||
                    bookingNumber.trim().isEmpty()) {

                return null
            }


            def bookings =
                    Booking.findBookingsByNbr(
                            bookingNumber
                    )


            if (bookings == null ||
                    bookings.isEmpty()) {

                log(
                        "Booking Count: 0"
                )

                return null
            }


            log(
                    "Booking Count: " +
                    "${bookings.size()}"
            )


            Booking booking =
                    bookings[0] as Booking

            log(
                    "Booking: " +
                    "${booking}"
            )

            log(
                    "Booking Number: " +
                    "${booking.getEqboNbr()}"
            )

            return booking


        } catch (Exception e) {

            log(
                    "Booking Resolution Exception: " +
                    "${e.getClass().getName()}"
            )

            log(
                    "Booking Resolution Message: " +
                    "${e.getMessage()}"
            )

            return null
        }
    }


    /*
     * ==============================================================
     * BILLABLE EVENT PROCESSING
     * ==============================================================
     */
    private boolean processBillableEvent(
            Unit unit,
            UnitFacilityVisit ufv,
            String eventId,
            String note) {

        synchronized (BILLABLE_EVENT_LOCK) {

            return processBillableEventInternal(
                    unit,
                    ufv,
                    eventId,
                    note
            )
        }
    }


    /*
     * ==============================================================
     * BILLABLE EVENT DECISION
     * ==============================================================
     *
     * 1. Find actual CUE records for the Unit + Event Type.
     *
     * 2. If no CUE exists:
     *       RECORD
     *
     * 3. If CUE exists:
     *       inspect every matching CUE.
     *
     * 4. If ANY matching CUE is QUEUED:
     *       DO NOT RECORD
     *
     * 5. If matching CUEs exist but none is QUEUED:
     *       RECORD
     */
    private boolean processBillableEventInternal(
            Unit unit,
            UnitFacilityVisit ufv,
            String eventId,
            String note) {

        if (unit == null ||
                ufv == null) {

            log(
                    "${eventId}: Unit/UFV is NULL."
            )

            return false
        }


        String equipmentNumber =
                unit.getUnitId()

        Long unitGkey =
                unit.getUnitGkey() as Long

        Long ufvGkey =
                ufv.getUfvGkey() as Long


        try {

            EventType eventType =
                    EventType.findEventType(
                            eventId
                    )

            if (eventType == null) {

                log(
                        "EventType not found: " +
                        "${eventId}"
                )

                return false
            }


            /*
             * ======================================================
             * DIRECT CUE QUERY
             * ======================================================
             *
             * Do NOT use:
             *
             * ChargeableUnitEvent.isCUEExistsByStatus()
             *
             * here.
             *
             * We query the actual CUE rows directly.
             */
            List<ChargeableUnitEvent> cues =
                    findUnitBillableEvents(
                            unitGkey,
                            eventId
                    )


            int cueCount =
                    cues == null
                            ? 0
                            : cues.size()


            log(
                    "${eventId} CUE Count: " +
                    "${cueCount}"
            )


            /*
             * ======================================================
             * NO EXISTING CUE
             * ======================================================
             */

            if (cueCount == 0) {

                log(
                        "${eventId}: no existing CUE."
                )

                return recordBillableEvent(
                        unit,
                        eventType,
                        eventId,
                        note
                )
            }


            /*
             * ======================================================
             * EXISTING CUES
             * ======================================================
             */

            boolean queuedFound =
                    false


            for (ChargeableUnitEvent cue :
                    cues) {

                if (cue == null) {
                    continue
                }


                Long cueGkey = null
                String cueEventType = null
                String cueStatus = null
                Long cueUnitGkey = null
                Long cueUfvGkey = null
                String cueEquipment = null


                try {

                    cueGkey =
                            cue.getEventGkey()

                } catch (Exception ignored) {
                }


                try {

                    cueEventType =
                            cue.getEventType()

                } catch (Exception ignored) {
                }


                try {

                    cueStatus =
                            cue.getStatus()

                } catch (Exception ignored) {
                }


                try {

                    cueUnitGkey =
                            cue.getBexuUnitGkey()

                } catch (Exception ignored) {
                }


                try {

                    cueUfvGkey =
                            cue.getBexuUfvGkey()

                } catch (Exception ignored) {
                }


                try {

                    cueEquipment =
                            cue.getBexuEqId()

                } catch (Exception ignored) {
                }


                log(
                        "CUE GKEY: " +
                        "${cueGkey}"
                )

                log(
                        "CUE Event Type: " +
                        "${cueEventType}"
                )

                log(
                        "CUE Status: " +
                        "${cueStatus}"
                )

                log(
                        "CUE Unit GKEY: " +
                        "${cueUnitGkey}"
                )

                log(
                        "CUE UFV GKEY: " +
                        "${cueUfvGkey}"
                )

                log(
                        "CUE Equipment: " +
                        "${cueEquipment}"
                )


                /*
                 * Confirm exact event.
                 */
                if (!eventId.equals(
                        cueEventType)) {

                    continue
                }


                /*
                 * Confirm exact Unit.
                 */
                if (unitGkey != null &&
                        cueUnitGkey != null &&
                        !unitGkey.equals(
                                cueUnitGkey)) {

                    continue
                }


                /*
                 * Confirm exact UFV where available.
                 */
                if (ufvGkey != null &&
                        cueUfvGkey != null &&
                        !ufvGkey.equals(
                                cueUfvGkey)) {

                    continue
                }


                /*
                 * ==================================================
                 * THE IMPORTANT CHECK
                 * ==================================================
                 */
                if (ChargeableUnitEvent.QUEUED.equals(
                        cueStatus)) {

                    queuedFound =
                            true

                    log(
                            "${eventId}: QUEUED CUE FOUND."
                    )

                    break
                }
            }


            /*
             * ======================================================
             * EXISTING + QUEUED
             * ======================================================
             */

            if (queuedFound) {

                log(
                        "${eventId}: existing QUEUED event found."
                )

                log(
                        "${eventId}: NOT RECORDED."
                )

                return false
            }


            /*
             * ======================================================
             * EXISTING + NOT QUEUED
             * ======================================================
             *
             * This follows your requirement:
             *
             * existing event + status != QUEUED
             *     -> record again
             */
            log(
                    "${eventId}: existing event(s) found, " +
                    "but none are QUEUED."
            )

            return recordBillableEvent(
                    unit,
                    eventType,
                    eventId,
                    note
            )


        } catch (Exception e) {

            log(
                    "${eventId} Exception: " +
                    "${e.getClass().getName()}"
            )

            log(
                    "${eventId} Message: " +
                    "${e.getMessage()}"
            )

            return false
        }
    }


    /*
     * ==============================================================
     * DIRECT CUE QUERY
     * ==============================================================
     *
     * ChargeableUnitEvent properties used:
     *
     *     bexuUnitGkey
     *     bexuEventType
     *     bexuStatus
     *     bexuUfvGkey
     *
     * These correspond to the documented CUE getters:
     *
     *     getBexuUnitGkey()
     *     getEventType()
     *     getStatus()
     *     getBexuUfvGkey()
     */
    private List<ChargeableUnitEvent> findUnitBillableEvents(
            Long unitGkey,
            String eventId) {

        if (unitGkey == null ||
                eventId == null ||
                eventId.trim().isEmpty()) {

            return []
        }


        try {

            def hibernate =
                    Roastery.getHibernateApi()

            if (hibernate == null) {

                log(
                        "Hibernate API: NULL"
                )

                return []
            }


            /*
             * Direct HQL query.
             *
             * Do not filter status here.
             *
             * We need ALL matching CUEs so that we can determine
             * whether ANY of them is QUEUED.
             */
            String hql = """
                FROM ChargeableUnitEvent cue
                WHERE cue.bexuUnitGkey = ${unitGkey}
                AND cue.bexuEventType = '${eventId}'
            """


            log(
                    "CUE Query Unit GKEY: " +
                    "${unitGkey}"
            )

            log(
                    "CUE Query Event Type: " +
                    "${eventId}"
            )


            def result =
                    hibernate.find(
                            hql
                    )


            if (result == null) {

                return []
            }


            return result as List<ChargeableUnitEvent>


        } catch (Exception e) {

            log(
                    "CUE Query Exception: " +
                    "${e.getClass().getName()}"
            )

            log(
                    "CUE Query Message: " +
                    "${e.getMessage()}"
            )

            return []
        }
    }


    /*
     * ==============================================================
     * RECORD BILLABLE EVENT
     * ==============================================================
     */
    private boolean recordBillableEvent(
            Unit unit,
            EventType eventType,
            String eventId,
            String note) {

        try {

            ServicesManager servicesManager =
                    (ServicesManager) Roastery.getBean(
                            ServicesManager.BEAN_ID
                    )

            if (servicesManager == null) {

                log(
                        "ServicesManager: NULL"
                )

                return false
            }


            log(
                    "Recording ${eventId} for " +
                    "${unit.getUnitId()}"
            )


            servicesManager.recordEvent(
                    eventType,
                    note,
                    null,
                    null,
                    unit,
                    null
            )


            log(
                    "${eventId}: RECORDED"
            )

            return true


        } catch (Exception e) {

            log(
                    "${eventId} Recording Exception: " +
                    "${e.getClass().getName()}"
            )

            log(
                    "${eventId} Recording Message: " +
                    "${e.getMessage()}"
            )

            return false
        }
    }


    /*
     * ==============================================================
     * BOOKING DOCUMENTATION
     * ==============================================================
     *
     * Booking
     *    |
     *    +-- Order Item 1
     *    |      +-- Container A
     *    |      +-- Container B
     *    |
     *    +-- Order Item 2
     *           +-- Container C
     *
     * Check UV_DOCUMENTATION against ALL containers.
     *
     * If ANY associated container has a QUEUED
     * UV_DOCUMENTATION event:
     *
     *     DO NOT RECORD
     *
     * Otherwise:
     *
     *     record exactly ONE UV_DOCUMENTATION
     *     against the rollover container.
     */
    private boolean processBookingDocumentation(
            Booking booking,
            Unit rolloverUnit) {

        if (booking == null ||
                rolloverUnit == null) {

            return false
        }


        synchronized (BILLABLE_EVENT_LOCK) {

            String bookingNumber = null

            try {

                bookingNumber =
                        booking.getEqboNbr()

            } catch (Exception ignored) {
            }


            log(
                    "Booking Number: " +
                    "${bookingNumber}"
            )


            /*
             * ======================================================
             * BOOKING ORDER ITEMS
             * ======================================================
             */

            def bookingOrderItems =
                    booking.getEqboOrderItems()


            if (bookingOrderItems == null ||
                    bookingOrderItems.isEmpty()) {

                log(
                        "Booking Order Item Count: 0"
                )

                return false
            }


            log(
                    "Booking Order Item Count: " +
                    "${bookingOrderItems.size()}"
            )


            /*
             * ======================================================
             * ASSOCIATED UNITS
             * ======================================================
             */

            List<Unit> associatedUnits =
                    new ArrayList<Unit>()


            for (Object orderItemObject :
                    bookingOrderItems) {

                if (orderItemObject == null) {
                    continue
                }


                log(
                        "Booking Order Item: " +
                        "${orderItemObject}"
                )


                try {

                    def unitsDQ =
                            orderItemObject
                                    .getUnitsReceivedForOrderItemDQ()


                    if (unitsDQ == null) {
                        continue
                    }


                    def units =
                            Roastery
                                    .getHibernateApi()
                                    .findEntitiesByDomainQuery(
                                            unitsDQ
                                    )


                    if (units == null) {
                        continue
                    }


                    for (Object unitObject :
                            units) {

                        if (!(unitObject instanceof Unit)) {
                            continue
                        }


                        Unit associatedUnit =
                                (Unit) unitObject


                        if (!associatedUnits.contains(
                                associatedUnit)) {

                            associatedUnits.add(
                                    associatedUnit
                            )
                        }
                    }


                } catch (Exception e) {

                    log(
                            "Associated Unit Resolution Exception: " +
                            "${e.getMessage()}"
                    )
                }
            }


            /*
             * Always make sure the rollover container
             * is part of the list.
             */
            if (!associatedUnits.contains(
                    rolloverUnit)) {

                associatedUnits.add(
                        rolloverUnit
                )
            }


            log(
                    "Associated Unit Count: " +
                    "${associatedUnits.size()}"
            )


            /*
             * ======================================================
             * CHECK UV_DOCUMENTATION
             * ======================================================
             */

            for (Unit associatedUnit :
                    associatedUnits) {

                if (associatedUnit == null) {
                    continue
                }


                String equipmentNumber =
                        associatedUnit.getUnitId()


                Long associatedUnitGkey =
                        associatedUnit.getUnitGkey() as Long


                UnitFacilityVisit associatedUfv =
                        associatedUnit
                                .getUnitActiveUfvNowActive()


                if (associatedUfv == null) {

                    associatedUfv =
                            associatedUnit
                                    .findProbableInitialUfv()
                }


                Long associatedUfvGkey = null

                if (associatedUfv != null) {

                    associatedUfvGkey =
                            associatedUfv
                                    .getUfvGkey() as Long
                }


                log(
                        "Booking Unit: " +
                        "${equipmentNumber}"
                )

                log(
                        "Booking Unit GKEY: " +
                        "${associatedUnitGkey}"
                )

                log(
                        "Booking Unit UFV GKEY: " +
                        "${associatedUfvGkey}"
                )


                /*
                 * Get ALL documentation CUEs for this unit.
                 */
                List<ChargeableUnitEvent> documentationCues =
                        findUnitBillableEvents(
                                associatedUnitGkey,
                                DOCUMENTATION_EVENT
                        )


                int documentationCueCount =
                        documentationCues == null
                                ? 0
                                : documentationCues.size()


                log(
                        "${equipmentNumber} " +
                        "${DOCUMENTATION_EVENT} CUE Count: " +
                        "${documentationCueCount}"
                )


                /*
                 * Inspect every documentation CUE.
                 */
                if (documentationCues != null) {

                    for (ChargeableUnitEvent cue :
                            documentationCues) {

                        if (cue == null) {
                            continue
                        }


                        String cueStatus = null
                        String cueEventType = null
                        Long cueGkey = null


                        try {

                            cueGkey =
                                    cue.getEventGkey()

                        } catch (Exception ignored) {
                        }


                        try {

                            cueEventType =
                                    cue.getEventType()

                        } catch (Exception ignored) {
                        }


                        try {

                            cueStatus =
                                    cue.getStatus()

                        } catch (Exception ignored) {
                        }


                        log(
                                "Documentation CUE GKEY: " +
                                "${cueGkey}"
                        )

                        log(
                                "Documentation CUE Event Type: " +
                                "${cueEventType}"
                        )

                        log(
                                "Documentation CUE Status: " +
                                "${cueStatus}"
                        )


                        if (DOCUMENTATION_EVENT.equals(
                                cueEventType) &&
                                ChargeableUnitEvent.QUEUED.equals(
                                        cueStatus)) {

                            log(
                                    "${DOCUMENTATION_EVENT} " +
                                    "QUEUED event already exists " +
                                    "for ${equipmentNumber}."
                            )

                            log(
                                    "${DOCUMENTATION_EVENT}: " +
                                    "NOT RECORDED."
                            )

                            return false
                        }
                    }
                }
            }


            /*
             * ======================================================
             * NO QUEUED DOCUMENTATION FOUND
             * ======================================================
             *
             * Record exactly one documentation event.
             *
             * Target = rollover container.
             */
            UnitFacilityVisit rolloverUfv =
                    rolloverUnit
                            .getUnitActiveUfvNowActive()


            if (rolloverUfv == null) {

                rolloverUfv =
                        rolloverUnit
                                .findProbableInitialUfv()
            }


            if (rolloverUfv == null) {

                log(
                        "Rollover UFV: NULL"
                )

                return false
            }


            log(
                    "Documentation Target: " +
                    "${rolloverUnit.getUnitId()}"
            )


            return processBillableEvent(
                    rolloverUnit,
                    rolloverUfv,
                    DOCUMENTATION_EVENT,
                    DOCUMENTATION_NOTE
            )
        }
    }
}