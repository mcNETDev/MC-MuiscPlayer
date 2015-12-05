package de.tisan.mcoref.communication;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ClientToServerHandler implements IMessageHandler<ClientToServerMessage, IMessage> {

	private Communication comm;

	public ClientToServerHandler() {

	}

	protected void setCommInstance(Communication comm) {
		this.comm = comm;
	}

	@Override
	public IMessage onMessage(ClientToServerMessage message, MessageContext ctx) {
		System.out.println("Message on the server received!");
		if (ctx.side.equals(Side.SERVER)) {
			System.out.println("I am the server.");
			comm.pushServerEvent(message.getTag());
		}
		return null;
	}
}
