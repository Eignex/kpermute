import com.eignex.kpermute.Permutations;

public final class Consumer {
    public static void main(String[] args) {
        var ints = Permutations.intPermutation(97);
        var longs = Permutations.longPermutation(97L);
        for (int value = 0; value < 97; value++) {
            if (ints.decode(ints.encode(value)) != value || longs.decode(longs.encode(value)) != value) {
                throw new AssertionError("Published permutation failed to round trip " + value);
            }
        }
        System.out.println("Published Java consumer passed");
    }
}
