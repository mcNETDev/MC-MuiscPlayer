package de.tisan.mcoref.communication;

import java.util.ArrayList;

public class CommunicationManager {
	private static ArrayList<Communication> communications = new ArrayList<Communication>();

	public static Communication createCommunication(String modId) {
		Communication cc = CommunicationManager.getCommunication(modId);
		if (cc != null) {
			return cc;
		}
		Communication c = new Communication(modId);
		CommunicationManager.communications.add(c);
		return c;
	}

	public static Communication getCommunication(String modId) {
		for (Communication c : CommunicationManager.communications) {
			if (c.getName().equalsIgnoreCase(modId)) {
				return c;
			}
		}
		return null;
	}

}
