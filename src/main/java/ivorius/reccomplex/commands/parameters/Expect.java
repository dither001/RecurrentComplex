/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.parameters;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord;

/**
 * Created by lukas on 30.05.17.
 */
public class Expect<T extends Expect<T>>
{
    protected final Map<String, Param> params = new HashMap<>();
    protected final Set<String> flags = new HashSet<>();
    protected String cur;

    Expect()
    {
        getOrCreate(null);
    }

    public static <T extends Expect<T>> T start()
    {
        //noinspection unchecked
        return (T) new Expect();
    }

    public T named(@Nonnull String name)
    {
        getOrCreate(cur = name);
        return (T) this;
    }

    public T flag(@Nonnull String name)
    {
        flags.add(name);
        return named(name);
    }

    public T skip(int num)
    {
        return next(Collections.emptyList());
    }

    public T next(Completer completion)
    {
        Param cur = getOrCreate(this.cur);
        cur.next(completion);
        return (T) this;
    }

    @Nonnull
    protected Param getOrCreate(String id)
    {
        Param param = params.get(id);
        if (param == null)
            params.put(this.cur, param = new Param());
        return param;
    }

    public T any(Object... completion)
    {
        return next(Arrays.asList(completion));
    }

    public T next(Collection<?> completion)
    {
        return next((server, sender, args, pos) -> getListOfStringsMatchingLastWord(args, completion));
    }

    public T next(Function<String[], ? extends Collection<String>> completion)
    {
        return next((server, sender, args, pos) -> completion.apply(args));
    }

    public T randomString()
    {
        return any(UUID.randomUUID().toString());
    }

    public T repeat()
    {
        Param cur = params.get(this.cur);
        if (cur == null) throw new IllegalStateException();
        cur.repeat = true;
        return (T) this;
    }

    public int index()
    {
        return params.size();
    }

    public List<String> get(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        List<String> quoted = Arrays.stream(Parameters.quoted(args)).map(Parameters::trimQuotes).collect(Collectors.toList());
        String[] paramArray = quoted.toArray(new String[quoted.size()]);

        Parameters parameters = Parameters.of(args, flags.stream().toArray(String[]::new));

        String lastID = parameters.order.get(parameters.order.size() - 1);
        Parameter entered = lastID != null ? parameters.get(lastID) : parameters.get();
        Param param = this.params.get(lastID);

        if (param != null && (entered.count() <= param.completion.size() || param.repeat)
                // It notices we are entering a parameter so it won't be added to the parameters args anyway
                && !quoted.get(quoted.size() - 1).startsWith(Parameters.flagPrefix))
        {
            return param.completion.get(Math.min(entered.count() - 1, param.completion.size() - 1)).complete(server, sender, paramArray, pos).stream()
                    // More than one word, let's wrap this in quotes
                    .map(s -> s.contains(" ") && !s.startsWith("\"") ? String.format("\"%s\"", s) : s)
                    .collect(Collectors.toList());
        }

        return remaining(paramArray, parameters.flags);
    }

    @Nonnull
    public List<String> remaining(String[] paramArray, Set<String> flags)
    {
        return getListOfStringsMatchingLastWord(paramArray, this.params.keySet().stream()
                .filter(p -> p != null && !flags.contains(p))
                .map(p -> Parameters.flagPrefix + p).collect(Collectors.toList()));
    }

    public interface Completer
    {
        public Collection<String> complete(MinecraftServer server, ICommandSender sender, String[] argss, @Nullable BlockPos pos);
    }

    protected class Param
    {
        protected final List<Completer> completion = new ArrayList<>();
        protected boolean repeat;

        public Param next(Completer completion)
        {
            this.completion.add(completion);
            return this;
        }
    }
}