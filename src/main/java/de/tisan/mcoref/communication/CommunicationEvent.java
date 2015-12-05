package de.tisan.mcoref.communication;

import net.minecraft.nbt.NBTTagCompound;

public interface CommunicationEvent {
	public void onServerToClientMessageReceived(NBTTagCompound tag);
	public void onClientToServerMessageReceived(NBTTagCompound tag);
	
}
