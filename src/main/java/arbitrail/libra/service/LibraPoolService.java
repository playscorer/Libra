package arbitrail.libra.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class LibraPoolService {

	private final static Logger LOG = Logger.getLogger(LibraPoolService.class);

	private final static int NB_THREADS = 2;

	private static final ThreadFactory factory = new ExceptionThreadFactory(new ExceptionHandler());

	private static final ExecutorService pool = Executors.newFixedThreadPool(NB_THREADS, factory);

	public void startService(Runnable service) {
		pool.execute(service);
	}

	private static class ExceptionThreadFactory implements ThreadFactory {
		
		private static final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
		
		private final Thread.UncaughtExceptionHandler handler;

		public ExceptionThreadFactory(Thread.UncaughtExceptionHandler handler) {
			this.handler = handler;
		}

		@Override
		public Thread newThread(Runnable service) {
			Thread thread = defaultFactory.newThread(service);
			thread.setUncaughtExceptionHandler(handler);
			return thread;
		}
	}

	private static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable t) {
			LOG.fatal("Uncaught Exception in Thread " + thread.getId() + " - " + thread.getName(), t);
		}
	}
}
