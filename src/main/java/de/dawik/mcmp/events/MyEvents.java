package de.dawik.mcmp.events;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.ServerChatEvent;

public class MyEvents {
	@SubscribeEvent
	public void onJoin(ServerChatEvent e) {
		System.out.println("CHAT : " + e.message);
	}
	
}
