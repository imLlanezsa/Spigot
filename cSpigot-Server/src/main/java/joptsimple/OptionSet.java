package joptsimple;

import io.papermc.paper.util.maplist.ObjectMapList;
import joptsimple.internal.Objects;

import java.util.*;

public class OptionSet {

    private final Map<String, AbstractOptionSpec<?>> detectedOptions = new HashMap<>();
    private final Map<AbstractOptionSpec<?>, List<String>> optionsToArguments = new IdentityHashMap<>();
    private final List<String> nonOptionArguments = new ArrayList<>();
    private final Map<String, List<?>> defaultValues;

    OptionSet(Map<String, List<?>> defaults) {
        this.defaultValues = new HashMap<>(defaults);
    }

    public boolean has(String option) {
        return this.detectedOptions.containsKey(option);
    }

    public boolean has(OptionSpec<?> option) {
        return this.optionsToArguments.containsKey(option);
    }

    public boolean hasArgument(String option) {
        AbstractOptionSpec<?> spec = this.detectedOptions.get(option);
        return spec != null && this.hasArgument(spec);
    }

    public boolean hasArgument(OptionSpec<?> option) {
        Objects.ensureNotNull(option);
        List<String> values = this.optionsToArguments.get(option);
        return values != null && !values.isEmpty();
    }

    public Object valueOf(String option) {
        Objects.ensureNotNull(option);
        AbstractOptionSpec<?> spec = this.detectedOptions.get(option);
        if (spec == null) {
            List<?> defaults = this.defaultValuesFor(option);
            return defaults.isEmpty() ? null : defaults.get(0);
        } else {
            return this.valueOf(spec);
        }
    }

    public <V> V valueOf(OptionSpec<V> option) {
        Objects.ensureNotNull(option);
        List<V> values = this.valuesOf(option);
        switch (values.size()) {
            case 0:
                return null;
            case 1:
                return values.get(0);
            default:
                throw new MultipleArgumentsForOptionException(option.options());
        }
    }

    public List<?> valuesOf(String option) {
        Objects.ensureNotNull(option);
        AbstractOptionSpec<?> spec = this.detectedOptions.get(option);
        return spec == null ? this.defaultValuesFor(option) : this.valuesOf(spec);
    }

    public <V> List<V> valuesOf(OptionSpec<V> option) {
        Objects.ensureNotNull(option);
        List<String> values = this.optionsToArguments.get(option);
        if (values != null && !values.isEmpty()) {
            AbstractOptionSpec<V> spec = (AbstractOptionSpec) option;
            List<V> convertedValues = new ArrayList<>();
            for ( String each : values ) {
                convertedValues.add(spec.convert(each));
            }

            return Collections.unmodifiableList(convertedValues);
        } else {
            return this.defaultValueFor(option);
        }
    }

    public List<String> nonOptionArguments() {
        return Collections.unmodifiableList(this.nonOptionArguments);
    }

    void add(AbstractOptionSpec<?> option) {
        this.addWithArgument(option, null);
    }

    void addWithArgument(AbstractOptionSpec<?> option, String argument) {
        for ( String each : option.options() ) {
            this.detectedOptions.put(each, option);
        }

        List<String> optionArguments = this.optionsToArguments.computeIfAbsent(option, k -> new ArrayList<>());
        if (argument != null) {
            optionArguments.add(argument);
        }

    }

    void addNonOptionArgument(String argument) {
        this.nonOptionArguments.add(argument);
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that != null && this.getClass().equals(that.getClass())) {
            OptionSet other = (OptionSet) that;
            Map<AbstractOptionSpec<?>, List<String>> thisOptionsToArguments = new HashMap<>(this.optionsToArguments);
            Map<AbstractOptionSpec<?>, List<String>> otherOptionsToArguments = new HashMap<>(other.optionsToArguments);
            return this.detectedOptions.equals(other.detectedOptions) && thisOptionsToArguments.equals(otherOptionsToArguments) && this.nonOptionArguments.equals(other.nonOptionArguments());
        } else {
            return false;
        }
    }

    public int hashCode() {
        Map<AbstractOptionSpec<?>, List<String>> thisOptionsToArguments = new HashMap<>(this.optionsToArguments);
        return this.detectedOptions.hashCode() ^ thisOptionsToArguments.hashCode() ^ this.nonOptionArguments.hashCode();
    }

    private <V> List<V> defaultValuesFor(String option) {
        if (this.defaultValues.containsKey(option)) {
            List<V> defaults = (List) this.defaultValues.get(option);
            return defaults;
        } else {
            return Collections.emptyList();
        }
    }

    private <V> List<V> defaultValueFor(OptionSpec<V> option) {
        return this.defaultValuesFor(option.options().iterator().next());
    }

    static {
    }
}
