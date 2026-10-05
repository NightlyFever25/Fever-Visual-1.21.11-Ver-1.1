package fever.visual.systems.commands;

public record Parameter<T>(String name, boolean required, boolean vararg, ParameterValidator<T> validator) {
}
