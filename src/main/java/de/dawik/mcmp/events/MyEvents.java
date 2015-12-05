package de.dawik.mcmp.events;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import de.dawik.mcmp.Keys;
import de.dawik.mcmp.MCMPMod;
import de.dawik.mcmp.gui.GUIBack;
import net.minecraftforge.event.ServerChatEvent;

public class MyEvents {
	@SubscribeEvent
	public void onJoin(ServerChatEvent e) {
		System.out.println("CHAT : " + e.message);
	}
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event){
		if(Keys.keyOpenGUI.getIsKeyPressed()){
			System.out.println("key");
			FMLClientHandler.instance().getClientPlayerEntity().openGui(MCMPMod.instance, GUIBack.GUI_ID, null, 0, 0, 0);
		}
	}
}
