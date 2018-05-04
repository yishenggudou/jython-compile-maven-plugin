
def test():
	assert True

def test_alg():
	assert 1 + 1 == 2

def fun(i):
	return i+1

def testfun():
	assert fun(1) == 2

from local.fib import Fibonacci

def testfib():
	assert Fibonacci.calc(1) == 1 
	assert Fibonacci.calc(2) == 1
	assert Fibonacci.calc(3) == 2 
	assert Fibonacci.calc(4) == 3 
