package de.dawik.mcmp;

import cpw.mods.fml.common.network.IGuiHandler;
import de.dawik.mcmp.gui.GUIBack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class MCMPGuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if(ID == GUIBack.GUI_ID){
			return new GUIBack();
		}
		return null;
	}

}
