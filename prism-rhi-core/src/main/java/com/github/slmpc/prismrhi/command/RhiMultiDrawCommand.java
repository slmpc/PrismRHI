package com.github.slmpc.prismrhi.command;

import java.util.ArrayList;
import java.util.List;

public record RhiMultiDrawCommand(List<RhiDrawCommand> commands) {
    public RhiMultiDrawCommand {
        commands = List.copyOf(commands == null ? List.of() : commands);
        validateCommonInstanceState(commands);
    }

    public RhiMultiDrawCommand(Iterable<RhiDrawCommand> commands) {
        this(copy(commands));
    }

    private static List<RhiDrawCommand> copy(Iterable<RhiDrawCommand> commands) {
        if (commands == null) {
            return List.of();
        }
        List<RhiDrawCommand> result = new ArrayList<>();
        for (RhiDrawCommand command : commands) {
            result.add(command);
        }
        return result;
    }

    public int drawCount() {
        return commands.size();
    }

    public int instanceCount() {
        return commands.isEmpty() ? 0 : commands.get(0).instanceCount();
    }

    public int firstInstance() {
        return commands.isEmpty() ? 0 : commands.get(0).firstInstance();
    }

    private static void validateCommonInstanceState(List<RhiDrawCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        int instanceCount = commands.get(0).instanceCount();
        int firstInstance = commands.get(0).firstInstance();
        for (RhiDrawCommand command : commands) {
            if (command.instanceCount() != instanceCount || command.firstInstance() != firstInstance) {
                throw new IllegalArgumentException("multiDraw requires common instanceCount and firstInstance");
            }
        }
    }
}
