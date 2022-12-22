/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.storelocator;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.basecommerce.enums.PointOfServiceTypeEnum;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.storelocator.impl.CommerceMockGeoWebServiceWrapper;
import de.hybris.platform.storelocator.location.Location;
import de.hybris.platform.storelocator.location.impl.LocationDTO;
import de.hybris.platform.storelocator.location.impl.LocationDtoWrapper;
import de.hybris.platform.storelocator.map.Map;
import de.hybris.platform.storelocator.map.impl.DefaultMapService;
import de.hybris.platform.storelocator.model.PointOfServiceModel;
import de.hybris.platform.storelocator.route.DistanceAndRoute;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.spi.LoggerFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;


@IntegrationTest
public class GeocodingTest extends AbstractGeocodingTest
{
	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(GeocodingTest.class);
	@Resource
	private GeoWebServiceWrapper geoServiceWrapper;

	private Location center;
	private Location poi1;
	private Location poi2;
	private Location start;
	private Location destination;

	@Before
	public void setUp() throws Exception
	{
		//assume
		Assume.assumeThat(
				"Test should be executed only when mock geo web service wrapper is configured"
						+ CommerceMockGeoWebServiceWrapper.class.getName(),
				geoServiceWrapper, instanceOf(CommerceMockGeoWebServiceWrapper.class));

		createCoreData();
		createTestCronJob(Integer.valueOf(10), Integer.valueOf(1));

		//create test locations
		center = createAndStoreTestLocation("center", "Nymphenburger strasse", "86", "80636", "Munchen", "DE");
		poi1 = createAndStoreTestLocation("poi1", "Nymphenburger strasse", "1", "80636", "Munchen", "DE");
		poi2 = createAndStoreTestLocation("poi2", "Nymphenburger strasse", "10", "80636", "Munchen", "DE");
		start = createAndStoreTestLocation("hybris Muenchen", "Nymphenburger Strasse", "86", "80636", "Munchen", "DE");
		destination = createAndStoreTestLocation("locationX", "Nymphenburger Strasse", "100", "80636", "Munchen", "DE");

		//geocode by means of cronjob
		getGeocodeAddressesJob().perform(getCronJobService().getCronJob("testCronJob"));
		//refresh locations
		start = getLocationService().getLocationByName(start.getName());
		destination = getLocationService().getLocationByName(destination.getName());
		center = getLocationService().getLocationByName(center.getName());
		poi1 = getLocationService().getLocationByName(poi1.getName());
		poi2 = getLocationService().getLocationByName(poi2.getName());
	}

	/**
	 * Tests instantiation of {@link Location} by means of {@link LocationDTO} object. Verifies location service
	 * operation against this location as well as proper geocoding of it's address data.
	 *
	 * @throws Exception
	 */
	@Test
	public void testLocationDTO() throws Exception
	{
		final LocationDTO dto = new LocationDTO();
		dto.setName("testLocation");
		dto.setStreet("Nymphenburger strasse");
		dto.setBuildingNo("1");
		dto.setPostalCode("80636");
		dto.setCity("Muenchen");
		dto.setCountryIsoCode("DE");
		dto.setDescription("testDescription");
		dto.setType(LocationDTO.LOCATION_TYPE_POS);

		final LocationDtoWrapper wrapper = new LocationDtoWrapper(dto);
		//save to data base
		assertTrue("Saving location from DTO failed", getLocationService().saveOrUpdateLocation(wrapper));
		//fetch by name
		Location resultingLocation = getLocationService().getLocationByName("testLocation");
		assertEquals("Name of the location not as expected", dto.getName(), resultingLocation.getName());
		assertEquals("Location address data not as expected", wrapper.getAddressData(), resultingLocation.getAddressData());

		getGeocodeAddressesJob().perform(getCronJobService().getCronJob("testCronJob"));

		resultingLocation = getLocationService().getLocationByName("testLocation");
		assertNotNull("After cron job execution location should be geocoded", resultingLocation.getGPS());

		assertTrue(getLocationService().deleteLocation(resultingLocation));
		assertNull(getLocationService().getLocationByName(resultingLocation.getName()));
	}

