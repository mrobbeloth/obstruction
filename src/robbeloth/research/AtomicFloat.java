package robbeloth.research;

import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.Float.*;

import java.io.Serializable;


/*
 * credit: https://stackoverflow.com/users/276052/aioobe
 */
class AtomicFloat extends Number implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 4893351481301476398L;
	private AtomicInteger bits;

    public AtomicFloat() {
        this(0f);
    }

    public AtomicFloat(float initialValue) {
        bits = new AtomicInteger(floatToIntBits(initialValue));
    }

    public final boolean compareAndSet(float expect, float update) {
        return bits.compareAndSet(floatToIntBits(expect),
                                  floatToIntBits(update));
    }

    public final void set(float newValue) {
        bits.set(floatToIntBits(newValue));
    }

    public final float get() {
        return intBitsToFloat(bits.get());
    }

    public float floatValue() {
        return get();
    }

    public final float getAndSet(float newValue) {
        return intBitsToFloat(bits.getAndSet(floatToIntBits(newValue)));
    }

    public final boolean weakCompareAndSet(float expect, float update) {
        return bits.weakCompareAndSetPlain(floatToIntBits(expect),
                                           floatToIntBits(update));
    }

    public double doubleValue() { return (double) floatValue(); }
    public int intValue()       { return (int) get();           }
    public long longValue()     { return (long) get();          }

}