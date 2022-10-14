//----------------------------------------------------------------------------
// $Id$ 
//----------------------------------------------------------------------------

package hexgui.sgf;

import hexgui.hex.HexColor;
import hexgui.hex.HexPoint;
import hexgui.hex.Move;
import hexgui.game.Node;
import hexgui.game.GameInfo;

import java.io.*;
import java.awt.Dimension;
import java.lang.StringBuilder;
import java.lang.NumberFormatException;
import static java.text.MessageFormat.format;
import java.util.*;

//----------------------------------------------------------------------------

/** SGF reader. 
    See https://www.red-bean.com/sgf/ for the SGF definition.
    NOTE: Uses StringBuilder which requires Java 1.5.
*/
public final class SgfReader
{
    /** Sgf exception. */
    public static class SgfError
	extends Exception
    {
	public SgfError(String message)
	{
	    super(message);
	}
    }

    private static final int GM_HEXGAME = 11;


    /** Constructor. 
	Parse the input stream in sgf format. 
    */
    public SgfReader(InputStream in) throws SgfError
    {
	InputStreamReader reader = new InputStreamReader(in);
	m_reader = new LineNumberReader(reader);
	m_tokenizer = new StreamTokenizer(m_reader);
	m_gameinfo = new GameInfo();
	m_warnings = new Vector<String>();
        m_swap_bug = false;
	try {
	    findGameTree();
	    m_gametree = parseGameTree(null, true);
	    m_reader.close();
	}
	catch (IOException e) {
	    throw sgfError("IO error occurred while parsing file.");
	}
    }

    public Node getGameTree()
    {
	return m_gametree;
    }

    public GameInfo getGameInfo()
    {
	return m_gameinfo;
    }

    public Vector<String> getWarnings()
    {
	if (m_warnings.size() == 0) 
	    return null;
	return m_warnings;
    }

    //------------------------------------------------------------

    /** Fast forward to the first "(". */
    private void findGameTree() throws SgfError, IOException
    {
	while (true) {
	    int ttype = m_tokenizer.nextToken();
	    if (ttype == StreamTokenizer.TT_EOF)
		throw sgfError("No game tree found!");
	    
	    if (ttype == '(') {
		m_tokenizer.pushBack();
		break;
	    }		
	}
    }

    private Node parseGameTree(Node parent, boolean isroot) 
	throws SgfError, IOException
    {
	int ttype = m_tokenizer.nextToken();
	if (ttype != '(') 
	    throw sgfError("Missing '(' at head of game tree.");

	Node node = parseNode(parent, isroot);
	
	ttype = m_tokenizer.nextToken();
	if (ttype != ')') 
	    throw sgfError("Game tree not closed!");

	return node;
    }

    private Node parseNode(Node parent, boolean isroot) 
	throws SgfError, IOException
    {
	int ttype = m_tokenizer.nextToken();
	if (ttype != ';') 
	    throw sgfError("Error at head of node!");

	Node node = new Node();
	node.setParent(parent);
	if (parent != null) 
	    parent.addChild(node);

	boolean done = false;
	while (!done) {
	    ttype = m_tokenizer.nextToken();
	    switch(ttype) {
	    case '(':
		m_tokenizer.pushBack();
		parseGameTree(node, false);
		break;

	    case ';':
		m_tokenizer.pushBack();
		parseNode(node, false);
		done = true;
		break;

	    case ')':
		m_tokenizer.pushBack();
		done = true;
		break;

	    case StreamTokenizer.TT_WORD:
		parseProperty(node, isroot);
		break;

	    case StreamTokenizer.TT_EOF:
		throw sgfError("Unexpected EOF in node!");

	    default:
		throw sgfError("Error in SGF file.");
	    }
	}

	return node;
    }

