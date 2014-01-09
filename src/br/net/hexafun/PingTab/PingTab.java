package br.net.hexafun.PingTab;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.mcstats.MetricsLite;

public final class PingTab extends JavaPlugin implements Listener {

	public BukkitTask PingTask;
	public BukkitTask AlertTask;
	public String PingString;
	public Scoreboard PingScoreboard;
	public Scoreboard NormalScoreboard;
	private int goodPing;
	private int mediumPing;
	protected boolean disableTab;
	protected int alertThreshold;
	protected String alertMessage;
	private boolean showPluginName;
	private boolean coloredPing;
	private String ownPingMessage;
	private String pingMessage;
	private String goodPingColor;
	private String mediumPingColor;
	private String badPingColor;
	FileConfiguration config;
	private int pingTimer;
	private int alertTimer;
	private boolean alertPlayers;
	private int samplingAmount;
	private int tabMode;
	private int alertMode;

	PlayersPings playersPings;

	public PingTab() {
		super();
	}

	/*
	 * Message Related Functions
	 */

	/**
	 * @param ping
	 * @return String
	 */
	private String formatPingColor(int ping) {
		String ret;

		if (this.coloredPing) {
			if (ping <= this.goodPing) {
				ret = goodPingColor + "" + ping + ChatColor.RESET;
			} else if (ping <= this.mediumPing) {
				ret = mediumPingColor + "" + ping + ChatColor.RESET;
			} else {
				ret = badPingColor + "" + ping + ChatColor.RESET;
			}
		} else {
			ret = "" + ping;
		}

		return ret;
	}

	/**
	 * @param msg
	 * @return String
	 */
	private String insertPluginName(String msg) {
		if (this.showPluginName) {
			msg = ChatColor.DARK_GREEN + "[PingTab] " + ChatColor.RESET + msg;
		}
		return msg;
	}

	/**
	 * @param msg
	 * @param player
	 * @param playersPings
	 * @param pingMode
	 * @return String
	 */
	private String replacePlaceholders(String msg, Player player,
			PlayersPings playersPings, int pingMode) {
		msg = msg.replaceAll("%playername", player.getPlayerListName());

		int ping = playersPings.getPing((CraftPlayer) player, pingMode);

		msg = msg
				.replaceAll("%pingInstant", formatPingColor(playersPings
						.getLastPing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingAverage", formatPingColor(playersPings
				.getAveragePing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMedian", formatPingColor(playersPings
				.getMedianPing((CraftPlayer) player)));
		msg = msg
				.replaceAll("%pingMode", formatPingColor(playersPings
						.getModePing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMidrange", formatPingColor(playersPings
				.getMidrangerPing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMixedAverage", formatPingColor(playersPings
				.getMixedAveragePing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMixedMedian", formatPingColor(playersPings
				.getMixedMedianPing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMixedMode", formatPingColor(playersPings
				.getMixedModePing((CraftPlayer) player)));
		msg = msg.replaceAll("%pingMixedMidrange", formatPingColor(playersPings
				.getMixedMidrangePing((CraftPlayer) player)));

		msg = msg.replaceAll("%ping", formatPingColor(ping));

		msg = msg.replaceAll("%%", "%");

		return msg;
	}

	/**
	 * @param msg
	 * @param showPluginName
	 * @return String
	 */
	private String formatMessage(String msg, boolean showPluginName) {
		if (showPluginName) {
			msg = insertPluginName(msg);
		}
		msg = ChatColor.translateAlternateColorCodes('&', msg);
		return msg;
	}

	/**
	 * @param msg
	 * @param playersPings
	 * @param pingMode
	 * @param player
	 * @param showPluginName
	 * @return String
	 */
	private String formatMessage(String msg, PlayersPings playersPings,
			int pingMode, Player player, boolean showPluginName) {
		if (showPluginName) {
			msg = insertPluginName(msg);
		}
		msg = replacePlaceholders(msg, player, playersPings, pingMode);
		msg = ChatColor.translateAlternateColorCodes('&', msg);
		return msg;
	}

