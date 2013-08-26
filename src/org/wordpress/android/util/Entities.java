package org.wordpress.android.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * Provides HTML and XML entity utilities.
 * </p>
 * 
 * @see <a
 *      href="http://hotwired.lycos.com/webmonkey/reference/special_characters/">ISO
 *      Entities</a>
 * @see <a href="http://www.w3.org/TR/REC-html32#latin1">HTML 3.2 Character
 *      Entities for ISO Latin-1</a>
 * @see <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">HTML 4.0
 *      Character entity references</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#h-5.3">HTML 4.01
 *      Character References</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#code-position">HTML
 *      4.01 Code positions</a>
 * @author <a href="mailto:alex@purpletech.com">Alexander Day Chaffee</a>
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @since 2.0
 * @version $Id: Entities.java 636641 2008-03-13 06:11:30Z bayard $
 */
class Entities {

    private static class StrNumPair {
        private final String str;
        private final int num;

        StrNumPair(String str, int num) {
            this.str = str;
            this.num = num;
        }
    }

    private static final StrNumPair[] BASIC_ARRAY = {
            new StrNumPair("quot", 34), new StrNumPair("amp", 38),
            new StrNumPair("lt", 60), new StrNumPair("gt", 62) };

    private static final StrNumPair[] APOS_ARRAY = { new StrNumPair("apos", 39), };

    // package scoped for testing
    private static final StrNumPair[] ISO8859_1_ARRAY = {
            new StrNumPair("nbsp", 160), new StrNumPair("iexcl", 161),
            new StrNumPair("cent", 162), new StrNumPair("pound", 163),
            new StrNumPair("curren", 164), new StrNumPair("yen", 165),
            new StrNumPair("brvbar", 166), new StrNumPair("sect", 167),
            new StrNumPair("uml", 168), new StrNumPair("copy", 169),
            new StrNumPair("ordf", 170), new StrNumPair("laquo", 171),
            new StrNumPair("not", 172), new StrNumPair("shy", 173),
            new StrNumPair("reg", 174), new StrNumPair("macr", 175),
            new StrNumPair("deg", 176), new StrNumPair("plusmn", 177),
            new StrNumPair("sup2", 178), new StrNumPair("sup3", 179),
            new StrNumPair("acute", 180), new StrNumPair("micro", 181),
            new StrNumPair("para", 182), new StrNumPair("middot", 183),
            new StrNumPair("cedil", 184), new StrNumPair("sup1", 185),
            new StrNumPair("ordm", 186), new StrNumPair("raquo", 187),
            new StrNumPair("frac14", 188), new StrNumPair("frac12", 189),
            new StrNumPair("frac34", 190), new StrNumPair("iquest", 191),
            new StrNumPair("Agrave", 192), new StrNumPair("Aacute", 193),
            new StrNumPair("Acirc", 194), new StrNumPair("Atilde", 195),
            new StrNumPair("Auml", 196), new StrNumPair("Aring", 197),
            new StrNumPair("AElig", 198), new StrNumPair("Ccedil", 199),
            new StrNumPair("Egrave", 200), new StrNumPair("Eacute", 201),
            new StrNumPair("Ecirc", 202), new StrNumPair("Euml", 203),
            new StrNumPair("Igrave", 204), new StrNumPair("Iacute", 205),
            new StrNumPair("Icirc", 206), new StrNumPair("Iuml", 207),
            new StrNumPair("ETH", 208), new StrNumPair("Ntilde", 209),
            new StrNumPair("Ograve", 210), new StrNumPair("Oacute", 211),
            new StrNumPair("Ocirc", 212), new StrNumPair("Otilde", 213),
            new StrNumPair("Ouml", 214), new StrNumPair("times", 215),
            new StrNumPair("Oslash", 216), new StrNumPair("Ugrave", 217),
            new StrNumPair("Uacute", 218), new StrNumPair("Ucirc", 219),
            new StrNumPair("Uuml", 220), new StrNumPair("Yacute", 221),
            new StrNumPair("THORN", 222), new StrNumPair("szlig", 223),
            new StrNumPair("agrave", 224), new StrNumPair("aacute", 225),
            new StrNumPair("acirc", 226), new StrNumPair("atilde", 227),
            new StrNumPair("auml", 228), new StrNumPair("aring", 229),
            new StrNumPair("aelig", 230), new StrNumPair("ccedil", 231),
            new StrNumPair("egrave", 232), new StrNumPair("eacute", 233),
            new StrNumPair("ecirc", 234), new StrNumPair("euml", 235),
            new StrNumPair("igrave", 236), new StrNumPair("iacute", 237),
            new StrNumPair("icirc", 238), new StrNumPair("iuml", 239),
            new StrNumPair("eth", 240), new StrNumPair("ntilde", 241),
            new StrNumPair("ograve", 242), new StrNumPair("oacute", 243),
            new StrNumPair("ocirc", 244), new StrNumPair("otilde", 245),
            new StrNumPair("ouml", 246), new StrNumPair("divide", 247),
            new StrNumPair("oslash", 248), new StrNumPair("ugrave", 249),
            new StrNumPair("uacute", 250), new StrNumPair("ucirc", 251),
            new StrNumPair("uuml", 252), new StrNumPair("yacute", 253),
            new StrNumPair("thorn", 254), new StrNumPair("yuml", 255), };