    /** Parse a point or move value.
        Supports both standard SGF notation for Hex (a1, ...) and Go-like
        notation used by Little Golem (aa, ...) */
    private HexPoint parsePoint(String s) throws SgfError
    {
        s = s.trim().toLowerCase(Locale.ENGLISH);
        HexPoint result = null;
        if (s.length() >= 2)
        {
            if (s.length() == 2 && s.charAt(1) >= 'a' && s.charAt(1) <= 'z')
            {
                // Go-like SGF notation
                int x = s.charAt(0) - 'a';
                int y = s.charAt(1) - 'a';
                if (x < HexPoint.MAX_WIDTH && y < HexPoint.MAX_HEIGHT)
                    result = HexPoint.get(x, y);
            }
            else
            {
                // Standard SGF notation for Hex
                try
                {
                    int x = s.charAt(0) - 'a';
                    int y = Integer.parseInt(s.substring(1)) - 1;
                    if (y >= 0 && y < HexPoint.MAX_HEIGHT)
                        result = HexPoint.get(x, y);
                }
                catch (NumberFormatException e)
                {
                }
            }
        }
        if (result == null)
            throw sgfError(format("Invalid point {0}", s));
        return result;
    }

    private HexPoint parseMove(String s) throws SgfError
    {
        s = s.trim().toLowerCase(Locale.ENGLISH);

        // Special case: some or all versions of HexGui up to 0.9.GIT
        // incorrectly used "swap-pieces" instead of "swap-sides".
        // The SGF specification states:
        //
        // * swap-sides - the player elects to swap sides with his
        //   opponent; if he was playing Black he now plays White, and
        //   vice versa.
        //
        // * swap-pieces - the player elects to swap pieces with his
        //   opponent - all of Black's pieces are colored White, and
        //   White's pieces are colored Black. Then the entire board
        //   is reflected in the long diagonal axis.
        //
        // For backward compatibility, we must compensate for the
        // incorrect use of "swap-pieces" when reading SGF files
        // written by HexGui 0.9.GIT or earlier.

        if (m_swap_bug && s.equals("swap-pieces")) {
            s = "swap-sides";
        }
        
        // HexPoint.get() handles special move values like "swap"
        HexPoint result = HexPoint.get(s);
        if (result == null)
            // Handles Go-style point notation (aa, ...)
            result = parsePoint(s);
        return result;
    }

    private void parseProperty(Node node, boolean isroot) 
	throws SgfError, IOException
    {
	int x,y;
	String name = m_tokenizer.sval;

        boolean done = false;
        while(!done) {

	    int ttype = m_tokenizer.nextToken();
            if (ttype != '[') {
                done = true;
            }
            m_tokenizer.pushBack();
            if (done) {
                break;
            }

            String val;
            if (name.equals("C")) {
                val = parseComment();
            } else {
                val = parseValue();
            }
            //System.out.println(name + "[" + val + "]");
	
            if (name.equals("W")) {
                HexPoint point = parseMove(val);
                node.setMove(new Move(point, HexColor.WHITE));
            } 
            else if (name.equals("B")) {
                HexPoint point = parseMove(val);
                node.setMove(new Move(point, HexColor.BLACK));
            } 
            else if (name.equals("AB")) {
                node.addSetup(HexColor.BLACK, parsePoint(val));
            }
            else if (name.equals("AW")) {
                node.addSetup(HexColor.WHITE, parsePoint(val));
            }
            else if (name.equals("AE")) {
                node.addSetup(HexColor.EMPTY, parsePoint(val));
            }
            else if (name.equals("LB")) {
                node.addLabel(val);
            }
            else if (name.equals("FF")) {
                node.setSgfProperty(name, val);
                x = parseInt(val);
                if (x < 1 || x > 4)
                    throw sgfError("Invalid SGF Version! (" + x + ")");
            }
            else if (name.equals("GM")) {
                node.setSgfProperty(name, val);
                if (!isroot) sgfWarning("GM property in non-root node!");
                if (parseInt(val) != GM_HEXGAME) throw sgfError("Not a Hex game!");
            }
            else if (name.equals("SZ")) {
                node.setSgfProperty(name, val);
                if (!isroot) sgfWarning("GM property in non-root node!");
                Dimension dim = new Dimension();
                String sp[] = val.split(":");
                if (sp.length == 1) {
                    x = parseInt(sp[0]);
                    dim.setSize(x,x);
                } else if (sp.length == 2) {
                    x = parseInt(sp[0]);
                    y = parseInt(sp[1]);
                    dim.setSize(x,y);
                } else {
                    throw sgfError("Malformed boardsize!");
                }
                m_gameinfo.setBoardSize(dim);
            } 
            else if (name.equals("AP")) {
                node.setSgfProperty(name, val);
                String regex = "HexGui:0\\.[0-9](\\z|[^0-9].*)";
                if (val.matches(regex)) {
                    // version HexGui:0.9 or earlier
                    m_swap_bug = true;
                }
            }
            else {
                node.setSgfProperty(name, val);
            }
        }
    }

