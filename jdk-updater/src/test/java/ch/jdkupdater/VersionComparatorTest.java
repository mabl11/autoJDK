package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class VersionComparatorTest {

    private final VersionComparator comparator = new VersionComparator();

    @Test
    void testComparesNumbersByValue() {
        assertTrue(comparator.compare("jdk-9", "jdk-25") < 0);
        assertTrue(comparator.compare("jdk-25.0.10", "jdk-25.0.9") > 0);
    }

    @Test
    void testComparesPlusAndDotNamesByNumbers() {
        assertTrue(comparator.compare("jdk-25.0.4+7", "jdk-25.0.5.11") < 0);
    }

    @Test
    void testLongerVersionIsNewer() {
        assertTrue(comparator.compare("jdk-25", "jdk-25.0.1") < 0);
    }

    @Test
    void testEqualNamesAreEqual() {
        assertEquals(0, comparator.compare("jdk-25.0.5.11", "jdk-25.0.5.11"));
    }

    @Test
    void testSortsFromOldestToNewest() {
        List<String> sorted = Stream.of("jdk-25.0.5.11", "jdk-9", "jdk-25.0.4+7", "jdk-21.0.2")
                .sorted(comparator)
                .toList();
        assertEquals(List.of("jdk-9", "jdk-21.0.2", "jdk-25.0.4+7", "jdk-25.0.5.11"), sorted);
    }
}
