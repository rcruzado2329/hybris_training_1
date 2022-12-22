package de.hybris.platform.warehousing.sourcing.ban.service;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.ordersplitting.model.WarehouseModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.time.TimeService;
import de.hybris.platform.warehousing.sourcing.ban.dao.SourcingBanDao;
import de.hybris.platform.warehousing.sourcing.ban.service.impl.DefaultSourcingBanService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.web.AppletConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultSourcingBanServiceTest
{
	@Mock
	private SourcingBanDao sourcingBanDao;

	@Mock
	private ConfigurationService configurationService;

	@Mock
	private TimeService timeService;

	private DefaultSourcingBanService service = new DefaultSourcingBanService();

	@Before
	public void before()
	{
		service.setTimeService(timeService);
		service.setConfigurationService(configurationService);
		service.setSourcingBanDao(sourcingBanDao);

		final BaseConfiguration configs = new BaseConfiguration();
		configs.setProperty("warehousing.ban.toobusy.days", 1);
		when(configurationService.getConfiguration()).thenReturn(configs);
	}

	@Test
	public void getSouringBanAlignWithHour()
	{
		final Collection<WarehouseModel> warehouses = Arrays.asList(new WarehouseModel());

		final Date currentTime = new Date();
		final long alignedMs = currentTime.getTime() - currentTime.getTime() % (3600 * 1000);
		final long withoutBanMs = alignedMs - 24 * 3600 * 1000;

		when(timeService.getCurrentTime()).thenReturn(currentTime);
		service.getSourcingBan(warehouses);
		verify(sourcingBanDao).getSourcingBan(warehouses, new Date(withoutBanMs));

	}
}