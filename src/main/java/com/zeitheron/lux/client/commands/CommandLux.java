package com.zeitheron.lux.client.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.luxpack.apis.LuxPackAPIv2;
import com.zeitheron.lux.proxy.ClientProxy;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.server.command.CommandTreeBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandLux extends CommandTreeBase
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
				ClientProxy.terrainProgram.onReload();
				ClientProxy.entityProgram.onReload();
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
					sender.sendMessage(new TextComponentString(sb.toString()));
					GuiScreen.setClipboardString(sb.toString());
					sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Copied to clipboard!"));
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
						tt.getStyle().setColor(TextFormatting.GOLD);
						sender.sendMessage(new TextComponentString("Type: ").appendSibling(tt));
						
						TextComponentString file = new TextComponentString("/logs/latest.log");
						file.getStyle().setColor(TextFormatting.GOLD);
						sender.sendMessage(new TextComponentString("This tile's properties have been dumped to console. See ").appendSibling(file));
						
						LuxPackAPIv2.TScriptJSInternal.dump(tile);
					}
				}
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
