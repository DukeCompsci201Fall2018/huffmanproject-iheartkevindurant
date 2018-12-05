import java.util.PriorityQueue;

//Will Schmidt
//Michael Williams
/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
		System.out.println("ligma");
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = freqs(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makecodings(root);
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while(true) {
			int val = in.readBits(BITS_PER_WORD);
			if(val == -1) break;
			String code = codings[val];
			out.writeBits(code.length(),Integer.parseInt(code,2));
		}
		String code = codings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code,2));
		
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		// TODO Auto-generated method stub
		if(root.myLeft != null && root.myRight != null) {
		out.writeBits(1, 0);
		writeHeader(root.myLeft,out);
		writeHeader(root.myRight,out);
		}
		else if(root.myValue != -1) {
		out.writeBits(1, 1);
		out.writeBits(9, root.myValue);
		if(myDebugLevel == DEBUG_HIGH) {
			System.out.printf("Value %d written to tree header\n", root.myValue);
		}
		}
		}

	private String[] makecodings(HuffNode root) {
		String[] encodings = new String[ALPH_SIZE + 1];
	    codehelper(encodings, root, "");
	    return encodings;
	}
	private void codehelper(String[] codings, HuffNode sub, String currPath) {
		if(sub == null) {
			return;
		}
		if(sub.myLeft == null && sub.myRight == null) {
			codings[sub.myValue] = currPath;
			if(myDebugLevel == DEBUG_HIGH) {
				System.out.printf("Coding %s created for tree\n", currPath);
			}
			return;
		}
		codehelper(codings, sub.myLeft, currPath + "0");
		codehelper(codings, sub.myRight, currPath + "1");
	}

	//if this does not work change the 0 to a -1 in the line where u make t

	private HuffNode makeTreeFromCounts(int[] freq) {
	// TODO Auto-generated method stub
	PriorityQueue<HuffNode> pq = new PriorityQueue<>();


	for(int i = 0; i < ALPH_SIZE + 1; i++) {
	    pq.add(new HuffNode(i,freq[i],null,null));
	}
	 
	while (pq.size() > 1) {
	    HuffNode left = pq.remove();
	    HuffNode right = pq.remove();
	    // create new HuffNode t with weight from
	    // left.weight+right.weight and left, right subtrees
	    HuffNode t = new HuffNode(-1,left.myWeight + right.myWeight,left,right);
	    pq.add(t);
	}
	HuffNode root = pq.remove();

	return root;
	}

	private int[] freqs(BitInputStream in) {
		int[] ret = new int[ALPH_SIZE + 1];
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			ret[val] = ret[val] + 1;
		}
		ret[PSEUDO_EOF] = 1;
		return ret;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int bits = in.readBits(BITS_PER_INT);
		if(bits != HUFF_TREE){
			throw new HuffException("illegal header starts with " + bits);
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}

	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		HuffNode current = root;
		while (current != null) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else { 
		           if (bits == 0) current = current.myLeft;
		      else current = current.myRight;

		           if (current.myLeft == null && current.myRight == null) {
		               if (current.myValue == PSEUDO_EOF) 
		                   break;   // out of loop
		               else {
		                   out.writeBits(BITS_PER_WORD,current.myValue);
		                   current = root; // start back after leaf
		               }
		           }
		       }
		   }

		}



	private HuffNode readTreeHeader(BitInputStream in) {
		int leafroot = in.readBits(1);
		if(leafroot == -1) {
			throw new HuffException("Error in reading tree header");
		}
		if(leafroot == 0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			return new HuffNode(0,0, left, right);
		}
		else {
			int val = in.readBits(BITS_PER_WORD + 1);
			if(myDebugLevel == DEBUG_HIGH) {
				System.out.printf("Tree value %d read from header\n", val);
			}
			return new HuffNode(val, 0, null, null);

		}
	}
}