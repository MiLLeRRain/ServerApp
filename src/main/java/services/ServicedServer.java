package services;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import server.ServerUI;

public class ServicedServer extends Service <String>{


	String s;
	ServerUI su;

	public ServicedServer(String s, ServerUI serverUI)
	{
		this.s = s;
		this.su = serverUI;
	}

	@Override
	protected Task<String> createTask() {
		// TODO Auto-generated method stub

		return new Task <String>()
		{
			@Override
			protected String call() throws Exception {
				//Thread.sleep(10000);

				new ThreadedServer(su);

				return "Running???";
			}

			@Override protected void succeeded() {
				super.succeeded();
				updateMessage("Done!");
			}

			@Override protected void cancelled() {
				super.cancelled();
				updateMessage("Cancelled!");
			}

			@Override protected void failed() {
				super.failed();
				updateMessage("Failed!");
			}
		};

	}

}