package de.dawik.mcmp;

import de.tisan.mcoref.communication.CommunicationEvent;
import net.minecraft.nbt.NBTTagCompound;

public class MCMPNetworkHandler implements CommunicationEvent {

	@Override //TO CLIENT
	public void onServerToClientMessageReceived(NBTTagCompound tag) {

	}

	@Override //TO SERVER
	public void onClientToServerMessageReceived(NBTTagCompound tag) {

	}

}
