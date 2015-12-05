package de.dawik.mcmp.gui;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.AbstractDocument.Content;

import de.dawik.mcmp.MCMPMod;
import de.tisan.flatui.components.fbutton.FlatButton;
import de.tisan.flatui.components.fcommons.Anchor;
import de.tisan.flatui.components.fcommons.FlatColors;
import de.tisan.flatui.components.fcommons.FlatLayoutManager;
import de.tisan.flatui.components.flisteners.ActionListener;
import de.tisan.flatui.components.flisteners.MouseClickedHandler;
import de.tisan.flatui.components.flisteners.Priority;
import javafx.animation.KeyFrame;
import net.minecraft.client.Minecraft;

public class GUIFront extends JFrame {
	public static void main(String[] args) {
		new GUIFront().setVisible(true);
	}

	public GUIFront() {
		// MCMPMod.instance.gui = this;
		FlatLayoutManager man = FlatLayoutManager.get(this);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		man.setResizable(false);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = 300;
		int h = 400;
		setBounds((int) (dim.getWidth() / 2) - (w / 2), (int) (dim.getHeight() / 2) - (h / 2), w, h);
		JPanel content = new JPanel();
		content.setLayout(null);
		content.setBorder(null);
		setContentPane(content);
		content.setBackground(FlatColors.BACKGROUND);
		FlatButton btnX = new FlatButton("X", man);
		btnX.setBounds(w - 25, 5, 20, 20);
		btnX.addActionListener(Priority.NORMAL, new ActionListener() {

			@Override
			public void onAction(MouseClickedHandler arg0) {
				dispose();
				Minecraft.getMinecraft().displayGuiScreen(null);
				if (Minecraft.getMinecraft().currentScreen == null) {
					Minecraft.getMinecraft().setIngameFocus();
				}
			}
		});
	}

}
