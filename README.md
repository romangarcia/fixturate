Fixturate
=========
   Author: Roman Garcia <romangarciam@gmail.com>

About
=====
Fixturate is a library that allows you to define test fixtures for your model in an easy / reusable way.

It really just is a text format and a convention to declare datasets to be used in testing.
Similar to Rails / Play fixtures, but instead of YAML markup, a simpler text
format, similar to Java Properties, but with a few extensions.

Quick Start
===========
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
 - Special scenario named "default" will be returned when no scenario is specified

Really early stages! I'm not even sure this crap is useful yet!

Example
=======

*User model* -- basic Java with constructor and mutable properties

    package fixturate.example;
    public class User {
        private final String firstName;
        private final String lastName;
        private int age;
        private boolean alive;

        public JavaUser(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }

        public boolean isAlive() {
            return alive;
        }
    }

*User fixture (file fixturate/example/User.fixtures)*

    [john user alive]
    firstName = "John"
    lastName  = "Doe"
    age       = 65
    alive     = true

    [jane user dead]
    firstName = "Jane"
    lastName  = "Doe"
    age       = 60
    alive     = false

*Order model* -- Java bean with constructor with reference to User

    package fixturate.example;
    public class Order {
        private final Long item;
        private final String description;
        private final User user;

        public JavaOrder(Long item, String description, User user) {
            this.item = item;
            this.description = description;
            this.user = user;
        }

        public Long getItem() {
            return item;
        }

        public String getDescription() {
            return description;
        }

        public User getUser() {
            return user;
        }
    }

*Order fixture (file fixturate/example/Order.fixtures)*

    [default]
    item           = 4321
    description    = "Just a default order"
    user           = $[john user alive]

    [product one for john]
    item           = 1
    description    = "Product One"
    user           = $[john user alive]

    [product two for jane]
    item           = 2
    description    = "Product Two"
    user           = $[jane user dead]

*Invoice Model* -- another via constructor, this time with a Java Collection involved

    package fixturate.example;
    public class Invoice {

        private final Long number;

        private final List<Order> orders;

        public JavaInvoice(Long number, List<Order> orders) {
            this.number = number;
            this.orders = orders;
        }

        public Long getNumber() {
            return number;
        }

        public List<Order> getOrders() {
            return orders;
        }

    }

*Invoice fixtures (file fixturate/example/Invoice.fixtures)*

    [default]
    number         = 1
    orders    	   = $[product one for john], $[product two for jane]

What's missing
==============
- Support for Dates / JodaTime
- Support for Optional properties
- Model validation (make sure no properties are left null?)
- all, any, any(5)...allow creating several fixtures on one call
- Custom Parsers / Custom Converters
- Get rid of the commons-beans dependency
- Tool to export JavaBeans to fixtures datasets
