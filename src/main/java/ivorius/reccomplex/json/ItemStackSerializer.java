/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.json;

import com.google.gson.*;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.structures.registry.MCRegistrySpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.FixTypes;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * Created by lukas on 25.05.14.
 */
public class ItemStackSerializer implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack>
{
    private MCRegistrySpecial registry;
    private DataFixer fixer;

    public ItemStackSerializer(MCRegistrySpecial registry)
    {
        this.registry = registry;
        this.fixer = DataFixesManager.createFixer();
    }

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", registry.itemHidingMode().containedItemID(src).toString());
        jsonObject.addProperty("damage", src.getItemDamage());
        jsonObject.addProperty("count", src.stackSize);

        if (src.hasTagCompound())
        {
            if (RecurrentComplex.USE_JSON_FOR_NBT)
                jsonObject.add("tag", context.serialize(src.getTagCompound()));
            else
                jsonObject.addProperty("tagBase64", NbtToJson.getBase64FromNBT(src.getTagCompound()));
        }

        return jsonObject;
    }

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject jsonObject = JsonUtils.getJsonElementAsJsonObject(json, "ItemStack");

        String id = JsonUtils.getJsonObjectStringFieldValue(jsonObject, "id");
        int damage = JsonUtils.getJsonObjectIntegerFieldValue(jsonObject, "damage");
        int count = JsonUtils.getJsonObjectIntegerFieldValue(jsonObject, "count");

        ItemStack stack = registry.itemHidingMode().constructItemStack(new ResourceLocation(id), count, damage);

        if (jsonObject.has("tag"))
        {
            NBTTagCompound compound = context.deserialize(jsonObject.get("tag"), NBTTagCompound.class);
            stack.setTagCompound(compound);
        }
        else if (jsonObject.has("tagBase64"))
        {
            NBTTagCompound compound = NbtToJson.getNBTFromBase64(JsonUtils.getJsonObjectStringFieldValue(jsonObject, "tagBase64"));
            stack.setTagCompound(compound);
        }

        stack = fixItemStack(stack, id);

        return stack;
    }

    @Nonnull
    protected ItemStack fixItemStack(ItemStack stack, String itemID)
    {
        NBTTagCompound postCompound = new NBTTagCompound();
        stack.writeToNBT(postCompound);
        postCompound.setString("id", itemID); // If the item was unproperly loaded before, here it gets to try again
        fixer.process(FixTypes.ITEM_INSTANCE, postCompound);
        stack = ItemStack.loadItemStackFromNBT(postCompound);
        return stack;
    }
}
