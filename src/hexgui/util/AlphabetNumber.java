// AlphabetNumber.java

package hexgui.util;

/** Alphabet numbers.

    We start from 1, i.e., 1 = a, 2 = b, ..., 26 = z, 27 = aa, etc.

    0 and negative numbers, we start with "!", e.g.: 0 = !a, -1 = !b.
*/
public final class AlphabetNumber
{
    /** Convert an integer to a lower-case alphabet number. */
    public static String toString(int n)
    {
        if (n <= 0) {
            return "!" + toString(1-n);
        }
        if (n <= 26) {
            return Character.toString((char)((int)'a' + n - 1));
        }
        int hi = (n-1) / 26;
        int lo = (n-1) % 26;
            
        return toString((n-1) / 26) + Character.toString((char)(int)'a' + lo);
    }
}
