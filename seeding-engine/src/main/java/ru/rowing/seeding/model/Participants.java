package ru.rowing.seeding.model;

/** Диапазон числа участников, для которого применим план (включительно). */
public record Participants(int min, int max) {
    public boolean contains(int n) {
        return n >= min && n <= max;
    }
}