    // Parse an SGF "SimpleText" property value.
    private String parseValue() throws SgfError, IOException
    {
	int ttype = m_tokenizer.nextToken();
	if (ttype != '[') {
	    throw sgfError("Property missing opening '['.");
        }

	StringBuilder sb = new StringBuilder(256);
	boolean quoted = false;
	while (true) {
	    int ch = m_reader.read();
	    if (ch < 0) {
		throw sgfError("Property runs to EOF.");
            }
            // Don't rely on the default conversion, because
            // sb.append(ch) would convert the integer to a string
            // rather than a character.
            char c = (char)ch;
            
	    if (!quoted) {
		if (c == ']') {
                    break;
                }
		if (c == '\\') {
		    quoted = true;
                } else {
		    if (Character.isWhitespace(c)) {
                        // The spec says "Whitespaces other than space
                        // must be converted to space".
                        sb.append(' ');
                    } else {
			sb.append(c);
                    }
		}
	    } else {
		quoted = false;
                if (Character.isWhitespace(c)) {
                    // The spec says "Any char following "\" is
                    // inserted verbatim (exception: whitespaces still
                    // have to be converted to space!)."
                    sb.append(' ');
                } else {
                    sb.append(c);
                }
	    }
	}

	return sb.toString();
    }

    // Parse an SGF "Text" property value.
    private String parseComment() throws SgfError, IOException
    {
	int ttype = m_tokenizer.nextToken();
	if (ttype != '[') {
	    throw sgfError("Comment missing opening '['.");
        }
        
	StringBuilder sb = new StringBuilder(4096);
	boolean quoted = false;
	while (true) {
	    int ch = m_reader.read();
	    if (ch < 0) {
		throw sgfError("Comment runs to EOF.");
            }
            char c = (char)ch;
            
	    if (!quoted) {
		if (c == ']') {
                    break;
                }

		if (c == '\\') {
		    quoted = true;
		} else {
                    if (Character.isWhitespace(c) && c != '\n') {
                        // The spec says "White spaces other than
                        // linebreaks are converted to space (e.g. no
                        // tab, vertical tab, ..)."

                        // Also note that the LineNumberReader class
                        // already takes care of newline conversion,
                        // i.e., "\n", "\r", and "\r\n" are all
                        // converted to '\n'.
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
		}
	    } else {
		quoted = false;
                if (c == '\n') {
                    // Append nothing. The spec says: "Soft line
                    // break: linebreaks preceded by a "\" (soft
                    // linebreaks are converted to "", i.e. they are
                    // removed)".
                } else if (Character.isWhitespace(c)) {
                    // The spec says: "Any char following "\" is
                    // inserted verbatim (exception: whitespaces still
                    // have to be converted to space!).
                    sb.append(' ');
                } else {
                    sb.append(c);
                }
	    }
	}

	return sb.toString();
    }

    private int parseInt(String str) throws SgfError
    {
	int ret;
	try {
	    ret = Integer.parseInt(str);
	}
	catch (NumberFormatException e) {
	    throw sgfError("Error parsing integer.");
	}
	return ret;
    }

    //----------------------------------------------------------------------

    private void verifyGame(Node root) throws SgfError
    {
	if (m_gameinfo.getBoardSize()==null) {
	    throw sgfError("Missing SZ property.");
        }
    }

    private SgfError sgfError(String msg)
    {
	return new SgfError("Line " + m_reader.getLineNumber() + ": " + msg);
    }

    private void sgfWarning(String msg)
    {
	m_warnings.add("Line " + m_reader.getLineNumber() + ": " + msg); 
    }
    
    private StreamTokenizer m_tokenizer;
    private LineNumberReader m_reader;
    private Node m_gametree;
    private GameInfo m_gameinfo;
    private Vector<String> m_warnings;
    private boolean m_swap_bug;
}

//----------------------------------------------------------------------------
