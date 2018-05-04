from freshen import *

from fib import fibrange

@When('I calculate the first (\d+) fibonacci numbers')
def calculate(n):
	n = int(n)
	ftc.fibval = fibrange(n) 

@Then('it should give me (\[.*\])')
def check_result(value):
	assert_equal(str(ftc.fibval), value)

