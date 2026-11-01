This module contains JUnit helpers (extensions like `WithTimeZone`, `TemporaryFolder`) and fitness tests.

It should have a module-info.java, like:
```
module hosh.test.support {
   
      requires org.junit.jupiter.api;
      requires org.junit.jupiter.params;
     
      requires com.tngtech.archunit;
      requires com.tngtech.archunit.junit5.api;

      requires org.assertj.core;
      requires org.mockito;
}
```

but I was not able to make it work with JUnit5/Maven.