    // http://www.w3.org/TR/REC-html40/sgml/entities.html
    // package scoped for testing
    private static final StrNumPair[] HTML40_ARRAY = {
            // <!-- Latin Extended-B -->
            new StrNumPair("fnof", 402),
            new StrNumPair("Alpha", 913),
            new StrNumPair("Beta", 914),
            new StrNumPair("Gamma", 915),
            new StrNumPair("Delta", 916),
            new StrNumPair("Epsilon", 917),
            new StrNumPair("Zeta", 918),
            new StrNumPair("Eta", 919),
            new StrNumPair("Theta", 920),
            new StrNumPair("Iota", 921),
            new StrNumPair("Kappa", 922),
            new StrNumPair("Lambda", 923),
            new StrNumPair("Mu", 924),
            new StrNumPair("Nu", 925),
            new StrNumPair("Xi", 926),
            new StrNumPair("Omicron", 927),
            new StrNumPair("Pi", 928),
            new StrNumPair("Rho", 929),
            // <!-- there is no Sigmaf, and no U+03A2 character either -->
            new StrNumPair("Sigma", 931),
            new StrNumPair("Tau", 932),
            new StrNumPair("Upsilon", 933),
            new StrNumPair("Phi", 934),
            new StrNumPair("Chi", 935),
            new StrNumPair("Psi", 936),
            new StrNumPair("Omega", 937),
            new StrNumPair("alpha", 945),
            new StrNumPair("beta", 946),
            new StrNumPair("gamma", 947),
            new StrNumPair("delta", 948),
            new StrNumPair("epsilon", 949),
            new StrNumPair("zeta", 950),
            new StrNumPair("eta", 951),
            new StrNumPair("theta", 952),
            new StrNumPair("iota", 953),
            new StrNumPair("kappa", 954),
            new StrNumPair("lambda", 955),
            new StrNumPair("mu", 956),
            new StrNumPair("nu", 957),
            new StrNumPair("xi", 958),
            new StrNumPair("omicron", 959),
            new StrNumPair("pi", 960),
            new StrNumPair("rho", 961),
            new StrNumPair("sigmaf", 962),
            new StrNumPair("sigma", 963),
            new StrNumPair("tau", 964),
            new StrNumPair("upsilon", 965),
            new StrNumPair("phi", 966),
            new StrNumPair("chi", 967),
            new StrNumPair("psi", 968),
            new StrNumPair("omega", 969),
            new StrNumPair("thetasym", 977),
            new StrNumPair("upsih", 978),
            new StrNumPair("piv", 982),
            // <!-- General Punctuation -->
            new StrNumPair("bull", 8226),
            // <!-- bullet is NOT the same as bullet operator, U+2219 -->
            new StrNumPair("hellip", 8230),
            new StrNumPair("prime", 8242),
            new StrNumPair("Prime", 8243),
            new StrNumPair("oline", 8254),
            new StrNumPair("frasl", 8260),
            // <!-- Letterlike Symbols -->
            new StrNumPair("weierp", 8472),
            new StrNumPair("image", 8465),
            new StrNumPair("real", 8476),
            new StrNumPair("trade", 8482),
            new StrNumPair("alefsym", 8501),
            // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
            // same glyph could be used to depict both characters -->
            // <!-- Arrows -->
            new StrNumPair("larr", 8592),
            new StrNumPair("uarr", 8593),
            new StrNumPair("rarr", 8594),
            new StrNumPair("darr", 8595),
            new StrNumPair("harr", 8596),
            new StrNumPair("crarr", 8629),
            new StrNumPair("lArr", 8656),
            // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
            // arrow but also does not have any other character for that function.
            // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
            new StrNumPair("uArr", 8657),
            new StrNumPair("rArr", 8658),
            // <!-- ISO 10646 does not say this is the 'implies' character but does not
            // have another character with this function so ?rArr can be used for
            // 'implies' as ISOtech suggests -->
            new StrNumPair("dArr", 8659),
            new StrNumPair("hArr", 8660),
            // <!-- Mathematical Operators -->
            new StrNumPair("forall", 8704),
            new StrNumPair("part", 8706),
            new StrNumPair("exist", 8707),
            new StrNumPair("empty", 8709),
            new StrNumPair("nabla", 8711),
            new StrNumPair("isin", 8712),
            new StrNumPair("notin", 8713),
            new StrNumPair("ni", 8715),
            // <!-- should there be a more memorable name than 'ni'? -->
            new StrNumPair("prod", 8719),
            // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
            // though the same glyph might be used for both -->
            new StrNumPair("sum", 8721),
            // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
            // though the same glyph might be used for both -->
            new StrNumPair("minus", 8722),
            new StrNumPair("lowast", 8727),
            new StrNumPair("radic", 8730),
            new StrNumPair("prop", 8733),
            new StrNumPair("infin", 8734),
            new StrNumPair("ang", 8736),
            new StrNumPair("and", 8743),
            new StrNumPair("or", 8744),
            new StrNumPair("cap", 8745),
            new StrNumPair("cup", 8746),
            new StrNumPair("int", 8747),
            new StrNumPair("there4", 8756),
            new StrNumPair("sim", 8764), // tilde operator = varies with = similar to,U+223C ISOtech -->
            // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
            // the same glyph might be used to represent both -->
            new StrNumPair("cong", 8773),
            new StrNumPair("asymp", 8776),
            new StrNumPair("ne", 8800),
            new StrNumPair("equiv", 8801),
            new StrNumPair("le", 8804),
            new StrNumPair("ge", 8805),
            new StrNumPair("sub", 8834),
            new StrNumPair("sup", 8835),
            // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
            // Symbol font encoding and is not included. Should it be, for symmetry?
            // It is in ISOamsn --> <!ENTITY nsub", "8836"},
            // not a subset of, U+2284 ISOamsn -->
            new StrNumPair("sube", 8838),
            new StrNumPair("supe", 8839),
            new StrNumPair("oplus", 8853),
            new StrNumPair("otimes", 8855),
            new StrNumPair("perp", 8869),
            new StrNumPair("sdot", 8901),
            // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
            // <!-- Miscellaneous Technical -->
            new StrNumPair("lceil", 8968),
            new StrNumPair("rceil", 8969),
            new StrNumPair("lfloor", 8970),
            new StrNumPair("rfloor", 8971),
            new StrNumPair("lang", 9001),
            // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
            // mark' -->
            new StrNumPair("rang", 9002),
            // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
            // 'single right-pointing angle quotation mark' -->
            // <!-- Geometric Shapes -->
            new StrNumPair("loz", 9674),
            // <!-- Miscellaneous Symbols -->
            new StrNumPair("spades", 9824),
            // <!-- black here seems to mean filled as opposed to hollow -->
            new StrNumPair("clubs", 9827),
            new StrNumPair("hearts", 9829),
            new StrNumPair("diams", 9830),

            // <!-- Latin Extended-A -->
            new StrNumPair("OElig", 338),
            new StrNumPair("oelig", 339),
            // <!-- ligature is a misnomer, this is a separate character in some languages -->
            new StrNumPair("Scaron", 352),
            new StrNumPair("scaron", 353),
            new StrNumPair("Yuml", 376),
            // <!-- Spacing Modifier Letters -->
            new StrNumPair("circ", 710),
            new StrNumPair("tilde", 732),
            // <!-- General Punctuation -->
            new StrNumPair("ensp", 8194), new StrNumPair("emsp", 8195),
            new StrNumPair("thinsp", 8201), new StrNumPair("zwnj", 8204),
            new StrNumPair("zwj", 8205), new StrNumPair("lrm", 8206),
            new StrNumPair("rlm", 8207), new StrNumPair("ndash", 8211),
            new StrNumPair("mdash", 8212), new StrNumPair("lsquo", 8216),
            new StrNumPair("rsquo", 8217), new StrNumPair("sbquo", 8218),
            new StrNumPair("ldquo", 8220), new StrNumPair("rdquo", 8221),
            new StrNumPair("bdquo", 8222), new StrNumPair("dagger", 8224),
            new StrNumPair("Dagger", 8225), new StrNumPair("permil", 8240),
            new StrNumPair("lsaquo", 8249),
            // <!-- lsaquo is proposed but not yet ISO standardized -->
            new StrNumPair("rsaquo", 8250),
            // <!-- rsaquo is proposed but not yet ISO standardized -->
            new StrNumPair("euro", 8364), };

