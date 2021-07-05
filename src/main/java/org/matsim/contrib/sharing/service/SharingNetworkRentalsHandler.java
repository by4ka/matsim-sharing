package org.matsim.contrib.sharing.service;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.events.SharingDropoffEvent;
import org.matsim.contrib.sharing.service.events.SharingDropoffEventHandler;
import org.matsim.contrib.sharing.service.events.SharingPickupEvent;
import org.matsim.contrib.sharing.service.events.SharingPickupEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;

public class SharingNetworkRentalsHandler implements SharingPickupEventHandler, SharingDropoffEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {
	private EventsManager eventsManager;
	private SharingServiceConfigGroup serviceParams;
	private final Network network;
	private Map<Id<Person>, SharingPickupEvent> pickups = new HashMap<>();
	private Map<Id<Person>, Double> distance = new HashMap<>();
	private Map<Id<Vehicle>, Id<Person>> personToVehicle = new HashMap<>();
	public static final String PERSON_MONEY_EVENT_PURPOSE_SHARING_FARE = "sharingFareDistance";

	
	public SharingNetworkRentalsHandler(EventsManager eventsManager, SharingServiceConfigGroup serviceParams, Network network) {
		this.eventsManager = eventsManager;
		this.serviceParams = serviceParams;
		this.network = network;
	}

	@Override
	public void handleEvent(SharingPickupEvent event) {

		if (event.getServiceId().toString().equals(serviceParams.getId())) {
			pickups.put(event.getPersonId(), event);
		}
	}	
	
	@Override
	public void reset(int iteration) {
		pickups.clear();
		distance.clear();
		personToVehicle.clear();
	}

	@Override
	public void handleEvent(SharingDropoffEvent event) {

		if (event.getServiceId().toString().equals(serviceParams.getId())) {
			Verify.verify(this.distance.containsKey(event.getPersonId()));
			double sharedFare = this.distance.get(event.getPersonId()) * this.serviceParams.getDistanceFare();
			eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), sharedFare,  PERSON_MONEY_EVENT_PURPOSE_SHARING_FARE, event.getServiceId().toString()));
			this.distance.remove(event.getPersonId());
			this.pickups.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.personToVehicle.get(event.getVehicleId());
		if (pickups.containsKey(personId)) {
			Link link = this.network.getLinks().get(event.getLinkId());
			distance.compute(personId, (k,v) -> v == null ? link.getLength() : v + link.getLength());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {

		if (pickups.containsKey(event.getPersonId())) {
			personToVehicle.put(event.getVehicleId(), event.getPersonId());
			
		}
	}

}
