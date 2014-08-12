package com.mqtt.mdmapp;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class DevicePolicy implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4207377300245386627L;

	@SerializedName(value = "enable_camera")
	private boolean enable_camera;

	@SerializedName(value = "encrypt_device")
	private boolean encrypt_device;

	@SerializedName(value = "password_strength")
	private int password_strength;

	@SerializedName(value = "password_quality")
	private int password_quality;

	@SerializedName(value = "minimum_numeric")
	private int minimum_numeric;

	@SerializedName(value = "mimimum_symbols")
	private int mimimum_symbols;

	@SerializedName(value = "minimum_numbers")
	private int minimum_numbers;

	@SerializedName(value = "minimum_lowercase")
	private int minimum_lowercase;

	@SerializedName(value = "minimum_uppercase")
	private int minimum_uppercase;

	@SerializedName(value = "password_length")
	private int password_length;

	@SerializedName(value = "password_expiry_timeout")
	private int password_expiry_timeout;

	public boolean isEnable_camera() {
		return enable_camera;
	}

	public void setEnable_camera(boolean enable_camera) {
		this.enable_camera = enable_camera;
	}

	public boolean isEncrypt_device() {
		return encrypt_device;
	}

	public void setEncrypt_device(boolean encrypt_device) {
		this.encrypt_device = encrypt_device;
	}

	public int getPassword_strength() {
		return password_strength;
	}

	public void setPassword_strength(int password_strength) {
		this.password_strength = password_strength;
	}

	public int getPassword_quality() {
		return password_quality;
	}

	public void setPassword_quality(int password_quality) {
		this.password_quality = password_quality;
	}

	public int getMinimum_numeric() {
		return minimum_numeric;
	}

	public void setMinimum_numeric(int minimum_numeric) {
		this.minimum_numeric = minimum_numeric;
	}

	public int getMimimum_symbols() {
		return mimimum_symbols;
	}

	public void setMimimum_symbols(int mimimum_symbols) {
		this.mimimum_symbols = mimimum_symbols;
	}

	public int getMinimum_numbers() {
		return minimum_numbers;
	}

	public void setMinimum_numbers(int minimum_numbers) {
		this.minimum_numbers = minimum_numbers;
	}

	public int getMinimum_lowercase() {
		return minimum_lowercase;
	}

	public void setMinimum_lowercase(int minimum_lowercase) {
		this.minimum_lowercase = minimum_lowercase;
	}

	public int getMinimum_uppercase() {
		return minimum_uppercase;
	}

	public void setMinimum_uppercase(int minimum_uppercase) {
		this.minimum_uppercase = minimum_uppercase;
	}

	public int getPassword_length() {
		return password_length;
	}

	public void setPassword_length(int password_length) {
		this.password_length = password_length;
	}

	public int getPassword_expiry_timeout() {
		return password_expiry_timeout;
	}

	public void setPassword_expiry_timeout(int password_expiry_timeout) {
		this.password_expiry_timeout = password_expiry_timeout;
	}

}