    /**
     * <p>
     * The set of entities supported by standard XML.
     * </p>
     */
    public static final Entities XML;

    /**
     * <p>
     * The set of entities supported by HTML 3.2.
     * </p>
     */
    public static final Entities HTML32;

    /**
     * <p>
     * The set of entities supported by HTML 4.0.
     * </p>
     */
    public static final Entities HTML40;
    public static final Entities HTML40_escape;
    static {
        XML = new Entities();
        XML.addEntities(BASIC_ARRAY);
        XML.addEntities(APOS_ARRAY);
    }

    static {
        HTML32 = new Entities();
        HTML32.addEntities(BASIC_ARRAY);
        HTML32.addEntities(ISO8859_1_ARRAY);
    }

    static {
        HTML40 = new Entities();
        fillWithHtml40Entities(HTML40);
        HTML40_escape = new Entities();
        fillWithHtml40EntitiesEscape(HTML40);
    }

    /**
     * <p>
     * Fills the specified entities instance with HTML 40 entities.
     * </p>
     * 
     * @param entities
     *            the instance to be filled.
     */
    private static void fillWithHtml40Entities(Entities entities) {
        entities.addEntities(BASIC_ARRAY);
        entities.addEntities(ISO8859_1_ARRAY);
        entities.addEntities(HTML40_ARRAY);
    }

