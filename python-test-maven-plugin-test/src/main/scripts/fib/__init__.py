
from local.fib import Fibonacci

def fibrange(n):
	return [Fibonacci.calc(i) for i in range(1, n + 1)]

