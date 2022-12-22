/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.outboundsync.job.ItemChangeSender
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultChangesCollectorFactoryUnitTest extends Specification {
    def itemChangeSender = Stub ItemChangeSender
    def factory = new DefaultChangesCollectorFactory(itemChangeSender: itemChangeSender)

    @Test
    def "creates new instance of streaming changes collector"() {
        when:
        def collector = factory.create(Stub(OutboundSyncStreamConfigurationModel))

        then:
        StreamingChangesCollector.isCase collector
    }

    @Unroll
    @Test
    def "fails to create a new instance of streaming changes collector when #condition"() {
        given:
        def factory = new DefaultChangesCollectorFactory(itemChangeSender: changeSender)

        when:
        factory.create(stream)

        then:
        thrown IllegalArgumentException

        where:
        condition                      | changeSender           | stream
        'change sender is null'        | null                   | Stub(OutboundSyncStreamConfigurationModel)
        'stream configuration is null' | Stub(ItemChangeSender) | null
    }
}