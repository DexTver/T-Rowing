package ru.rowing.seeding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.rowing.seeding.model.Plan;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrelimSizingTest {

    private final Plan planA = PlanRepository.fromClasspath("/seeding/plan_A.json");

    @ParameterizedTest(name = "N={0} -> {1}+{2}")
    @CsvSource({
            "10, 5, 5",
            "11, 6, 5",
            "12, 6, 6",
            "13, 7, 6",
            "14, 7, 7",
            "15, 8, 7",
            "16, 8, 8",
            "17, 9, 8",
            "18, 9, 9"
    })
    void evenDescDistributesParticipantsBiggerHeatsFirst(int n, int first, int second) {
        List<Integer> sizes = PrelimSizing.sizesFor(planA, n);
        assertEquals(List.of(first, second), sizes);
    }

    @Test
    void everySizeSumsToN() {
        for (int n = 10; n <= 18; n++) {
            int sum = PrelimSizing.sizesFor(planA, n).stream().mapToInt(Integer::intValue).sum();
            assertEquals(n, sum, "сумма размеров должна равняться N=" + n);
        }
    }
}
