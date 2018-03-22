package com.latmod.yabba.tile;

import com.feed_the_beast.ftblib.lib.item.ItemEntry;
import com.feed_the_beast.ftblib.lib.item.ItemEntryWithCount;
import com.feed_the_beast.ftblib.lib.tile.EnumSaveType;
import com.feed_the_beast.ftblib.lib.tile.TileBase;
import com.latmod.yabba.YabbaConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class TileAntibarrel extends TileBase implements IItemHandlerModifiable
{
	public final Map<ItemEntry, ItemEntryWithCount> items = new LinkedHashMap<>();
	private ItemEntryWithCount[] itemsArray = null;
	public int totalChanges = 0;

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side)
	{
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side)
	{
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			return (T) this;
		}

		return super.getCapability(capability, side);
	}

	@Override
	public void writeData(NBTTagCompound nbt, EnumSaveType type)
	{
		if (type != EnumSaveType.SAVE)
		{
			return;
		}

		if (!items.isEmpty())
		{
			NBTTagList list = new NBTTagList();

			for (ItemEntryWithCount entry : items.values())
			{
				if (!entry.isEmpty())
				{
					list.appendTag(entry.serializeNBT());
				}
			}

			nbt.setTag("Inv", list);
		}
	}

	@Override
	public void readData(NBTTagCompound nbt, EnumSaveType type)
	{
		if (type != EnumSaveType.SAVE)
		{
			return;
		}

		items.clear();
		itemsArray = null;

		NBTTagList list = nbt.getTagList("Inv", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemEntryWithCount entryc = new ItemEntryWithCount(list.getCompoundTagAt(i));

			if (!entryc.isEmpty())
			{
				items.put(entryc.entry, entryc);
			}
		}
	}

	@Override
	public int getSlots()
	{
		return items.size() + 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return slot <= 0 || slot > items.size() ? ItemStack.EMPTY : getItemArray()[slot - 1].getStack(false);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack)
	{
		if (YabbaConfig.general.crash_on_set_methods)
		{
			throw new RuntimeException("Do not use setStackInSlot method! This is not a YABBA bug.");
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (stack.isEmpty())
		{
			return stack;
		}
		else if (slot >= 0 && slot <= items.size() && items.size() < YabbaConfig.general.antibarrel_capacity && !stack.isStackable())
		{
			if (!simulate)
			{
				ItemEntry entry = ItemEntry.get(stack);

				if (slot == 0)
				{
					ItemEntryWithCount entryc = items.get(entry);

					if (entryc == null)
					{
						entryc = new ItemEntryWithCount(entry, 0);
						items.put(entry, entryc);
						itemsArray = null;
					}

					entryc.count += stack.getCount();
				}
				else
				{
					ItemEntryWithCount entryc = getItemArray()[slot];

					if (entryc.entry.equalsEntry(entry))
					{
						entryc.count += stack.getCount();
					}
				}

				if (!world.isRemote)
				{
					totalChanges++;
					markDirty();
				}
			}

			return ItemStack.EMPTY;
		}

		return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		if (slot <= 0 || slot > items.size() || amount < 1)
		{
			return ItemStack.EMPTY;
		}

		ItemEntryWithCount entryc = getItemArray()[slot - 1];

		if (entryc.isEmpty())
		{
			return ItemStack.EMPTY;
		}

		int extracted = Math.min(amount, entryc.count);

		ItemStack is = entryc.entry.getStack(extracted, true);

		if (!simulate)
		{
			entryc.count -= extracted;

			if (!world.isRemote)
			{
				totalChanges++;
				markDirty();
			}
		}

		return is;
	}

	public ItemEntryWithCount[] getItemArray()
	{
		if (itemsArray == null)
		{
			itemsArray = items.values().toArray(new ItemEntryWithCount[0]);
		}

		return itemsArray;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean notifyBlock()
	{
		return false;
	}

	@Override
	public void markDirty()
	{
		sendDirtyUpdate();
	}

	@Override
	public boolean shouldDrop()
	{
		return !items.isEmpty();
	}
}