    private static void fillWithHtml40EntitiesEscape(Entities entities) {
        //entities.addEntities(BASIC_ARRAY);
        entities.addEntities(ISO8859_1_ARRAY);
        entities.addEntities(HTML40_ARRAY);
    }

    static interface EntityMap {
        /**
         * <p>
         * Add an entry to this entity map.
         * </p>
         * 
         * @param name
         *            the entity name
         * @param value
         *            the entity value
         */
        void add(String name, int value);

        /**
         * <p>
         * Returns the name of the entity identified by the specified value.
         * </p>
         * 
         * @param value
         *            the value to locate
         * @return entity name associated with the specified value
         */
        String name(int value);

        /**
         * <p>
         * Returns the value of the entity identified by the specified name.
         * </p>
         * 
         * @param name
         *            the name to locate
         * @return entity value associated with the specified name
         */
        int value(String name);
    }

    private static class PrimitiveEntityMap implements EntityMap {
        private Map<String, Integer> mapNameToValue = new HashMap<String, Integer>();

        private IntHashMap mapValueToName = new IntHashMap();

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, value);
            mapValueToName.put(value, name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return (String) mapValueToName.get(value);
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    private static abstract class MapIntMap implements Entities.EntityMap {
        protected Map<String, Integer> mapNameToValue;

        protected Map<Integer, String> mapValueToName;

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, value);
            mapValueToName.put(value, name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return mapValueToName.get(value);
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    static class HashEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>HashEntityMap</code>.
         */
        public HashEntityMap() {
            mapNameToValue = new HashMap<String, Integer>();
            mapValueToName = new HashMap<Integer, String>();
        }
    }

    static class TreeEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>TreeEntityMap</code>.
         */
        public TreeEntityMap() {
            mapNameToValue = new TreeMap<String, Integer>();
            mapValueToName = new TreeMap<Integer, String>();
        }
    }

    private static class LookupEntityMap extends PrimitiveEntityMap {
        private String[] lookupTable;

        private int LOOKUP_TABLE_SIZE = 256;

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            if (value < LOOKUP_TABLE_SIZE) {
                return lookupTable()[value];
            }
            return super.name(value);
        }

        /**
         * <p>
         * Returns the lookup table for this entity map. The lookup table is
         * created if it has not been previously.
         * </p>
         * 
         * @return the lookup table
         */
        private String[] lookupTable() {
            if (lookupTable == null) {
                createLookupTable();
            }
            return lookupTable;
        }