	/**
	 * Tests instantiation of {@link Location} by means of {@link PointOfServiceModel} object. Verifies location service
	 * operation against this location as well as proper geocoding of it's address data.
	 *
	 * @throws Exception
	 */
	@Test
	public void testLocationModel() throws Exception
	{
		final PointOfServiceModel posModel = getModelService().create(PointOfServiceModel.class);
		final String testLocationName = "testPOS";
		posModel.setName(testLocationName);
		posModel.setType(PointOfServiceTypeEnum.WAREHOUSE);

		final AddressModel addressModel = getModelService().create(AddressModel.class);
		addressModel.setOwner(posModel);
		addressModel.setStreetname("Dachauer strasse");
		addressModel.setStreetnumber("5");
		addressModel.setPostalcode("80636");
		addressModel.setTown("Muenchen");
		addressModel.setCountry(getCommonI18NService().getCountry("DE"));
		posModel.setAddress(addressModel);

		getModelService().saveAll();

		Location posLocation = getLocationService().getLocationByName(testLocationName);
		assertNotNull("Location saved as model was null", posLocation);
		assertTrue("Address of the location was not as expexted", posLocation.getAddressData().addressEquals(addressModel));
		assertNull("Before the geocoding cron job is triggered, location should not be geocoded", posLocation.getGPS());

		//triggering cronjob
		getGeocodeAddressesJob().perform(getCronJobService().getCronJob("testCronJob"));
		posLocation = getLocationService().getLocationByName(testLocationName);
		assertNotNull("After the geocoding cron job was triggered, location should be geocoded", posLocation.getGPS());

		assertTrue(getLocationService().deleteLocation(posLocation));
		assertNull(getLocationService().getLocationByName(posLocation.getName()));

	}


	/**
	 * Test - getMap(IGPS, radius, addresses) returns a map centered on the point given and covering the radius
	 * specified, and listing the addresses given
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetMapFromManagerPOIs() throws Exception
	{
		final Location center = getLocationService().getLocation("Zygmunta Starego", "11", "44100", "Gliwice", "PL", true);

		final List<Location> pois = new ArrayList<Location>();
		pois.add(poi1);
		pois.add(poi2);
		final String title = "test title";
		final Map map = getMapService().getMap(center.getGPS(), 3, pois, title);
		assertNotNull("Map was null", map);
		assertTrue("Map's radius was supposed to be 3", 3 == map.getRadius());
		assertEquals("Map's title not as expected", title, map.getTitle());
		assertNotNull(map.getPointsOfInterest());
		assertEquals(poi1, map.getPointsOfInterest().get(0));
		assertEquals(poi2, map.getPointsOfInterest().get(1));
		assertTrue("Two POIs expected on the map", 2 == map.getPointsOfInterest().size());

	}


	/**
	 * Test - getMap(..) return a map with the route shown between the two points
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetMapFromManagerRoute() throws Exception
	{

		final List<Location> pois = new ArrayList<Location>();
		pois.add(poi1);

		final String title = "test title";
		final Map map = getMapService().getMap(center.getGPS(), 3, pois, title, poi1);
		assertNotNull("Map was null", map);
		assertTrue("Map's radius was supposed to be 3", 3 == map.getRadius());
		assertEquals("Map's title not as expected", title, map.getTitle());
		assertNotNull("Map should contain 1 POI", map.getPointsOfInterest());
		assertEquals("POI not as expexted", poi1, map.getPointsOfInterest().get(0));
		assertNotNull("Map should contain a route", map.getDistanceAndRoute());
		assertEquals("Route start not as expected", center.getGPS(), map.getDistanceAndRoute().getRoute().getStart());
		assertEquals("Route destination not as expected", poi1, map.getDistanceAndRoute().getRoute().getDestination());

	}

	/**
	 * Test comunication with Google API - getting route between 2 point and distance info.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRouteInstantiation() throws Exception
	{
		final DistanceAndRoute dnr = getRouteService().getDistanceAndRoute(start, destination);
		assertEquals(0.3, dnr.getEagleFliesDistance(), 0.05);
		assertEquals(309, dnr.getRoadDistance(), 10);
		assertNotNull("Google route information was null", dnr.getRoute());
	}

	/**
	 * Tests IGeoWebServiceWrapper, which returns a route for a well defined inputs: Address and Address
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDistanceAndRoute1() throws Exception
	{
		final DistanceAndRoute result = geoServiceWrapper.getDistanceAndRoute(start, destination);
		assertNotNull("Resulting IDistanceAndRoute object was null", result);
		assertNotNull("Resulting IRoute object was null", result.getRoute());
		assertTrue("Road distance between two known test points should be determined", result.getRoadDistance() != 0);
		assertTrue("'Eagle flies' distance between two known test points should be determined",
				result.getEagleFliesDistance() != 0);
	}

	/**
	 * Tests IGeoWebServiceWrapper, which returns a route for a well defined inputs: GPS and Address
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDistanceAndRoute2() throws Exception
	{
		final DistanceAndRoute result = geoServiceWrapper.getDistanceAndRoute(start.getGPS(), destination);
		assertNotNull("Resulting IDistanceAndRoute object was null", result);
		assertNotNull("Resulting IRoute object was null", result.getRoute());
		assertTrue("Road distance between two known test points should be determined", result.getRoadDistance() != 0);
		assertTrue("'Eagle flies' distance between two known test points should be determined",
				result.getEagleFliesDistance() != 0);
	}

}
