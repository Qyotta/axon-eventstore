package de.qyotta.eventstore;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
// @formatter:off
@SuiteClasses({
   EventReaderTest.class,
   DeleteStreamTest.class,
   EventWriterTest.class,
   EventStreamTest.class,
   EventStreamReaderTest.class
})
//@formatter:on
public class AllTests {
   //
}
