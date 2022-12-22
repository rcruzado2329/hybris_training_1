/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.outboundsync.job.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.deltadetection.ChangeDetectionService
import de.hybris.deltadetection.ChangesCollector
import de.hybris.deltadetection.ItemChangeDTO
import de.hybris.platform.core.PK
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.outboundsync.job.ChangesCollectorFactory
import de.hybris.platform.outboundsync.job.ItemChangeSender
import de.hybris.platform.outboundsync.model.OutboundChannelConfigurationModel
import de.hybris.platform.outboundsync.model.OutboundSyncCronJobModel
import de.hybris.platform.outboundsync.model.OutboundSyncJobModel
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationContainerModel
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class OutboundSyncCronJobPerformableUnitTest extends Specification {
    def changeDetectionService = Mock ChangeDetectionService
    def itemChangeSender = Stub(ItemChangeSender)
    def changesCollectorFactory = Stub ChangesCollectorFactory

    def cronJobPerformable = new OutboundSyncCronJobPerformable(
            changeDetectionService: changeDetectionService,
            changesCollectorFactory: changesCollectorFactory,
            itemChangeSender: itemChangeSender)

    @Test
    def "does not collectChangesForType when changesCollectorFactory is null"() {
        given:
        cronJobPerformable.setChangesCollectorFactory(null)
        def cronJob = defaultCronJob([productStream: "Product"])

        when:
        def result = cronJobPerformable.perform(cronJob)

        then:
        CronJobResult.SUCCESS == result.getResult()
        CronJobStatus.FINISHED == result.getStatus()
        0 * changeDetectionService.collectChangesForType(_, _, _)
    }

    @Test
    def "perform results in error when stream configuration container is null"() {
        given:
        def cronJob = Stub(OutboundSyncCronJobModel) {
            getJob() >> Stub(OutboundSyncJobModel) {
                getStreamConfigurationContainer() >> null
            }
        }

        when:
        def result = cronJobPerformable.perform(cronJob)

        then:
        CronJobResult.ERROR == result.getResult()
        CronJobStatus.FINISHED == result.getStatus()
    }

    @Test
    def "perform results in failure when stream configuration collectChangesFromConfigurations throws RuntimeException"() {
        given:
        def cronJob = defaultCronJob([productStream: "Product"])

        changeDetectionService.collectChangesForType(_, _, _) >> { throw new RuntimeException() }

        when:
        def result = cronJobPerformable.perform(cronJob)

        then:
        CronJobResult.FAILURE == result.getResult()
        CronJobStatus.FINISHED == result.getStatus()
    }

    @Test
    @Unroll
    def "perform results in success with #streamIdTypeCodeMap and changes #changes"() {
        given:
        def cronJob = defaultCronJob(streamIdTypeCodeMap)

        when:
        def result = cronJobPerformable.perform(cronJob)

        then:
        CronJobResult.SUCCESS == result.getResult()
        CronJobStatus.FINISHED == result.getStatus()
        changeDetectionService.collectChangesForType(_, _, _) >> { args -> assert args[1].getItemSelector() == "whereClause" }

        where:
        streamIdTypeCodeMap                                    | changes
        [productStream: "Product"]                             | [Stub(ItemChangeDTO)]
        [productStream: "Product"]                             | []
        [productStream: "Product", categoryStream: "Category"] | [Stub(ItemChangeDTO)]
        [productStream: "Product", productStream2: "Product"]  | [Stub(ItemChangeDTO)]
        [:]                                                    | [Stub(ItemChangeDTO)]
    }

    @Test
    def "exception thrown in factory is propagated to cronjob performable"() {
        given:
        changesCollectorFactory.create(_) >> { throw new IllegalArgumentException() }

        when:
        def result = cronJobPerformable.perform(defaultCronJob([productStream: "Product"]))

        then:
        CronJobResult.FAILURE == result.getResult()
        CronJobStatus.FINISHED == result.getStatus()
    }

    @Test
    def "change detection service uses changes collector returned from factory"() {
        given:
        def changesCollector = Stub ChangesCollector
        changesCollectorFactory.create(_) >> changesCollector

        when:
        cronJobPerformable.perform(defaultCronJob([:]))

        then:
        changeDetectionService.collectChangesForType(_, _, _) >> { args ->
            assert args[2] == changesCollector
        }
    }

    def defaultCronJob(Map streamIdTypeCodeMap) {
        Stub(OutboundSyncCronJobModel) {
            getJob() >> Stub(OutboundSyncJobModel) {
                getStreamConfigurationContainer() >> Stub(OutboundSyncStreamConfigurationContainerModel) {
                    getConfigurations() >> streamConfigurations(streamIdTypeCodeMap)
                    getId() >> "outboundSyncDataStreams"
                }
            }
        }
    }

    def streamConfigurations(Map streamIdTypeCodeMap) {
        streamIdTypeCodeMap.collect {
            streamId, typeCode -> streamConfiguration(streamId, typeCode)
        }
    }

    def streamConfiguration(def streamId, def typeCode) {
        Stub(OutboundSyncStreamConfigurationModel) {
            getStreamId() >> streamId
            getItemTypeForStream() >> Stub(ComposedTypeModel) {
                getCode() >> typeCode
            }
            getOutboundChannelConfiguration() >> Stub(OutboundChannelConfigurationModel) {
                getIntegrationObject() >> Stub(IntegrationObjectModel) {
                    getPk() >> PK.fromLong(1)
                }

                getPk() >> PK.fromLong(2)

            }
            getWhereClause() >> "whereClause"
        }
    }
}