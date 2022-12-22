/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.deltadetection.ItemChangeDTO
import de.hybris.platform.core.PK
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.outboundsync.dto.OutboundItemDTO
import de.hybris.platform.outboundsync.dto.impl.DeltaDetectionOutboundItemChange
import de.hybris.platform.outboundsync.job.ItemChangeSender
import de.hybris.platform.outboundsync.model.OutboundChannelConfigurationModel
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class StreamingChangesCollectorUnitTest extends Specification {
    private static final Long IO_PK = 1L
    private static final Long OUTBOUND_CHANNEL_CONFIG_PK = 2L
    def streamConfiguration = Stub(OutboundSyncStreamConfigurationModel) {
        getOutboundChannelConfiguration() >> Stub(OutboundChannelConfigurationModel) {
            getIntegrationObject() >> Stub(IntegrationObjectModel) {
                getPk() >> PK.fromLong(IO_PK)
            }
            getPk() >> PK.fromLong(OUTBOUND_CHANNEL_CONFIG_PK)
        }
    }
    def itemChangeSender = Stub(ItemChangeSender)
    def streamChangesCollector = new StreamingChangesCollector(itemChangeSender, streamConfiguration)

    @Test
    @Unroll
    def "cannot be created with a null #propertyName"() {
        when:
        new StreamingChangesCollector(itemChangeSenderVal, streamConfigVal)

        then:
        thrown(IllegalArgumentException)

        where:
        propertyName          | streamConfigVal                            | itemChangeSenderVal
        'itemChangeSender'    | Stub(OutboundSyncStreamConfigurationModel) | null
        'streamConfiguration' | null                                       | Stub(ItemChangeSender)
    }

    @Test
    def "collect sends the expected OutboutboundItemChangeDTO"() {
        when:
        def collectResp = streamChangesCollector.collect(Stub(ItemChangeDTO))

        then:
        itemChangeSender.send(_ as OutboundItemDTO) >> { args ->
            with(args[0]) {
                integrationObjectPK == IO_PK
                channelConfigurationPK == OUTBOUND_CHANNEL_CONFIG_PK
                item instanceof DeltaDetectionOutboundItemChange
            }
        }
        collectResp
    }

    @Test
    def "collect rethrows IllegalArgumentException"() {
        when:
        streamChangesCollector.collect(null)

        then:
        thrown(IllegalArgumentException)
    }
}