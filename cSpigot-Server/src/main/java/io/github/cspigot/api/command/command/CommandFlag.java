package io.github.cspigot.api.command.command;


public class CommandFlag {

    public static final char FLAG_PREFIX = '-';
    private final char character;
    private final String value;

    public CommandFlag(char character) {
        this(character, null);
    }

    public CommandFlag(char character, String value) {
        this.character = character;
        this.value = value;
    }

    public char getCharacter() {
        return character;
    }

    public String getValue() {
        return value;
    }

    public String flagPrefixToString() {
        return String.valueOf(new char[]{FLAG_PREFIX, character});
    }

}
