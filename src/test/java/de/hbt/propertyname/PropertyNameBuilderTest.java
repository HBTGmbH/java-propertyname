package de.hbt.propertyname;

import static de.hbt.propertyname.PropertyNameBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

class PropertyNameBuilderTest {
	class AbstractEntity {
		public AbstractEntity() {
			throw new AssertionError();
		}

		public long getVersion() {
			throw new AssertionError();
		}

		public boolean isArchived() {
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
		public Address() {
			throw new AssertionError();
		}

		public String getCity() {
			throw new AssertionError();
		}

		public List<Integer> getNumbers() {
			throw new AssertionError();
		}
	}

	class BusinessPartner extends AbstractEntity {
		public BusinessPartner() {
			throw new AssertionError();
		}

		public String getLegalName() {
			throw new AssertionError();
		}

		public Set<Address> getAddresses() {
			throw new AssertionError();
		}

		public String getAcronym() {
			throw new AssertionError();
		}
	}

	class ContractPosition extends AbstractEntity {
		public BigDecimal getPrice() {
			throw new AssertionError();
		}
	}

	class Contract extends AbstractEntity {
		public Contract() {
			throw new AssertionError();
		}

		public BusinessPartner getCustomer() {
			throw new AssertionError();
		}

		public List<ContractPosition> getPositions() {
			throw new AssertionError();
		}

		public Date getCreationDay() {
			throw new AssertionError();
		}
	}

	class SalesContract extends Contract {
		public SalesContract() {
			throw new AssertionError();
		}
	}

	@Test
	void testNameOf() {
		assertThat(name(of(Contract::getCustomer).getLegalName())).isEqualTo("customer.legalName");
		assertThat(name(of(Contract::getCustomer).getAddresses())).isEqualTo("customer.addresses");
		assertThat(name(of(Contract::getCustomer).getVersion())).isEqualTo("customer.version");
		assertThat(nameOf(Contract::getCustomer)).isEqualTo("customer");
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
