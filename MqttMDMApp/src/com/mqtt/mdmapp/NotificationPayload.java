package com.mqtt.mdmapp;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class NotificationPayload implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5473248901854523736L;

	@SerializedName(value = "command")
	private String command;

	@SerializedName(value = "message")
	private String message;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
