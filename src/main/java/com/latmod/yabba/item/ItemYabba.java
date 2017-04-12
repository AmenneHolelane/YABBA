package com.latmod.yabba.item;

import com.feed_the_beast.ftbl.lib.item.ItemBase;
import com.latmod.yabba.Yabba;
import com.latmod.yabba.YabbaCommon;

/**
 * Created by LatvianModder on 19.01.2017.
 */
public class ItemYabba extends ItemBase
{
    public ItemYabba(String id)
    {
        super(Yabba.MOD_ID + ':' + id);
        setCreativeTab(YabbaCommon.TAB);
    }
}