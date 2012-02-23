package fr.emse.tscserver;

public class TENProtocol {

    private static final int HELO = 0;
    private static final int ACCESS = 1;
    private static final int INFO = 2;
    private static final int QUIT = 3;
    private String[] commands = { "HELO", "ACCESS", "INFO", "QUIT" };

	String connect(String name, String request) {
		if (request.equals(commands[HELO] + " " + name)) return "OK";
		else return null;
	}

	public String processInput(String request) {
        String theOutput = null;
        for (int i=1; i<commands.length; i++) {
        	if (request.startsWith(commands[i])) theOutput = processCommand(i, request);
        }
        return theOutput;
    }

	private String processCommand(int idComm, String request) {
		String response = null;
		switch (idComm) {
		case ACCESS:
			break;
		case INFO:
			break;
		case QUIT:
			response = request;
			break;
		default:
			break;
		}
		return response;
	}

}
