package com.zeitheron.lux.api.light;

import com.zeitheron.lux.api.event.GatherLightsEvent;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

public interface ILightItem
{
	void addLightsAsEntity(EntityItem entity, GatherLightsEvent event);
	
	void addLightsAsHeld(EntityLivingBase holder, ItemStack stack, EntityEquipmentSlot slot, GatherLightsEvent event);
}