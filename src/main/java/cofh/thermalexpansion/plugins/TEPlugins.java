package cofh.thermalexpansion.plugins;

import cofh.core.CoFHProps;
import cofh.thermalexpansion.ThermalExpansion;

import java.util.ArrayList;

public class TEPlugins {

	static class Plugin {

		public Class<?> pluginClass = null;
		public String pluginPath;

		public Plugin(String pluginPath) {

			this.pluginPath = "cofh.thermalexpansion.plugins." + pluginPath;
		}

		public void preInit() {

			try {
				pluginClass = TEPlugins.class.getClassLoader().loadClass(pluginPath);
				pluginClass.getMethod("preInit", new Class[0]).invoke(null);
			} catch (Throwable t) {
				if (CoFHProps.enableDebugOutput) {
					t.printStackTrace();
				}
			}
		}

		public void initialize() {

			try {
				if (pluginClass != null) {
					pluginClass.getMethod("initialize", new Class[0]).invoke(null);
				}
			} catch (Throwable t) {
				if (CoFHProps.enableDebugOutput) {
					t.printStackTrace();
				}
			}
		}

		public void postInit() {

			try {
				if (pluginClass != null) {
					pluginClass.getMethod("postInit", new Class[0]).invoke(null);
				}
			} catch (Throwable t) {
				if (CoFHProps.enableDebugOutput) {
					t.printStackTrace();
				}
			}
		}

		public void loadComplete() {

			try {
				if (pluginClass != null) {
					pluginClass.getMethod("loadComplete", new Class[0]).invoke(null);
				}
			} catch (Throwable t) {
				if (CoFHProps.enableDebugOutput) {
					t.printStackTrace();
				}
			}
		}

		public void registerRenderInformation() {

			try {
				if (pluginClass != null) {
					pluginClass.getMethod("registerRenderInformation", new Class[0]).invoke(null);
				}
			} catch (Throwable t) {
				if (CoFHProps.enableDebugOutput) {
					t.printStackTrace();
				}
			}
		}
	}

	public static ArrayList<Plugin> pluginList = new ArrayList<Plugin>();

	static {
		// addPlugin("cc.CCPlugin", "ComputerCraft");
		addPlugin("mfr.MFRPlugin", "MineFactoryReloaded");
		addPlugin("ee3.EE3Plugin", "EE3");
		addPlugin("eio.EIOPlugin", "EnderIO");
		addPlugin("forestry.ForestryPlugin", "Forestry");
		addPlugin("thaumcraft.ThaumcraftPlugin", "Thaumcraft");
	}

	public static void preInit() {

		ThermalExpansion.log.info("ThermalExpansion: Loading Plugins...");
		for (int i = 0; i < pluginList.size(); i++) {
			pluginList.get(i).preInit();
		}
		ThermalExpansion.log.info("Thermal Expansion: Finished Loading Plugins.");
	}

	public static void initialize() {

		for (int i = 0; i < pluginList.size(); i++) {
			pluginList.get(i).initialize();
		}
	}

	public static void postInit() {

		for (int i = 0; i < pluginList.size(); i++) {
			pluginList.get(i).postInit();
		}
	}

	public static void loadComplete() {

		for (int i = 0; i < pluginList.size(); i++) {
			pluginList.get(i).loadComplete();
		}
	}

	public static void cleanUp() {

		pluginList.clear();
	}

	public static boolean addPlugin(String pluginPath, String pluginName) {

		boolean enable = ThermalExpansion.config.get("Plugins." + pluginName, "Enable", true);
		ThermalExpansion.config.save();

		if (enable) {
			pluginList.add(new Plugin(pluginPath));
			return true;
		}
		return false;
	}

}
