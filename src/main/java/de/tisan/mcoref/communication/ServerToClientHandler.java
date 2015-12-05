package de.tisan.mcoref.communication;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ServerToClientHandler implements IMessageHandler<ServerToClientMessage, IMessage> {

	private Communication comm;

	public ServerToClientHandler() {

	}

	protected void setCommInstance(Communication comm) {
		this.comm = comm;
	}

	@Override
	public IMessage onMessage(ServerToClientMessage message, MessageContext ctx) {
		if (ctx.side.equals(Side.CLIENT)) {
			comm.pushClientEvent(message.getTag());
		}
		return null;
	}

}
