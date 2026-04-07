package ru.copperside.sal.api.command;

/**
 * Command execution priority.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.CommandPriority}
 */
public enum CommandPriority {

    Idle(0),
    BelowNormal(4),
    Normal(5),
    AboveNormal(6),
    High(9),
    RealTime(10);

    private final int value;

    CommandPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name();
    }
}
