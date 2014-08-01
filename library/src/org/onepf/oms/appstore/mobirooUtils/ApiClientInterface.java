package org.onepf.oms.appstore.mobirooUtils;


public interface ApiClientInterface
{
	public abstract void executeAndBlock(final ApiClientHandler handler);
	public abstract void executeOnThread(final ApiClientHandler handler);
	public abstract void executeOnAsyncTask(final ApiClientHandler handler);
	public abstract void execute(final ApiClientHandler handler, ExecutionMode executionMode);
	public abstract void cancelAsyncTask();
	public abstract void cancelAsyncTask(final boolean mayInterruptIfRunning);
}
