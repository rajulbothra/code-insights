package com.sap.codeinsights;

import java.util.List;

import org.apache.commons.validator.routines.UrlValidator;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class CodeRequest {
	private String url;
	private String processorType;
	private List<Coder> result;

	public CodeRequest(String url, String processorType) {
		this.url = url;
		this.processorType = processorType;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getProcessorType() {
		return processorType;
	}

	public void setProcessorType(String processorType) {
		this.processorType = processorType;
	}

    public JsonObject toJson() {
        JsonParser parser = new JsonParser();
        return parser.parse(this.toString()).getAsJsonObject();
    }

	public void setResult(List<Coder> result) {
		this.result = result;
	}

	public List<Coder> getResult() {
		return result;
	}

	public Error getValidity() {
		if (url == null || url.trim().empty()) return new Error("No URL Provided", MISSING_URL);

		UrlValidator urlValidator = new UrlValidator();
		if (!urlValidator.isValid(url)) return new Error("Invalid URL", INVALID_URL);

		if (!processorType.equalsIgnoreCase("documentation")) return new Error("Invalid Processor Type", INVALID_PROCESSOR);

		return null;
	}

	@Override
	public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

	@Override
	public boolean equals(Object o) {
		CodeRequest cr = (CodeRequest) o;

		return cr.getURL().equalsIgnoreCase(this.getURL()) && 
			cr.getProcessorType().equalsIgnoreCase(this.getProcessorType());
	}
	
	@Override
	public int hashCode() {
		String hash = url + ":" + processorType;
		return hash.hashCode();
	}

	public static final int MISSING_URL = 1;
	public static final int INVALID_URL = 2;

}
