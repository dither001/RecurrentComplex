/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure;

import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.RCTextStyle;
import ivorius.reccomplex.commands.parameters.CommandExpecting;
import ivorius.reccomplex.commands.parameters.Expect;
import ivorius.reccomplex.commands.parameters.RCExpect;
import ivorius.reccomplex.commands.parameters.RCParameters;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandListStructures extends CommandExpecting
{
    public static final int RESULTS_PER_PAGE = 20;

    public static void showList(ICommandSender commandSender, int page, List<String> structureNames)
    {
        int startIndex = page * RESULTS_PER_PAGE;
        int endIndex = Math.min((page + 1) * RESULTS_PER_PAGE, structureNames.size());

        if (endIndex - startIndex > 0)
        {
            List<ITextComponent> components = new ArrayList<>(endIndex - startIndex + 2);

            components.add(new TextComponentString("[<--]"));
            if (page > 0)
                linkToPage(components.get(0), page - 1, ServerTranslations.format("commands.rclist.previous"));

            for (int i = 0; i < endIndex - startIndex; i++)
                components.add(RCTextStyle.structure(structureNames.get(startIndex + i)));

            components.add(new TextComponentString("[-->]"));
            if (page < (structureNames.size() - 1) / RESULTS_PER_PAGE)
                linkToPage(components.get(components.size() - 1), page + 1, ServerTranslations.format("commands.rclist.next"));

            commandSender.addChatMessage(ServerTranslations.join(components));
        }
        else
            commandSender.addChatMessage(ServerTranslations.get("commands.rclist.none"));
    }

    public static void linkToPage(ITextComponent component, int page, ITextComponent hoverTitle)
    {
        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                String.format("/%s %d", RCCommands.list.getCommandName(), page)));
        component.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverTitle));
        component.getStyle().setColor(TextFormatting.AQUA);
    }

    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "list";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC().any(0).descriptionU("page");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);
        int page = parameters.get().intAt(0).optional().orElse(0);

        List<String> structureNames = new ArrayList<>();
        structureNames.addAll(StructureRegistry.INSTANCE.ids());
        Collections.sort(structureNames, String.CASE_INSENSITIVE_ORDER);

        showList(commandSender, page, structureNames);
    }
}
