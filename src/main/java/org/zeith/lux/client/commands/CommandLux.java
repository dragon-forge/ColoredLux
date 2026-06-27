package org.zeith.lux.client.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.command.*;
import net.minecraft.init.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.*;
import net.minecraft.world.World;
import net.minecraftforge.common.property.*;
import net.minecraftforge.server.command.CommandTreeBase;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.luxpack.apis.LuxPackAPIv2;
import org.zeith.lux.proxy.ClientProxy;

import java.util.*;
import java.util.stream.Collectors;

public class CommandLux
		extends CommandTreeBase
{
	{
		addSubcommand(new CommandBase()
		{
			@Override
			public String getUsage(ICommandSender sender)
			{
				return "lux reload";
			}

			@Override
			public String getName()
			{
				return "reload";
			}

			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
			{
				sender.sendMessage(new TextComponentString("Resetting bound lights"));
				ClientProxy.EXISTING.clear();
				ClientProxy.EXISTING_ENTS.clear();
				sender.sendMessage(new TextComponentString("Reloading shaders"));
				IResourceManager res = Minecraft.getMinecraft().getResourceManager();
				ClientProxy.terrainProgram.prepareReload(res);
				ClientProxy.entityProgram.prepareReload(res);
				
				ClientProxy.terrainProgram.onReload(res);
				ClientProxy.entityProgram.onReload(res);
				sender.sendMessage(new TextComponentString("Reloading lux manager"));
				LuxManager.reload();
				sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Lux reloaded!"));
			}
		});

		addSubcommand(new CommandBase()
		{
			@Override
			public String getUsage(ICommandSender sender)
			{
				return "lux pickstate";
			}

			@Override
			public String getName()
			{
				return "pickstate";
			}

			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
			{
				RayTraceResult obj = Minecraft.getMinecraft().objectMouseOver;
				if(obj != null && obj.typeOfHit == RayTraceResult.Type.BLOCK)
				{
					BlockPos pos = obj.getBlockPos();
					World wld = Minecraft.getMinecraft().world;
					IBlockState state = wld.getBlockState(pos).getActualState(wld, pos);
					final StringBuilder sb = new StringBuilder().append("\"state\": { ").append(Joiner.on(", ").join(state.getProperties().entrySet().stream().map(entry -> JSONObject.quote(entry.getKey().getName()) + ": " + JSONObject.quote(Objects.toString(entry.getValue()))).collect(Collectors.toList())));
					if(state instanceof IExtendedBlockState)
					{
						sb.append(", ");
						final ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties = ((IExtendedBlockState) state).getUnlistedProperties();
						List<String> bleh = new ArrayList<>();
						unlistedProperties.forEach((key, value) -> value.ifPresent(trueValue -> bleh.add(JSONObject.quote(key.getName()) + ": " + JSONObject.quote(Objects.toString(trueValue)))));
						sb.append(Joiner.on(", ").join(bleh));
					}
					sb.append(" }");

					TextComponentString txt = new TextComponentString(sb.toString());
					txt.getStyle()
							.setItalic(true)
							.setColor(TextFormatting.AQUA)
							.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to copy!")))
							.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lux copytoclipboard " + sb));


					sender.sendMessage(txt);
				}
			}
		});

		addSubcommand(new CommandBase()
		{
			@Override
			public String getName()
			{
				return "picktile";
			}

			@Override
			public String getUsage(ICommandSender sender)
			{
				return "picktile";
			}

			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
			{
				RayTraceResult obj = Minecraft.getMinecraft().objectMouseOver;
				if(obj != null && obj.typeOfHit == RayTraceResult.Type.BLOCK)
				{
					BlockPos pos = obj.getBlockPos();
					World wld = Minecraft.getMinecraft().world;
					TileEntity tile = wld.getTileEntity(pos);
					if(tile == null)
						throw new CommandException("No tile entity found on " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
					else
					{
						TextComponentString tt = new TextComponentString(tile.getClass().getCanonicalName());
						tt.getStyle()
								.setColor(TextFormatting.GOLD)
								.setUnderlined(true)
								.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to copy!")))
								.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lux copytoclipboard " + tile.getClass().getCanonicalName()));

						sender.sendMessage(new TextComponentString("Type: ").appendSibling(tt));

						String code = LuxPackAPIv2.TScriptJSInternal.dump(tile, 0);

						TextComponentString ucode = new TextComponentString(code);
						ucode.getStyle()
								.setColor(TextFormatting.AQUA)
								.setItalic(true)
								.setUnderlined(true)
								.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to copy!")))
								.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lux copytoclipboard " + code));

						TextComponentString file = new TextComponentString("/logs/latest.log");
						file.getStyle()
								.setColor(TextFormatting.GOLD)
								.setUnderlined(true)
								.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to open file!")))
								.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "logs/latest.log"));

						sender.sendMessage(new TextComponentString("This tile's properties have been dumped to console. Search for ").appendSibling(ucode).appendText(" in ").appendSibling(file));
					}
				}
			}
		});

		addSubcommand(new CommandBase()
		{
			@Override
			public String getName()
			{
				return "copytoclipboard";
			}

			@Override
			public String getUsage(ICommandSender sender)
			{
				return "copytoclipboard";
			}

			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
			{
				GuiScreen.setClipboardString(String.join(" ", args));
				Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
			}
		});
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "/lux";
	}

	@Override
	public String getName()
	{
		return "lux";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 0;
	}
}
