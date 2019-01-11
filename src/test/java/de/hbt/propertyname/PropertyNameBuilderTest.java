package de.hbt.propertyname;

import static de.hbt.propertyname.PropertyNameBuilder.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class PropertyNameBuilderTest {
	abstract class AbstractEntity<T extends Number> {
		AbstractEntity() {
			throw new AssertionError();
		}

		T getVersion() {
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

	class Address extends AbstractEntity<Byte> {
		Address() {
			throw new AssertionError();
		}

		boolean isArchived() {
			throw new AssertionError();
		}

		String getCity() {
			throw new AssertionError();
		}

		List<Integer> getNumbers() {
			throw new AssertionError();
		}
	}

	class BusinessPartner extends AbstractEntity<Short> {
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

	class ContractPosition extends AbstractEntity<Integer> {
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

	class Contract extends AbstractEntity<Long> {
		Contract() {
			throw new AssertionError();
		}

		Long getVersion() {
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
		String nonGetterMethod() {
			return "";
		}
	}

	static Object[][] testSource() {
		return new Object[][] {
			{name(of(Contract::getCustomer).getLegalName()), "customer.legalName"},
			{name(of((Contract c) -> c.getCustomer().getLegalName())), "customer.legalName"},
			{name(of(Contract::getCustomer).getAddresses()), "customer.addresses"},
			{name(of(Contract::getCustomer).getVersion()), "customer.version"},
			{name(of(Contract::getShipment).getDestination()), "shipment.destination"},
			{name(of((Contract c) -> c.getShipment()).getDestination()), "shipment.destination"},
			{nameOf(Contract::getVersion), "version"},
			{nameOf((Contract c) -> c.getVersion()), "version"},
			{nameOf(Contract::getCustomer), "customer"},
			{nameOf(Contract::getShipment), "shipment"},
			{nameOf(Contract::getPositions), "positions"},
			{nameOf(Contract::getCreationDay), "creationDay"},
			{name(any(of(Contract::getCustomer).getAddresses()).getCity()), "customer.addresses.city"},
			{name(any(of((Contract c) -> c.getCustomer().getAddresses())).getCity()), "customer.addresses.city"},
			{name(any(of(Contract::getCustomer).getAddresses()).getNumbers()), "customer.addresses.numbers"},
			{nameOf(Contract::isArchived), "archived"},
			{name(of(SalesContract::getCustomer).getAcronym()), "customer.acronym"},
			{name(any(of(SalesContract::getPositions)).getPrice()), "positions.price"}
		};
	}

	@ParameterizedTest
	@MethodSource("testSource")
	void testNameOf(String actual, String expected) {
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void methodOnObjectThrows() {
		assertThatExceptionOfType(PropertyNameException.class).isThrownBy(() -> nameOf(Object::getClass))
				.withMessage("Calling methods declared by Object is unsupported");
	}

	@Test
	void callingNonGetterMethodReturnsNull() {
		assertThat(nameOf(SalesContract::nonGetterMethod)).isNull();
	}

}
