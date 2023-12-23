This module contains helpers specifically designed for spi classes.

It should have a module-info.java, like:
```
module hosh.spi.test.support {
       requires hosh.spi;
       requires org.assertj.core;
       requires org.mockito;
}
```

but I was not able to make it work with JUnit5/Maven.