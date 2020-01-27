package hosh.testsupport;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class WithExecutor implements AfterAllCallback {

	private final ExecutorService executorService;

	public WithExecutor(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public Future<?> submit(Runnable task) {
		return executorService.submit(task);
	}

	@Override
	public void afterAll(ExtensionContext extensionContext) {
		executorService.shutdown();
	}

}
