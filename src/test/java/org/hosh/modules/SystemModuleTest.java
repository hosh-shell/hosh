package org.hosh.modules;

import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.util.Arrays;

import org.hosh.modules.SystemModule.Env;
import org.hosh.modules.SystemModule.Exit;
import org.hosh.runtime.Version;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({ 
	SystemModuleTest.ExitTest.class,
	SystemModuleTest.EnvTest.class,
})
public class SystemModuleTest {

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ExitTest {

		@Rule
		public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Exit sut;

		@Test
		public void noArgs() {
			expectedSystemExit.expectSystemExitWithStatus(0);
			sut.run(Arrays.asList(), out, err);
		}

		@Test
		public void oneValidArg() {
			expectedSystemExit.expectSystemExitWithStatus(1);
			sut.run(Arrays.asList("1"), out, err);
		}

		@Test
		public void oneInvalidArg() {
			sut.run(Arrays.asList("asd"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("arg must be a number (0-999)")));
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("1", "2"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("too many parameters")));
		}
	}
	

}
