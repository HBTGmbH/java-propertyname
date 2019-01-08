package de.hbt.propertyname;

import static de.hbt.propertyname.PropertyNameBuilder.*;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class Bench {

	class A {
		public int getA() {
			throw new AssertionError();
		}

		public B getB() {
			throw new AssertionError();
		}

		public String getC() {
			throw new AssertionError();
		}

		public Collection<B> getBs() {
			throw new AssertionError();
		}
	}

	class B {
		public A getA() {
			throw new AssertionError();
		}

		public B getB() {
			throw new AssertionError();
		}
	}

	@Benchmark
	public String single() {
		return nameOf(A::getA);
	}

	@Benchmark
	public String chain() {
		return name(of(A::getB));
	}

	@Benchmark
	public String collection() {
		return name(any(of(A::getB).getA().getBs()));
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(Bench.class.getSimpleName()).forks(1).build();
		new Runner(opt).run();
	}
}
