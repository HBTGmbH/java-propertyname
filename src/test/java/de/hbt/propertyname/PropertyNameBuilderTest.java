package de.hbt.propertyname;

import static de.hbt.propertyname.PropertyNameBuilder.*;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

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

	class Contract extends AbstractEntity {
		public Contract() {
			throw new AssertionError();
		}

		public BusinessPartner getCustomer() {
			throw new AssertionError();
		}

		public List<BusinessPartner> getAgents() {
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
		// name(of(Function).getter) returning object
		assertThat(name(of(Contract::getCustomer).getLegalName())).isEqualTo("customer.legalName");
		// name(of(Function).getter) returning collection object
		assertThat(name(of(Contract::getCustomer).getAddresses())).isEqualTo("customer.addresses");
		// name(of(Function).getter) returning primitive
		assertThat(name(of(Contract::getCustomer).getVersion())).isEqualTo("customer.version");
		// nameOf(Function) returning object
		assertThat(nameOf(Contract::getCustomer)).isEqualTo("customer");
		// nameOf(Function) returning collection object
		assertThat(nameOf(Contract::getAgents)).isEqualTo("agents");
		// nameOf(Function) returning primitive
		assertThat(nameOf(Contract::getCreationDay)).isEqualTo("creationDay");
		// name(of(Runnable))
		assertThat(name(() -> of(Contract::getCustomer).getAddresses().forEach(a -> a.getCity())))
				.isEqualTo("customer.addresses.city");
	}

	@Test
	void methodReferencesShouldWorkWithSubclasses() {
		assertThat(nameOf(Contract::isArchived)).isEqualTo("archived");
		assertThat(name(() -> of(SalesContract::getCustomer).getAcronym())).isEqualTo("customer.acronym");
	}
}