        /**
         * <p>
         * Creates an entity lookup table of LOOKUP_TABLE_SIZE elements,
         * initialized with entity names.
         * </p>
         */
        private void createLookupTable() {
            lookupTable = new String[LOOKUP_TABLE_SIZE];
            for (int i = 0; i < LOOKUP_TABLE_SIZE; ++i) {
                lookupTable[i] = super.name(i);
            }
        }
    }

    private static class ArrayEntityMap implements EntityMap {
        protected int growBy = 100;

        protected int size = 0;

        protected String[] names;

        protected int[] values;

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code>.
         */
        public ArrayEntityMap() {
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying
         * the size by which the array should grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public ArrayEntityMap(int growBy) {
            this.growBy = growBy;
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            names[size] = name;
            values[size] = value;
            size++;
        }

        /**
         * Verifies the capacity of the entity array, adjusting the size if
         * necessary.
         * 
         * @param capacity
         *            size the array should be
         */
        protected void ensureCapacity(int capacity) {
            if (capacity > names.length) {
                int newSize = Math.max(capacity, size + growBy);
                String[] newNames = new String[newSize];
                System.arraycopy(names, 0, newNames, 0, size);
                names = newNames;
                int[] newValues = new int[newSize];
                System.arraycopy(values, 0, newValues, 0, size);
                values = newValues;
            }
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            for (int i = 0; i < size; ++i) {
                if (values[i] == value) {
                    return names[i];
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            for (int i = 0; i < size; ++i) {
                if (names[i].equals(name)) {
                    return values[i];
                }
            }
            return -1;
        }
    }

    private static class BinaryEntityMap extends ArrayEntityMap {

        /**
         * Constructs a new instance of <code>BinaryEntityMap</code>.
         */
        public BinaryEntityMap() {
            super();
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying
         * the size by which the underlying array should grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public BinaryEntityMap(int growBy) {
            super(growBy);
        }

        /**
         * Performs a binary search of the entity array for the specified key.
         * This method is based on code in {@link java.util.Arrays}.
         * 
         * @param key
         *            the key to be found
         * @return the index of the entity array matching the specified key
         */
        private int binarySearch(int key) {
            int low = 0;
            int high = size - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = values[mid];

                if (midVal < key) {
                    low = mid + 1;
                } else if (midVal > key) {
                    high = mid - 1;
                } else {
                    return mid; // key found
                }
            }
            return -(low + 1); // key not found.
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            int insertAt = binarySearch(value);
            if (insertAt > 0) {
                return; // note: this means you can't insert the same value twice
            }
            insertAt = -(insertAt + 1); // binarySearch returns it negative and off-by-one
            System.arraycopy(values, insertAt, values, insertAt + 1, size
                    - insertAt);
            values[insertAt] = value;
            System.arraycopy(names, insertAt, names, insertAt + 1, size
                    - insertAt);
            names[insertAt] = name;
            size++;
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            int index = binarySearch(value);
            if (index < 0) {
                return null;
            }
            return names[index];
        }
    }

    // package scoped for testing
    private EntityMap map = new Entities.LookupEntityMap();

    /**
     * <p>
     * Adds entities to this entity.
     * </p>
     * 
     * @param entityArray
     *            array of entities to be added
     */
    public void addEntities(StrNumPair[] entityArray) {
        for (int i = 0; i < entityArray.length; ++i) {
            addEntity(entityArray[i].str, entityArray[i].num);
        }
    }

    /**
     * <p>
     * Add an entity to this entity.
     * </p>
     * 
     * @param name
     *            name of the entity
     * @param value
     *            vale of the entity
     */
    public void addEntity(String name, int value) {
        map.add(name, value);
    }

    /**
     * <p>
     * Returns the name of the entity identified by the specified value.
     * </p>
     * 
     * @param value
     *            the value to locate
     * @return entity name associated with the specified value
     */
    public String entityName(int value) {
        return map.name(value);
    }

    /**
     * <p>
     * Returns the value of the entity identified by the specified name.
     * </p>
     * 
     * @param name
     *            the name to locate
     * @return entity value associated with the specified name
     */
    public int entityValue(String name) {
        return map.value(name);
    }

    /**
     * <p>
     * Escapes the characters in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1),
     * escape(&quot;\u00A1&quot;) will return &quot;&amp;foo;&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String escape(String str) {
        StringWriter stringWriter = createStringWriter(str);
        try {
            this.escape(stringWriter, str);
        } catch (IOException e) {
            // This should never happen because ALL the StringWriter methods called by #escape(Writer, String) do not
            // throw IOExceptions.
            //throw new UnhandledException(e);
        }
        return stringWriter.toString();
    }

    /**
     * <p>
     * Escapes the characters in the <code>String</code> passed and writes the
     * result to the <code>Writer</code> passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results of the escaping
     *            to. Assumed to be a non-null value.
     * @param str
     *            The <code>String</code> to escape. Assumed to be a non-null
     *            value.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from
     *             calls to the {@link Writer#write(int)} methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void escape(Writer writer, String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            String entityName = this.entityName(c);
            if (entityName == null) {
                if (c > 0x7F) {
                    writer.write("&#");
                    writer.write(Integer.toString(c, 10));
                    writer.write(';');
                } else {
                    writer.write(c);
                }
            } else {
                writer.write('&');
                writer.write(entityName);
                writer.write(';');
            }
        }
    }

    /**
     * <p>
     * Unescapes the entities in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1),
     * unescape(&quot;&amp;foo;&quot;) will return &quot;\u00A1&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String unescape(String str) {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            return str;
        } else {
            StringWriter stringWriter = createStringWriter(str);
            try {
                this.doUnescape(stringWriter, str, firstAmp);
            } catch (IOException e) {
                // This should never happen because ALL the StringWriter methods called by #escape(Writer, String)
                // do not throw IOExceptions.
                // throw new UnhandledException(e);
            }
            return stringWriter.toString();
        }
    }

    /**
     * Make the StringWriter 10% larger than the source String to avoid growing
     * the writer
     * 
     * @param str
     *            The source string
     * @return A newly created StringWriter
     */
    private StringWriter createStringWriter(String str) {
        return new StringWriter((int) (str.length() + (str.length() * 0.1)));
    }

    /**
     * <p>
     * Unescapes the escaped entities in the <code>String</code> passed and
     * writes the result to the <code>Writer</code> passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be
     *            non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be
     *            non-null.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from
     *             calls to the {@link Writer#write(int)} methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void unescape(Writer writer, String str) throws IOException {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            writer.write(str);
            return;
        } else {
            doUnescape(writer, str, firstAmp);
        }
    }

    /**
     * Underlying unescape method that allows the optimisation of not starting
     * from the 0 index again.
     * 
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be
     *            non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be
     *            non-null.
     * @param firstAmp
     *            The <code>int</code> index of the first ampersand in the
     *            source String.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from
     *             calls to the {@link Writer#write(int)} methods.
     */
    private void doUnescape(Writer writer, String str, int firstAmp)
            throws IOException {
        writer.write(str, 0, firstAmp);
        int len = str.length();
        for (int i = firstAmp; i < len; i++) {
            char c = str.charAt(i);
            if (c == '&') {
                int nextIdx = i + 1;
                int semiColonIdx = str.indexOf(';', nextIdx);
                if (semiColonIdx == -1) {
                    writer.write(c);
                    continue;
                }
                int amphersandIdx = str.indexOf('&', i + 1);
                if (amphersandIdx != -1 && amphersandIdx < semiColonIdx) {
                    // Then the text looks like &...&...;
                    writer.write(c);
                    continue;
                }
                String entityContent = str.substring(nextIdx, semiColonIdx);
                int entityValue = -1;
                int entityContentLen = entityContent.length();
                if (entityContentLen > 0) {
                    if (entityContent.charAt(0) == '#') { // escaped value content is an integer (decimal or
                        // hexidecimal)
                        if (entityContentLen > 1) {
                            char isHexChar = entityContent.charAt(1);
                            try {
                                switch (isHexChar) {
                                case 'X':
                                case 'x': {
                                    entityValue = Integer.parseInt(
                                            entityContent.substring(2), 16);
                                    break;
                                }
                                default: {
                                    entityValue = Integer.parseInt(
                                            entityContent.substring(1), 10);
                                }
                                }
                                if (entityValue > 0xFFFF) {
                                    entityValue = -1;
                                }
                            } catch (NumberFormatException e) {
                                entityValue = -1;
                            }
                        }
                    } else { // escaped value content is an entity name
                        entityValue = this.entityValue(entityContent);
                    }
                }

                if (entityValue == -1) {
                    writer.write('&');
                    writer.write(entityContent);
                    writer.write(';');
                } else {
                    writer.write(entityValue);
                }
                i = semiColonIdx; // move index up to the semi-colon
            } else {
                writer.write(c);
            }
        }
    }
}
