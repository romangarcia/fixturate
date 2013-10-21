Fixturate
=========
   Author: Roman Garcia <romangarciam@gmail.com>

About
=====
Fixturate is a library that allows you to define test fixtures for your model in an easy / reusable way.

It really just is a text format and a convention to declare datasets to be used in testing.
Similar to Rails / Play fixtures, but instead of YAML markup, a simpler text
format, similar to Java Properties, but with a few extensions.

Characteristics
===============
 - Each model class needs to define it's own fixtures in a file located and named after the class, but with extension *.fixtures*
 - In the fixture file, you declare different scenarios, and for each scenario you declare model properties
 - Properties names use the JavaBean convention, and should be valid Java identifiers
 - To inject a Constructor, declare properties using the same order they are declared in the constructor
 - Literal properties can be String (using '"' quote character), Long, Double or Boolean
    - aStringProperty = "This is a valid String, always with quotes"
    - aLongProperty = 360
    - aDoubleProperty = 3.14
    - aBooleanProperty = true
 - References to other model fixtures are declared using a special syntax: '$[scenario]'
    - aUserRefProperty = $[user with valid credentials]
 - Lists / Sets are declared using a comma separator between values
    - aLiteralListProperty = "This", "is", "a", "list"
    - aRefListProperty = $[scenario 1], $[scenario 2], $[scenario 3]

Really early stages! I'm not even sure this crap is useful yet!

What's missing
==============
- Support for Dates / JodaTime
- Support for Optional properties
- Model validation (make sure no properties are left null?)
- all, any, any(5)...allow creating several fixtures on one call
- Custom Parsers / Custom Converters
- Get rid of the commons-beans dependency
