/*
 * Copyright (c) 2025 TEAGTL. All Rights Reserved.
 */

package com.navis.external.framework.entity.AbstractEntityLifecycleInterceptor

import com.navis.external.framework.entity.AbstractEntityLifecycleInterceptor
import com.navis.external.framework.entity.EEntityView
import com.navis.external.framework.util.EFieldChanges
import com.navis.external.framework.util.EFieldChangesView
import com.navis.framework.persistence.Entity
import com.navis.extension.handler.business.entityinterception.DefaultEntityView
import com.navis.framework.util.LogUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger

/**
 * Global lifecycle interceptor for billable-event processing.
 *
 * Captures CREATE, UPDATE and DELETE lifecycle events and delegates
 * business-change processing to BusinessChangeDispatcher.
 */
class BillableEventLifecycleInterceptor
        extends AbstractEntityLifecycleInterceptor {

    private static final Logger LOGGER =
            Logger.getLogger(
                    BillableEventLifecycleInterceptor.class
            )

    private static final String TRIGGER_CREATE =
            "CREATE"

    private static final String TRIGGER_UPDATE =
            "UPDATE"

    private static final String TRIGGER_DELETE =
            "DELETE"

    private static final String BUSINESS_CHANGE_DISPATCHER =
            "BusinessChangeDispatcher"

    private static final String PARAM_TRIGGER_TYPE =
            "inTriggerType"

    private static final String PARAM_ENTITY =
            "inEntity"

    private static final String PARAM_ORIGINAL_FIELD_CHANGES =
            "inOriginalFieldChanges"

    private static final String PARAM_MORE_FIELD_CHANGES =
            "inMoreFieldChanges"

    @Override
    void onCreate(
            EEntityView inEntity,
            EFieldChangesView inOriginalFieldChanges,
            EFieldChanges inMoreFieldChanges) {

        LogUtils.setLogLevel(
                this.class,
                Level.INFO
        )

        LOGGER.info(
                "=============================================================="
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - CREATE"
        )

        LOGGER.info(
                "=============================================================="
        )

        logLifecycleDetails(
                inEntity,
                inOriginalFieldChanges,
                inMoreFieldChanges,
                TRIGGER_CREATE
        )

        invokeBusinessChangeDispatcher(
                inEntity,
                inOriginalFieldChanges,
                inMoreFieldChanges,
                TRIGGER_CREATE
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - CREATE COMPLETED"
        )

        LOGGER.info(
                "=============================================================="
        )
    }

    @Override
    void onUpdate(
            EEntityView inEntity,
            EFieldChangesView inOriginalFieldChanges,
            EFieldChanges inMoreFieldChanges) {

        LogUtils.setLogLevel(
                this.class,
                Level.INFO
        )

        LOGGER.info(
                "=============================================================="
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - UPDATE"
        )

        LOGGER.info(
                "=============================================================="
        )

        logLifecycleDetails(
                inEntity,
                inOriginalFieldChanges,
                inMoreFieldChanges,
                TRIGGER_UPDATE
        )

        invokeBusinessChangeDispatcher(
                inEntity,
                inOriginalFieldChanges,
                inMoreFieldChanges,
                TRIGGER_UPDATE
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - UPDATE COMPLETED"
        )

        LOGGER.info(
                "=============================================================="
        )
    }

    @Override
    void preDelete(Entity inEntity) {

        LogUtils.setLogLevel(
                this.class,
                Level.INFO
        )

        LOGGER.info(
                "=============================================================="
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - DELETE"
        )

        LOGGER.info(
                "=============================================================="
        )

        if (inEntity == null) {

            LOGGER.warn(
                    "DELETE entity is NULL."
            )

            return
        }

        LOGGER.info(
                "Entity Class: " +
                "${inEntity.getClass().getName()}"
        )

        LOGGER.info(
                "Entity: ${inEntity}"
        )

        LOGGER.info(
                "Entity lifecycle DELETE callback received."
        )

        invokeBusinessChangeDispatcher(
                new DefaultEntityView(inEntity),
                null,
                null,
                TRIGGER_DELETE
        )

        LOGGER.info(
                "BILLABLE EVENT LIFECYCLE INTERCEPTOR - DELETE COMPLETED"
        )

        LOGGER.info(
                "=============================================================="
        )
    }

    /**
     * Logs lifecycle entity and field-change information.
     */
    private void logLifecycleDetails(
            EEntityView inEntity,
            EFieldChangesView inOriginalFieldChanges,
            EFieldChanges inMoreFieldChanges,
            String inTriggerType) {

        if (inEntity == null) {

            LOGGER.warn(
                    "Entity View is NULL."
            )

            LOGGER.warn(
                    "Trigger: ${inTriggerType}"
            )

            return
        }

        String entityName =
                inEntity.getEntityName()

        LOGGER.info(
                "Entity: ${entityName}"
        )

        LOGGER.info(
                "Trigger: ${inTriggerType}"
        )

        LOGGER.info(
                "Entity View: ${inEntity}"
        )

        LOGGER.info(
                "Underlying Entity: ${inEntity._entity}"
        )

        if (inOriginalFieldChanges == null) {

            LOGGER.info(
                    "Original Field Changes: NULL"
            )

        } else {

            int fieldChangeCount =
                    inOriginalFieldChanges.getFieldChangeCount()

            LOGGER.info(
                    "Original Field Change Count: " +
                    "${fieldChangeCount}"
            )

            for (int i = 0;
                 i < fieldChangeCount;
                 i++) {

                def fieldChange =
                        inOriginalFieldChanges.getFieldChange(i)

                if (fieldChange == null) {

                    LOGGER.warn(
                            "Field Change [${i}]: NULL"
                    )

                    continue
                }

                LOGGER.info(
                        "Field Change [${i}]: ${fieldChange}"
                )

                LOGGER.info(
                        "Field ID [${i}]: " +
                        "${fieldChange.getFieldId()}"
                )
            }
        }

        if (inMoreFieldChanges == null) {

            LOGGER.info(
                    "More Field Changes: NULL"
            )

        } else {

            LOGGER.info(
                    "More Field Changes: " +
                    "${inMoreFieldChanges}"
            )
        }
    }

    /**
     * Loads and invokes the configured business-rule dispatcher.
     */
    private void invokeBusinessChangeDispatcher(
            EEntityView inEntity,
            EFieldChangesView inOriginalFieldChanges,
            EFieldChanges inMoreFieldChanges,
            String inTriggerType) {

        LOGGER.info(
                "--------------------------------------------------------------"
        )

        LOGGER.info(
                "BUSINESS CHANGE DISPATCHER INVOCATION STARTED"
        )

        LOGGER.info(
                "Dispatcher Library: " +
                "${BUSINESS_CHANGE_DISPATCHER}"
        )

        LOGGER.info(
                "Dispatcher Trigger: " +
                "${inTriggerType}"
        )

        if (inEntity == null) {

            LOGGER.warn(
                    "Dispatcher invocation skipped: Entity View is NULL."
            )

            return
        }

        LOGGER.info(
                "Dispatcher Entity: " +
                "${inEntity.getEntityName()}"
        )

        try {

            LOGGER.info(
                    "Calling getLibrary(" +
                    "'${BUSINESS_CHANGE_DISPATCHER}'" +
                    ")..."
            )

            def dispatcher =
                    getLibrary(
                            BUSINESS_CHANGE_DISPATCHER
                    )

            LOGGER.info(
                    "getLibrary() call completed."
            )

            if (dispatcher == null) {

                LOGGER.warn(
                        "BusinessChangeDispatcher library returned NULL."
                )

                LOGGER.warn(
                        "Configured library name: " +
                        "${BUSINESS_CHANGE_DISPATCHER}"
                )

                return
            }

            LOGGER.info(
                    "BusinessChangeDispatcher library FOUND."
            )

            LOGGER.info(
                    "Dispatcher Class: " +
                    "${dispatcher.getClass().getName()}"
            )

            Map<String, Object> dispatcherParameters =
                    new LinkedHashMap<String, Object>()

            dispatcherParameters.put(
                    PARAM_TRIGGER_TYPE,
                    inTriggerType
            )

            dispatcherParameters.put(
                    PARAM_ENTITY,
                    inEntity
            )

            dispatcherParameters.put(
                    PARAM_ORIGINAL_FIELD_CHANGES,
                    inOriginalFieldChanges
            )

            dispatcherParameters.put(
                    PARAM_MORE_FIELD_CHANGES,
                    inMoreFieldChanges
            )

            LOGGER.info(
                    "Dispatcher parameters prepared."
            )

            LOGGER.info(
                    "Invoking dispatcher.execute()..."
            )

            dispatcher.execute(
                    dispatcherParameters
            )

            LOGGER.info(
                    "dispatcher.execute() returned successfully."
            )

        } catch (Exception e) {

            LOGGER.warn(
                    "Exception while invoking " +
                    "BusinessChangeDispatcher: ",
                    e
            )
        }

        LOGGER.info(
                "BUSINESS CHANGE DISPATCHER INVOCATION COMPLETED"
        )

        LOGGER.info(
                "--------------------------------------------------------------"
        )
    }
}