package fr.emse.tsc;

import java.io.Serializable;

public class Token implements Serializable {

	private static final long serialVersionUID = -8326677957702206448L;

	public static final String ENROLL = "HELO";
	public static final String ACCESS = "ACCE";
	public static final String PRES   = "PRES";
	public static final String QUIT   = "QUIT";

	public static final String OK = "OK";
	public static final String KO = "KO";

	private String command;
	private String response;
	private String name;
	private String ipAddress;
	private String macAddress;
	private String challenge;

	public Token(String name, String ipAddress, String macAddress, String challenge) {
		this.name = name;
		this.response = OK;
		this.ipAddress = ipAddress;
		this.macAddress = macAddress;
		this.challenge = challenge;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {

		this.command = command;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public void append(int count) {
		StringBuilder sb = new StringBuilder(challenge);
		sb.insert(0, count);
		challenge = sb.toString();
	}

	public String toString() {
		return " (" + command + " " + response + " " + name + " " + ipAddress + " " + macAddress + " " + challenge + ")";
	}

}
