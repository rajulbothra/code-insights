package com.sap.codeinsights;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.eclipse.jgit.lib.PersonIdent;

public class BlameCoder extends PersonIdent {
	public int commitsContributed;
	public int linesContributed;
	public float projectEquity;

	public BlameCoder(PersonIdent i) {
		super(i);
	}

    public JsonObject toJson() {
        JsonParser parser = new JsonParser();
        return parser.parse(this.toString()).getAsJsonObject();
    }

	@Override
	public boolean equals(final Object o) {
		final PersonIdent c = (PersonIdent) o;
		return 
			super.getName().equalsIgnoreCase(c.getName());
	}

	@Override
	public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
