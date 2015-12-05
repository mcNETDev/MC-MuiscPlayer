package de.dawik.mcmp.gui;

import de.dawik.mcmp.MCMPMod;
import net.minecraft.client.gui.GuiScreen;

public class GUIBack extends GuiScreen {
	public final static int GUI_ID = 1;

	public GUIBack() {
		new GUIFront().setVisible(true);
	}

	@Override
	public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
		drawDefaultBackground();
	}

	@Override
	protected void keyTyped(char p_73869_1_, int p_73869_2_) {
		super.keyTyped(p_73869_1_, p_73869_2_);
	}

	@Override
	protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
		if(MCMPMod.instance.gui != null){
			MCMPMod.instance.gui.toFront();
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
