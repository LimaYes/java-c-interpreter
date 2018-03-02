# Cover (Reloaded)

This Cover fork is an an improvement of the original cover project and supports int/uint, long/ulong as well as float and double datatypes.
Additionally, it comes along with many built-in mathematical operators which should work out of the box. Please give it a try.

# Quirky On-going Work

Please note, that the array implementation is far from perfect.
The global (and local) arrays are just working so-so, and their scope is not reflecting how arrays behave in C. For now, this is enough but for some more generic use, these features will need some more work. Hence, do not use this in productive environments and especially not when you are not 100% sure what you are doing. Generally speaking, use this code at your own risk.

# (Reliably) Supported Datatypes

Datatype | Description
--- | ---
int | 32-bit signed integer
uint | 32-bit unsigned integer
long | 64-bit signed integer
ulong | 64-bit unsigned integer
float | fixed point datatype (32 bit)
double | double precision datatype (64 bit)

# Supported Math Operators

Operator | Description
--- | ---
sinh( double d ) | Computes hyperbolic sine
sin( double d ) | Computes sine
cosh( double d ) | Computes hyperbolic cosine
cos( double d ) | Computes cosine
tanh( double d ) | Computes hyperbolic tangent
tan( double d ) | Computes tangent
asin( double d ) | Computes arc sine
acos( double d ) | Computes arc cosine
atan2( double d, double d ) | Computes arc tangent, using signs to determine quadrants
atan( double d ) | Computes arc tangent
exp( double d ) | Computes e raised to the given power
log10( double d ) | Computes common (base-10) logarithm
log2( double d ) | Computes common (base-2) logarithm
log( double d ) | Computes natural (base-e) logarithm
pow( double x, double y ) | Computes a number raised to the given power
sqrt( double d ) | Computes square root
ceil( double d ) | Computes smallest integer not less than the given value
floor( double d ) | Computes largest integer not greater than the given value
fabs( double d ) | Computes absolute value of a floating-point value
abs( int i ) | Computes absolute value of an integral value
fmod ( double x, double y ) | Computes remainder of the floating-point division operation
rotl32 ( int x, int y) | Performs a 32-bit left bit rotation
rotr32 ( int x, int y) | Performs a 32-bit right bit rotation
rotl64 ( long x, long y) | Performs a 64-bit left bit rotation
rotr64 ( long x, long y) | Performs a 64-bit right bit rotation

## FAQ

* **How fast is it?** For the *mandelbrot* benchmark Cover reaches 70% of the speed of C. See [Performance](PERFORMANCE.md) for details.
* **Can I use this for real world projects?** If the data types and built-in functions are suiting your needs, then sure!

## Design

Cover aims to support the following C++ features:
* standard C++ syntax
* basic types, control flow
* objects and multiple inheritance
* virtual functions
* basic standard library
* basic preprocessor support

Does NOT support the following C++ features:
* delete: everything is garbage collected
* pointer arithmetic and casting to incompatible types
* exceptions

## Prerequisites
* JDK 8
* maven3 

## Installation

You can actually skip all these steps except the last one. While the GraalVM is a nice thing to have, everything should still work somewhat with the original stock Java VM.

* Clone the repository using
  `git clone https://github.com/gerard-/cover.git`
* Download Graal VM Development Kit from 
  http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads
* Unpack the downloaded `graalvm_*.tar.gz` into `cover/graalvm`, or add a symlink.
* Verify that the file `cover/graalvm/bin/java` exists and is executable
* Execute `sh ./installcdt.sh`
* Execute `mvn compile package`

## IDE Setup 

### Eclipse
* Tested with Eclipse Mars SR2
* Open Eclipse with a new workspace
* Install `m2e` and `m2e-apt` plugins from the Eclipse marketplace (Help -> Eclipse Marketplace...)
* File -> Import... -> Existing Maven Projects -> Select `cover` folder -> Finish

### Netbeans
* Tested with Netbeans 8.1
* Open Netbeans
* File -> Open Project -> Select `cover` folder -> Open Project

### IntelliJ IDEA
* Tested with IntelliJ 2016.1.3 Community Edition
* Open IntelliJ IDEA
* File -> New -> Project from existing Sources -> Select `cover` folder -> Click next and keep everything default on several screens -> Finish

## Running

* Execute `./cover tests/HelloWorld.cover` to run a simple language source file.

## Debugging

* Execute `./cover -debug tests/HelloWorld.cpp`.
* Attach a Java remote debugger (like Eclipse) on port 8000.

## Further information

* [Truffle JavaDoc](http://lafo.ssw.uni-linz.ac.at/javadoc/truffle/latest/)
* [Truffle on Github](http://github.com/graalvm/truffle)
* [Graal on Github](http://github.com/graalvm/graal-core)
* [Truffle Tutorials and Presentations](https://wiki.openjdk.java.net/display/Graal/Publications+and+Presentations)
* [Truffle FAQ and Guidelines](https://wiki.openjdk.java.net/display/Graal/Truffle+FAQ+and+Guidelines)
* [Graal VM]( http://www.oracle.com/technetwork/oracle-labs/program-languages/overview) on the Oracle Technology Network
* [Papers on Truffle](http://ssw.jku.at/Research/Projects/JVM/Truffle.html)
* [Papers on Graal](http://ssw.jku.at/Research/Projects/JVM/Graal.html)
* [Original Cover on Github](https://github.com/gerard-/cover)

## License

Most of Cover is licensed under the [Apache License 2.0](LICENSE-APACHE). There are some [UPL](LICENSE-UPL) licensed files left from the SimpleLanguage implementation that was used as a base. Those are expected to be replaced soon.

