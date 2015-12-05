package de.dawik.mcmp;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import de.dawik.mcmp.events.MyEvents;
import de.dawik.mcmp.gui.GUIFront;
import de.dawik.mcmp.handler.ConfigurationHandler;
import de.dawik.mcmp.init.Register;
import de.dawik.mcmp.reference.Reference;
import de.dawik.mcmp.utility.Log;
import de.tisan.mcoref.communication.Communication;
import de.tisan.mcoref.communication.CommunicationManager;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = Reference.MOD_ID, version = Reference.VERSION, name = Reference.MOD_NAME, dependencies = "")
public class MCMPMod {
	@Mod.Instance(Reference.MOD_ID)
	public static MCMPMod instance;

	public static Communication con;

	public GUIFront gui;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ConfigurationHandler.init(event.getSuggestedConfigurationFile());
		FMLCommonHandler.instance().bus().register(new ConfigurationHandler());
		Register.preInit();
		Log.info("PREInitialization Complete!");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		MyEvents e = new MyEvents();
		FMLCommonHandler.instance().bus().register(e);
		MinecraftForge.EVENT_BUS.register(e);
		// NETWORK
		// con = CommunicationManager.createCommunication(Reference.MOD_ID);
		// con.addEvent(new DaWikNetworkHandler());
		// GUI
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, new MCMPGuiHandler());
		Register.init();
		Log.info("Initialization Complete!");
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Register.postInit();
		Log.info("POSTInitialization Complete!");
	}

	@Mod.EventHandler
	public void onAboutToStart(FMLServerAboutToStartEvent e) {
		System.out.println("About to start!");
	}
}
