package me.ellbristow.ChestBank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryLargeChest;
import net.minecraft.server.ItemStack;
import net.minecraft.server.TileEntityChest;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class ChestPlayerListener extends PlayerListener {
	
	public static ChestBank plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	private final HashMap<String, InventoryLargeChest> chests;
	private final File dataFolder;
	
	public ChestPlayerListener (ChestBank instance, File dataFolder) {
		plugin = instance;
		this.dataFolder = dataFolder;
		this.chests = new HashMap<String, InventoryLargeChest>();
	}
	
	public void onPlayerInteract (PlayerInteractEvent event) {
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			Block block = event.getClickedBlock();
			if (block.getTypeId() == 54 && plugin.isBankBlock(block)) {
				Player player = event.getPlayer();
				EntityPlayer ePlayer;
				ePlayer = ((CraftPlayer) player).getHandle();
				InventoryLargeChest lc = getChest(player.getName());
				ePlayer.a(lc);
				event.setCancelled(true);
			}
		}
	}
	
	public InventoryLargeChest getChest(String playerName) {
		InventoryLargeChest chest = chests.get(playerName.toLowerCase());

		if (chest == null)
			chest = addChest(playerName);

		return chest;
	}

	private InventoryLargeChest addChest(String playerName) {
		InventoryLargeChest chest = new InventoryLargeChest("ChestBank", new TileEntityChest(), new TileEntityChest());
		chests.put(playerName.toLowerCase(), chest);
		return chest;
	}

	public void removeChest(String playerName) {
		chests.remove(playerName.toLowerCase());
	}

	public void load() {
		chests.clear();

		dataFolder.mkdirs();
		final FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".chest");
			}
		};
		for (File chestFile : dataFolder.listFiles(filter)) {
			try {
				final InventoryLargeChest chest = new InventoryLargeChest("ChestBank", new TileEntityChest(), new TileEntityChest());
				final String playerName = chestFile.getName().substring(0, chestFile.getName().length() - 6);

				final BufferedReader in = new BufferedReader(new FileReader(chestFile));

				String line;
				int field = 0;
				while ((line = in.readLine()) != null) {
					if (line != "") {
						final String[] parts = line.split(":");
						try {
							int type = Integer.parseInt(parts[0]);
							int amount = Integer.parseInt(parts[1]);
							short damage = Short.parseShort(parts[2]);
							if (type != 0) {
								chest.setItem(field, new ItemStack(type, amount, damage));
							}
						} catch (NumberFormatException e) {
							// ignore
						}
						++field;
					}
				}

				in.close();
				chests.put(playerName.toLowerCase(), chest);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void save() {

		dataFolder.mkdirs();

		for (String playerName : chests.keySet()) {
			final InventoryLargeChest chest = chests.get(playerName);

			try {
				final File chestFile = new File(dataFolder, playerName + ".chest");
				if (chestFile.exists())
					chestFile.delete();
				chestFile.createNewFile();

				final BufferedWriter out = new BufferedWriter(new FileWriter(chestFile));

				for (ItemStack stack : chest.getContents()) {
					if (stack != null)
						out.write(stack.id + ":" + stack.count + ":" + stack.getData() + "\r\n");
					else
						out.write("0:0:0\r\n");
				}

				out.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}