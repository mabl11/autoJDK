package ch.jdkupdater;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Orders JDK folder names by version, for example {@code jdk-9 < jdk-25.0.4+7 < jdk-25.0.5.11}.
 *
 * <p>A name is split into runs of digits and runs of other characters. Runs of digits are compared as numbers,
 * all other runs as text. If all runs are equal, the name with more runs is the newer one.</p>
 */
public final class VersionComparator implements Comparator<String> {

    private static final Pattern RUN = Pattern.compile("\\d+|\\D+");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * Creates a comparator.
     */
    public VersionComparator() {
    }

    @Override
    public int compare(final String first, final String second) {
        List<String> firstRuns = runs(first);
        List<String> secondRuns = runs(second);
        int commonRuns = Math.min(firstRuns.size(), secondRuns.size());
        for (int i = 0; i < commonRuns; i++) {
            int result = compareRuns(firstRuns.get(i), secondRuns.get(i));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(firstRuns.size(), secondRuns.size());
    }

    private static List<String> runs(final String name) {
        return RUN.matcher(name).results().map(MatchResult::group).toList();
    }

    private static int compareRuns(final String first, final String second) {
        if (DIGITS.matcher(first).matches() && DIGITS.matcher(second).matches()) {
            return new BigInteger(first).compareTo(new BigInteger(second));
        }
        return first.compareTo(second);
    }
}
