package local.fib;

public class Fibonacci {
	public static int calc(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Not defined for " + n);
		if (n == 1)
			return 1;
		if (n == 2)
			return 1;
		else
			return calc(n - 1) + calc(n - 2);
	}
}