	/**
	 * @param msg
	 * @param playersPings
	 * @param pingMode
	 * @param player
	 * @param threshold
	 * @param showPluginName
	 * @return String
	 */
	protected String formatMessage(String msg, PlayersPings playersPings,
			int pingMode, Player player, int threshold, boolean showPluginName) {
		if (showPluginName) {
			msg = insertPluginName(msg);
		}
		msg = replacePlaceholders(msg, player, playersPings, pingMode);
		msg = ChatColor.translateAlternateColorCodes('&', msg);
		return msg;
	}

	/*
	 * Configuration related functions
	 */

	/**
	 * @return boolean
	 */
	private boolean loadStaticParamsConfig() {

		if (config.isInt("Interval")) {
			pingTimer = config.getInt("Interval");
		} else {
			pingTimer = 3;
		}

		if (config.isInt("AlertInterval")) {
			alertTimer = config.getInt("AlertInterval");
		} else {
			alertTimer = 5;
		}

		return true;
	}

	/**
	 * @return boolean
	 */
	private boolean loadParamsConfig() {
		if (config.isBoolean("DisableTab")) {
			disableTab = config.getBoolean("DisableTab");
		} else {
			disableTab = false;
		}

		if (config.isBoolean("ShowPluginNameOnMessages")) {
			showPluginName = config.getBoolean("ShowPluginNameOnMessages");
		} else {
			showPluginName = true;
		}

		if (config.isInt("SamplingAmount")) {
			samplingAmount = config.getInt("SamplingAmount");
		} else {
			samplingAmount = 20;
		}

		playersPings = new PlayersPings(this.samplingAmount, this.getLogger());

		String tabModeConfig;
		if (config.isString("TabMode")) {
			tabModeConfig = config.getString("TabMode");
			if (tabModeConfig.equalsIgnoreCase("instant")) {
				tabMode = 0;
			} else if (tabModeConfig.equalsIgnoreCase("average")) {
				tabMode = 1;
			} else if (tabModeConfig.equalsIgnoreCase("median")) {
				tabMode = 2;
			} else if (tabModeConfig.equalsIgnoreCase("mode")) {
				tabMode = 3;
			} else if (tabModeConfig.equalsIgnoreCase("midrange")) {
				tabMode = 4;
			} else if (tabModeConfig.equalsIgnoreCase("mixedaverage")) {
				tabMode = 5;
			} else if (tabModeConfig.equalsIgnoreCase("mixedmedian")) {
				tabMode = 6;
			} else if (tabModeConfig.equalsIgnoreCase("mixedmode")) {
				tabMode = 7;
			} else if (tabModeConfig.equalsIgnoreCase("mixedmidrange")) {
				tabMode = 8;
			} else {
				tabMode = 2;
			}
		} else {
			tabMode = 1;
		}

		if (config.isBoolean("ColoredPingParameter")) {
			coloredPing = config.getBoolean("ColoredPingParameter");
		} else {
			coloredPing = true;
		}

		if (config.isInt("GoodPing")) {
			goodPing = config.getInt("GoodPing");
		} else {
			goodPing = 200;
		}

		if (config.isString("GoodPingColor")) {
			goodPingColor = config.getString("GoodPingColor");
		} else {
			goodPingColor = "&2";
		}

		if (config.isInt("MediumPing")) {
			mediumPing = config.getInt("MediumPing");
		} else {
			mediumPing = 500;
		}

		if (config.isInt("MediumPingColor")) {
			mediumPingColor = config.getString("MediumPingColor");
		} else {
			mediumPingColor = "&6";
		}

		if (config.isInt("BadPingColor")) {
			badPingColor = config.getString("BadPingColor");
		} else {
			mediumPingColor = "&c";
		}

		if (config.isString("OwnPingMessage")) {
			ownPingMessage = config.getString("OwnPingMessage");
		} else {
			ownPingMessage = "Your ping is %ping ms";
		}

		if (config.isString("PingMessage")) {
			pingMessage = config.getString("PingMessage");
		} else {
			pingMessage = "%playername's ping is %ping ms";
		}

		if (config.isInt("AlertPlayers")) {
			alertPlayers = config.getBoolean("AlertPlayers");
		}

		String alertModeConfig;
		if (config.isString("AlertMode")) {
			alertModeConfig = config.getString("AlertMode");
			if (alertModeConfig == "Instant") {
				alertMode = 0;
			} else if (alertModeConfig == "Average") {
				alertMode = 1;
			} else if (alertModeConfig == "Median") {
				alertMode = 2;
			} else {
				alertMode = 2;
			}
		} else {
			alertMode = 2;
		}

		if (config.isInt("AlertThreshold")) {
			alertThreshold = config.getInt("AlertThreshold");
		} else {
			alertThreshold = 500;
		}

		if (config.isString("AlertMessage")) {
			alertMessage = config.getString("AlertMessage");
		} else {
			alertMessage = "%playername, your latency of %ping is above %threshold!";
		}
		return true;
	}

