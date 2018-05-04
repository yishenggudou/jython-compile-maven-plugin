package local.fib;

import junit.framework.Assert;

import org.junit.Test;

public class FibonacciTest {

	@Test
	public void test() {
		Assert.assertEquals(Fibonacci.calc(1), 1);
		Assert.assertEquals(Fibonacci.calc(2), 1);
		Assert.assertEquals(Fibonacci.calc(3), 2);
	}

}
