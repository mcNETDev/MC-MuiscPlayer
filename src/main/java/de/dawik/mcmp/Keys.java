package de.dawik.mcmp;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;

public class Keys {
	public static KeyBinding keyOpenGUI;
	
	
	public static void init(){
		keyOpenGUI = new KeyBinding("key.openMP3Player", Keyboard.KEY_M, "key.categories.mcmp");
		ClientRegistry.registerKeyBinding(keyOpenGUI);
	}
	
}
