
print "Python started."

from com.github.yishenggudou.mavenjython.test import FibSequenceCalc

import nose

def main():
	fc = FibSequenceCalc()
	print dir(nose)
	print [fc.calc() for i in dir(nose)]