	/*
	 * Plugin Section
	 */

	public void onEnable() {
		try {
			MetricsLite metrics = new MetricsLite(this);
			;
			metrics.start();
			getLogger().info(
					(new StringBuilder("Plugin metrics enabled!")).toString());
		} catch (IOException ioexception) {
			getLogger().warning(
					(new StringBuilder("Plugin netrics failed!")).toString());
		}

		getServer().getPluginManager().registerEvents(this, this);
		File configFile = new File(getDataFolder(), "config.yml");

		if (!configFile.exists()) {
			saveDefaultConfig();
		}

		// Configuration File Parsing
		config = YamlConfiguration.loadConfiguration(configFile);

		if (!loadStaticParamsConfig()) {
			getLogger().warning(
					"Error loading timers config, using default settings");
		}

		if (!loadParamsConfig()) {
			getLogger().warning(
					"Error loading parameters config, using default settings");
		}

		// Create the scoreboard and assign an dummy objective to it
		PingScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		NormalScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		PingScoreboard.registerNewObjective("PingTab", "dummy");

		// Assign the new scoreboard to the player list "section"
		if (PingScoreboard.getObjective(DisplaySlot.PLAYER_LIST) == null) {
			PingScoreboard.getObjective("PingTab").setDisplaySlot(
					DisplaySlot.PLAYER_LIST);
			PingScoreboard.getObjective("PingTab").setDisplayName("ms");
		}

		// The plugin can be configured to alert the player when latency to high
		if (alertPlayers) {
			// Create a task so we can send the alerts
			AlertTask = Bukkit.getScheduler().runTaskTimer(this,
					new Runnable() {
						public void run() {
							// Nothing to do, no one online
							if (Bukkit.getOnlinePlayers().length == 0) {
								return;
							}

							// Measure player ping and send the messages
							Player players[];
							int j = (players = Bukkit.getOnlinePlayers()).length;
							for (int i = 0; i < j; i++) {
								CraftPlayer player = (CraftPlayer) players[i];
								int ping = -2;
								switch (alertMode) {
								case 0:
									ping = playersPings.getLastPing(player);
									break;
								case 1:
									ping = playersPings.getAveragePing(player);
									break;
								case 2:
									ping = playersPings.getMedianPing(player);
									break;
								case 3:
									ping = playersPings.getModePing(player);
									break;
								case 4:
									ping = playersPings
											.getMidrangerPing(player);
									break;
								case 5:
									ping = playersPings
											.getMixedAveragePing(player);
									break;
								case 6:
									ping = playersPings
											.getMixedMedianPing(player);
									break;
								case 7:
									ping = playersPings
											.getMixedModePing(player);
									break;
								case 8:
									ping = playersPings
											.getMixedMidrangePing(player);
									break;
								default:
									ping = playersPings.getMedianPing(player);
									break;
								}

								if (ping > alertThreshold) {
									player.sendMessage(formatMessage(
											alertMessage, playersPings,
											alertMode, (Player) player,
											alertThreshold, showPluginName));
								}
							}
						}
					}, 20 * alertTimer * 60, 20 * alertTimer * 60);
		}

		if (!disableTab) {

			// Create a task so we can update ping values
			PingTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

				public void run() {

					// Nothing to do, no one online
					if (Bukkit.getOnlinePlayers().length == 0) {
						return;
					}

					// Measure player ping and populate the scoreboard
					Player players[];
					int j = (players = Bukkit.getOnlinePlayers()).length;
					for (int i = 0; i < j; i++) {
						CraftPlayer player = (CraftPlayer) players[i];
						if (!playersPings.playerExists(player)) {
							playersPings.addPlayer(player.getName());
						}
						playersPings.pingPlayer(player);
						int ping = playersPings.getPing(player, tabMode);

						Objective PingListObjective = PingScoreboard
								.getObjective("PingTab");

						if (PingListObjective != null) {
							if (!player.getPlayerListName().equals(
									player.getName())) {
								PingScoreboard
										.getObjective("PingTab")
										.getScore(
												Bukkit.getOfflinePlayer(player
														.getPlayerListName()))
										.setScore(ping);
							} else {
								PingScoreboard.getObjective("PingTab")
										.getScore(player).setScore(ping);
							}
						} else {
							getLogger().warning(
									(new StringBuilder("Objective IS NULL"))
											.toString());
						}
					}

					// Assign the populated Scoreboard to allowed players
					for (int k = 0; k < j; k++) {
						Player player = players[k];
						if (player.hasPermission("pingtab.showscoreboard")) {
							player.setScoreboard(PingScoreboard);
						} else {
							if (player.getScoreboard() != null) {
								player.setScoreboard(null);
							}
						}
					}
				}
			}, 20 * pingTimer, 20 * pingTimer);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent playerJoin) {
		CraftPlayer player = (CraftPlayer) playerJoin.getPlayer();
		playersPings.addPlayer(player.getName());
		playersPings.pingPlayer(player);
		int ping = playersPings.getLastPing(player);
		PingScoreboard.getObjective("PingTab").getScore(player).setScore(ping);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent playerQuit) {
		CraftPlayer player = (CraftPlayer) playerQuit.getPlayer();
		if (playersPings.playerExists(player)) {
			playersPings.removePlayer(player.getName());
		}
	}

	public void onDisable() {
		Bukkit.getScheduler().cancelTask(PingTask.getTaskId());
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		boolean canPing = false;

		if (cmd.getName().equalsIgnoreCase("ping")) {
			if (args.length == 0) {
				// Ping yourself, but not from console
				if (sender instanceof Player) {
					sender.sendMessage(this.formatMessage(this.ownPingMessage,
							this.playersPings, this.tabMode,
							(CraftPlayer) sender, this.showPluginName));
					return true;
				} else {
					sender.sendMessage(formatMessage(
							"Sorry, but you cannot ping yourself from console!",
							this.showPluginName));
					return true;
				}
			} else if (args[0].equalsIgnoreCase("list")) {
				Player players[];
				int j = (players = Bukkit.getOnlinePlayers()).length;

				int onlinePlayers = 0;
				if (sender instanceof Player) {
					for (int i = 0; i < j; i++) {
						if (((Player) sender).canSee(players[i])) {
							onlinePlayers++;
						}
					}
				} else {
					onlinePlayers = j;
				}

				if (onlinePlayers == 0) {
					sender.sendMessage(this.formatMessage("No players online!",
							false));
					return true;
				}

				String msg = "%playername ";
				boolean notParam = false;
				if (args.length == 1) {
					msg = msg + "- %ping";
				} else {
					int k = args.length;
					for (int l = 1; l < k; l++) {
						switch (args[l]) {
						case "ping":
						case "pingInstant":
						case "pingAverage":
						case "pingMedian":
						case "pingMode":
						case "pingMidrange":
						case "pingMixedAverage":
						case "pingMixedMedian":
						case "pingMixedMode":
						case "pingMixedMidrange":
							msg = msg + "- %" + args[l];
							break;
						default:
							notParam = true;
							break;
						}
					}
				}

				if (notParam) {
					sender.sendMessage(formatMessage(
							"One or more invalid arguments, valid ones are: ping,"
									+ " pingInstant, pingAverage, "
									+ "pingMedian, pingMode, "
									+ "pingMidrange, pingMixedAverage,"
									+ " pingMixedMedian, pingMixedMode,"
									+ " pingMixedMidrange. All of them are case sensitive.",
							false));
					return true;
				}

				sender.sendMessage(formatMessage(
						"&3=========================== &2Ping Tab &3=============================&r",
						false));

				String headerMsg = "Playername ";
				if (args.length == 1) {
					headerMsg = headerMsg + "- ping";
				} else {
					int k = args.length;
					for (int l = 1; l < k; l++) {
						switch (args[l]) {
						case "ping":
						case "pingInstant":
						case "pingAverage":
						case "pingMedian":
						case "pingMode":
						case "pingMidrange":
						case "pingMixedAverage":
						case "pingMixedMedian":
						case "pingMixedMode":
						case "pingMixedMidrange":
							headerMsg = headerMsg + "- " + args[l];
							break;
						}
					}
				}

				// sender.sendMessage(formatMessage(headerMsg, false));

				if (sender instanceof Player) {
					// From game
					for (int i = 0; i < j; i++) {
						if ((((Player) sender).canSee(players[i]))
								&& (players[i] != null)) {
							sender.sendMessage(this.formatMessage(msg,
									this.playersPings, this.tabMode,
									(CraftPlayer) players[i], false));
						}
					}
				} else {
					// From console
					for (int i = 0; i < j; i++) {
						sender.sendMessage(this.formatMessage(msg,
								this.playersPings, this.tabMode,
								(CraftPlayer) players[i], false));
					}
				}
			} else {
				// Ping anyone else
				Player player = (Player) Bukkit.getPlayer(args[0]);

				if (sender instanceof Player) {
					// From game
					if ((player != null) && (((Player) sender).canSee(player))) {
						canPing = true;
					}
				} else {
					// From console
					if (player != null) {
						canPing = true;
					}
				}

				if (canPing) {
					sender.sendMessage(this.formatMessage(pingMessage,
							playersPings, tabMode, player, showPluginName));
				} else {
					sender.sendMessage(formatMessage("The player " + args[0]
							+ " was not found!", this.showPluginName));
				}
			}
			return true;
		} else if (cmd.getName().equalsIgnoreCase("pingtab")) {
			if ((args.length != 0) && (args[0].equalsIgnoreCase("reload"))) {

				sender.sendMessage(formatMessage(
						"Reloading PingTab configuration file.",
						this.showPluginName));

				if (loadParamsConfig()) {
					sender.sendMessage(formatMessage(
							"PingTab configuration file reloaded.",
							this.showPluginName));
					getLogger().info("Configuration reloaded.");
				} else {
					sender.sendMessage(formatMessage(
							"PingTab configuration file reloaded error.",
							this.showPluginName));
					getLogger()
							.info("PingTab configuration reload error, using defaults.");
				}
			} else {
				sender.sendMessage(formatMessage("Unknown option " + args[0]
						+ ".", this.showPluginName));
			}
			return true;
		}
		return false;
	}
}
