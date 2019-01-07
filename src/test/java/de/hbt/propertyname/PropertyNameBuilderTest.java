package de.hbt.propertyname;

import static de.hbt.propertyname.PropertyNameBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

class PropertyNameBuilderTest {
	class AbstractEntity {
		AbstractEntity() {
			throw new AssertionError();
		}

		long getVersion() {
			throw new AssertionError();
		}

		boolean isArchived() {
			throw new AssertionError();
		}

		@Override
		public String toString() {
			throw new AssertionError();
		}

		@Override
		public boolean equals(Object obj) {
			throw new AssertionError();
		}

		@Override
		public int hashCode() {
			throw new AssertionError();
		}
	}

	class Address extends AbstractEntity {
		Address() {
			throw new AssertionError();
		}

		String getCity() {
			throw new AssertionError();
		}

		List<Integer> getNumbers() {
			throw new AssertionError();
		}
	}

	class BusinessPartner extends AbstractEntity {
		BusinessPartner() {
			throw new AssertionError();
		}

		String getLegalName() {
			throw new AssertionError();
		}

		Set<Address> getAddresses() {
			throw new AssertionError();
		}

		String getAcronym() {
			throw new AssertionError();
		}
	}

	class ContractPosition extends AbstractEntity {
		ContractPosition() {
			throw new AssertionError();
		}

		BigDecimal getPrice() {
			throw new AssertionError();
		}
	}

	interface Shipment {
		Address getDestination();
	}

	class Contract extends AbstractEntity {
		Contract() {
			throw new AssertionError();
		}

		Shipment getShipment() {
			throw new AssertionError();
		}

		BusinessPartner getCustomer() {
			throw new AssertionError();
		}

		List<ContractPosition> getPositions() {
			throw new AssertionError();
		}

		Date getCreationDay() {
			throw new AssertionError();
		}
	}

	class SalesContract extends Contract {
		SalesContract() {
			throw new AssertionError();
		}
	}

	@Test
	void testNameOf() {
		assertThat(name(of(Contract::getCustomer).getLegalName())).isEqualTo("customer.legalName");
		assertThat(name(of(Contract::getCustomer).getAddresses())).isEqualTo("customer.addresses");
		assertThat(name(of(Contract::getCustomer).getVersion())).isEqualTo("customer.version");
		assertThat(name(of(Contract::getShipment).getDestination())).isEqualTo("shipment.destination");
		assertThat(nameOf(Contract::getCustomer)).isEqualTo("customer");
		assertThat(nameOf(Contract::getShipment)).isEqualTo("shipment");
		assertThat(nameOf(Contract::getPositions)).isEqualTo("positions");
		assertThat(nameOf(Contract::getCreationDay)).isEqualTo("creationDay");
		assertThat(name(any(of(Contract::getCustomer).getAddresses()).getCity())).isEqualTo("customer.addresses.city");
		assertThat(name(any(of(Contract::getCustomer).getAddresses()).getNumbers())).isEqualTo("customer.addresses.numbers");
	}

	@Test
	void methodReferencesShouldWorkWithSubclasses() {
		assertThat(nameOf(Contract::isArchived)).isEqualTo("archived");
		assertThat(name(of(SalesContract::getCustomer).getAcronym())).isEqualTo("customer.acronym");
		assertThat(name(any(of(SalesContract::getPositions)).getPrice())).isEqualTo("positions.price");
	}
}
